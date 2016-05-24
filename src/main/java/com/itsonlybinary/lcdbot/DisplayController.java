package com.itsonlybinary.lcdbot;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.Lcd;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ItsOnlyBinary
 */
public class DisplayController implements Runnable {

    private final ConcurrentLinkedQueue<DisplayData> displayQueue;
    private final LCDbot lcdBot;
    private final TemperatureController temperatureController;
    private int lcdHandle;
    private volatile boolean running = false;
    private volatile boolean flashing = false;
    private volatile boolean backlight = false;
    private Thread thread;

    private volatile int delayFlashDefault;
    private volatile int delayFlash;
    private volatile int repeatFlash;

    DisplayController(LCDbot lcdBot) {
        displayQueue = new ConcurrentLinkedQueue<>();
        this.lcdBot = lcdBot;
        temperatureController = lcdBot.getTemperatureController();
    }

    public void enable() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void disable() {
        if (lcdHandle == -1) {
            return;
        }
        running = false;
        displayQueue.clear();
    }

    public void add(DisplayData data) {
        if (lcdHandle == -1) {
            return;
        }
        displayQueue.add(data);
    }

    @Override
    public void run() {
        try {
            //power on lcd
            Gpio.digitalWrite(LCDbot.PIN_LCD, true);
            Thread.sleep(10);

            lcdHandle = Lcd.lcdInit(
                    LCDbot.LCD_ROWS, // number of row supported by LCD
                    LCDbot.LCD_COLUMNS, // number of columns supported by LCD
                    LCDbot.LCD_BITS, // number of bits used to communicate to LCD
                    LCDbot.PIN_RS, // LCD RS pin
                    LCDbot.PIN_E, // LCD clock pin
                    LCDbot.PIN_DATA1, // LCD data bit 1
                    LCDbot.PIN_DATA2, // LCD data bit 2
                    LCDbot.PIN_DATA3, // LCD data bit 3
                    LCDbot.PIN_DATA4, // LCD data bit 4
                    0, 0, 0, 0);

            if (lcdHandle == -1) {
                System.out.println(" ==>> LCD INIT FAILED");
                running = false;
                return;
            }

            // Up Arrow
            Lcd.lcdCharDef(lcdHandle, 0, new byte[]{
                    0b00100,
                    0b01110,
                    0b11111,
                    0b00100,
                    0b00100,
                    0b00100,
                    0b00100,
                    0b00000
            });

            // Down Arrow
            Lcd.lcdCharDef(lcdHandle, 1, new byte[]{
                    0b00100,
                    0b00100,
                    0b00100,
                    0b00100,
                    0b11111,
                    0b01110,
                    0b00100,
                    0b00000
            });

            Lcd.lcdClear(lcdHandle);

            //power on backlight
            backlightEnable();

            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            DisplayData job = null;
            final int delayTempDefault = 100;
            final int delayTimeDefault = 10;
            final int delayTextDefault = 3;
            int delayTemp = 0;
            int delayTime = 0;
            int delayText = 0;


            while (running) {
                if (flashing) {
                    if (delayFlash == 0) {
                        if (backlight) {
                            backlightDisable();
                        } else {
                            backlightEnable();
                            if (repeatFlash == 0) {
                                flashing = false;
                            } else {
                                repeatFlash--;
                            }
                        }
                        delayFlash = delayFlashDefault;
                    } else {
                        delayFlash--;
                    }
                }
                if (delayTime == 0) {
                    // Update Time
                    Lcd.lcdPosition(lcdHandle, 0, 0);
                    Lcd.lcdPuts(lcdHandle, df.format(new Date()));
                    //Update Connection Status
                    Lcd.lcdPosition(lcdHandle, 9, 0);
                    Lcd.lcdPutchar(lcdHandle, (lcdBot.isConnected()) ? (byte) 0b0 : (byte) 0b1);
                    delayTime = delayTimeDefault;
                } else {
                    delayTime--;
                }
                if (delayTemp == 0) {
                    if (temperatureController.isReady()) {
                        Lcd.lcdPosition(lcdHandle, 11, 0);
                        double tempDouble = temperatureController.getTemperature(0);
                        DecimalFormat decimalFormat = new DecimalFormat((tempDouble > 99.9) ? "#" : "#.0");
                        String tempString = decimalFormat.format(tempDouble) + "C";

                        while (11 + tempString.length() < 16) {
                            tempString = " " + tempString;
                        }
                        Lcd.lcdPuts(lcdHandle, tempString);
                        delayTemp = delayTempDefault;
                    }
                } else {
                    delayTemp--;
                }

                if (job == null) {
                    job = displayQueue.poll();
                    delayText = 0;
                }
                if (job != null) {
                    if (delayText == 0) {
                        Lcd.lcdPosition(lcdHandle, job.getCol(), job.getRow());
                        Lcd.lcdPuts(lcdHandle, job.getText());

                        if (job.isDone()) {
                            job = null;
                        }
                        delayText = delayTextDefault;
                    } else {
                        delayText--;
                    }
                }

                Thread.sleep(100);
            }

            // DisplayController has finished
            Lcd.lcdClear(lcdHandle);
            lcdHandle = -1;

            // power off backlight
            backlightDisable();

            // power off lcd
            Gpio.digitalWrite(LCDbot.PIN_LCD, false);

        } catch (InterruptedException ex) {
            Logger.getLogger(DisplayController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean isRunning() {
        return running;
    }

    void waitForFinish() {
        try {
            thread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(DisplayController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean isFlashing() {
        return flashing;
    }

    private void backlightEnable() {
        if (!backlight) {
            backlight = true;
            Gpio.digitalWrite(LCDbot.PIN_LED, true);
        }
    }

    private void backlightDisable() {
        if (backlight) {
            backlight = false;
            Gpio.digitalWrite(LCDbot.PIN_LED, false);
        }
    }

    public void flash(int repeat, int delay) {
        flashing = true;
        repeatFlash = repeat;
        delayFlashDefault = delay;
        delayFlash = 0;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itsonlybinary.lcdbot;

import com.pi4j.wiringpi.Lcd;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ItsOnlyBinary
 */
public class DisplayController implements Runnable {

    private final ConcurrentLinkedQueue<DisplayData> displayQueue;
    private final int lcdHandle;
    private final LCDbot lcdBot;
    private final DecimalFormat decimalFormat;

    DisplayController(LCDbot lcdBot) {
        this.lcdBot = lcdBot;
        this.decimalFormat = lcdBot.getDecimalFormat();
        displayQueue = new ConcurrentLinkedQueue<>();
        lcdHandle = Lcd.lcdInit(
                LCDbot.LCD_ROWS, // number of row supported by LCD
                LCDbot.LCD_COLUMNS, // number of columns supported by LCD
                LCDbot.LCD_BITS, // number of bits used to communicate to LCD
                LCDbot.PIN_RS, // LCD RS pin
                LCDbot.PIN_E, // LCD strobe pin
                LCDbot.PIN_DATA1, // LCD data bit 1
                LCDbot.PIN_DATA2, // LCD data bit 2
                LCDbot.PIN_DATA3, // LCD data bit 3
                LCDbot.PIN_DATA4, // LCD data bit 4
                0, 0, 0, 0);
        if (lcdHandle == -1) {
            System.out.println(" ==>> LCD INIT FAILED");
            return;
        }

        Lcd.lcdClear(lcdHandle);
    }

    public void add(DisplayData data) {
        displayQueue.add(data);
    }

    @Override
    public void run() {
        if (lcdHandle == -1) {
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        DisplayData job = null;
        int delayTemp = 0;
        int delayTime = 0;
        int delayText = 0;
        while (true) {
            try {
                if (delayTime == 0) {
                    Lcd.lcdPosition(lcdHandle, 0, 0);
                    Lcd.lcdPuts(lcdHandle, df.format(new Date()));
                    delayTime = 10;
                }
                if (delayTemp == 0) {
                    Lcd.lcdPosition(lcdHandle, 8, 0);
                    String temp = decimalFormat.format(lcdBot.getTemp()) + "C";
                    while (8 + temp.length() < 16) {
                        temp = " " + temp;
                    }
                    Lcd.lcdPuts(lcdHandle, temp);
                    delayTemp = 100;
                }
                if (job == null) {
                    job = displayQueue.poll();
                    delayText = 0;
                }
                if (job != null && delayText == 0) {
                    Lcd.lcdPosition(lcdHandle, job.getCol(), job.getRow());
                    Lcd.lcdPuts(lcdHandle, job.getText());

                    if (job.isDone()) {
                        job = null;
                    }
                    delayText = 3;
                }
                delayTemp--;
                delayTime--;
                delayText--;
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(DisplayController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

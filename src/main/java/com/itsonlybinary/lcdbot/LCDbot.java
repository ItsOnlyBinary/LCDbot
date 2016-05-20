/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itsonlybinary.lcdbot;

import com.pi4j.wiringpi.Gpio;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

/**
 *
 * @author ItsOnlyBinary
 */
public class LCDbot {

    private final static String botNick = "LCDbot";
    private final static String botChannel = "#test";
    private final static String botHost = "irc.someserver.org";
    private final static int botPort = 6667;

    public final static int LCD_ROWS = 2;
    public final static int LCD_COLUMNS = 16;
    public final static int LCD_BITS = 4;

    public final static int PIN_RS = 11;
    public final static int PIN_E = 10;
    public final static int PIN_DATA1 = 0;
    public final static int PIN_DATA2 = 1;
    public final static int PIN_DATA3 = 2;
    public final static int PIN_DATA4 = 3;

    public final static int PIN_LED = 4;
    public final static int PIN_BUTTON = 6;
    public final static String W1_DEVICES_PATH = "/sys/bus/w1/devices/";
    public final static String W1_SLAVE = "/w1_slave";
    public final static String DEVICE_NAME = "28-031581dd3aff";

    public static void main(String args[]) throws InterruptedException {
        if (Gpio.wiringPiSetup() == -1) {
            System.out.println(" ==>> GPIO SETUP FAILED");
            return;
        }
        LCDbot lcdBot = new LCDbot();
    }

    private Client client;

    private final DecimalFormat decimalFormat;
    private final DisplayController displayController;

    LCDbot() throws InterruptedException {
        System.out.println("LCDbot Starting");

        decimalFormat = new DecimalFormat("#.#");

        // Enable LED GPIO
        Gpio.pinMode(PIN_LED, Gpio.OUTPUT);
        Gpio.digitalWrite(PIN_LED, false);

//        GpioUtil.export(PIN_BUTTON, GpioUtil.DIRECTION_IN);
//        GpioUtil.setEdgeDetection(PIN_BUTTON, GpioUtil.EDGE_BOTH);
//        Gpio.pinMode(PIN_BUTTON, Gpio.INPUT);
//        Gpio.pullUpDnControl(PIN_BUTTON, Gpio.PUD_DOWN);
//        GpioInterrupt.enablePinStateChangeCallback(PIN_BUTTON);
        // Turn Backlight on
        Gpio.digitalWrite(PIN_LED, true);

        displayController = new DisplayController(this);
        new Thread(displayController).start();

        displayController.add(new DisplayData(1, 0, "Connecting...", false));
        botConnect();
        displayController.add(new DisplayData(1, 0, "Connected!", false));
    }

    public DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    private void botConnect() {
        client = Client.builder().nick(botNick).serverHost(botHost).serverPort(botPort).secure(false).build();
        client.getEventManager().registerEventListener(this);
        client.addChannel(botChannel);
    }

    double getTemp() {
        double temp = 0;
        try {
            List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(W1_DEVICES_PATH + DEVICE_NAME + W1_SLAVE), Charset.defaultCharset());
            for (String line : lines) {
                if (line.contains("t=")) {
                    temp = Double.parseDouble(line.substring(line.indexOf("t=") + 2, line.length())) / 1000;
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(LCDbot.class.getName()).log(Level.SEVERE, null, ex);
        }
        return temp;
    }

    @Handler
    public void onChannelMessage(ChannelMessageEvent event) throws InterruptedException, IOException {
        Channel channel = event.getChannel();
        if ("Hello LCDbot".equalsIgnoreCase(event.getMessage())) {
            channel.sendMessage("Hi " + event.getActor().getNick() + "!");
        }
        if (botChannel.equalsIgnoreCase(channel.getName())) {
            if (event.getMessage().equalsIgnoreCase("!temp")) {
                channel.sendMessage("Temperature is " + decimalFormat.format(getTemp()) + " deg C / " + decimalFormat.format((getTemp() * 1.8) + 32) + " deg F");
            } else {
                displayController.add(new DisplayData(1, 0, event.getActor().getNick() + ": " + event.getMessage(), true));
            }
        }
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itsonlybinary.lcdbot;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.Gpio;
import java.io.IOException;
import java.text.DecimalFormat;
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
    public final static Pin PIN_BUTTON = RaspiPin.GPIO_06;
    public final static String W1_DEVICES_PATH = "/sys/bus/w1/devices/";
    public final static String W1_SLAVE = "/w1_slave";
    public final static String DEVICE_NAME = "28-031581dd3aff";

    public static void main(String args[]) throws InterruptedException {
        LCDbot lcdBot = new LCDbot();
    }
    
    private Client client;
    private final GpioController gpio;
    private final DecimalFormat decimalFormat;
    private final DisplayController displayController;
    private final TemperatureController temperatureController;
    private final GpioPinDigitalInput button;
    private String slapee = "";

    LCDbot() throws InterruptedException {
        System.out.println("LCDbot Starting");

        gpio = GpioFactory.getInstance();

        button = gpio.provisionDigitalInputPin(PIN_BUTTON, PinPullResistance.PULL_UP);
        button.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                if (event.getState() == PinState.LOW && slapee.equals("") == false) {
                    String message = "slaps " + slapee + " around with a large trout";
                    client.sendCTCPMessage(botChannel, "ACTION " + message);
                }
            }

        });

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

        temperatureController = new TemperatureController();
        new Thread(temperatureController).start();

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
        return temperatureController.getTemperature();
    }

    @Handler
    public void onChannelMessage(ChannelMessageEvent event) throws InterruptedException, IOException {
        Channel channel = event.getChannel();
        if ("Hello LCDbot".equalsIgnoreCase(event.getMessage())) {
            channel.sendMessage("Hi " + event.getActor().getNick() + "!");
        }

        if (botChannel.equalsIgnoreCase(channel.getName())) {
            if (event.getMessage().equalsIgnoreCase("!temp")) {
                channel.sendMessage("Temperature is " + decimalFormat.format(getTemp()) + " Celsius | " + decimalFormat.format((getTemp() * 1.8) + 32) + " Fahrenheit | " + decimalFormat.format(getTemp() + 273.15) + " Kelvin.");
            } else if (event.getMessage().startsWith("!setslapee ")) {
                slapee = event.getMessage().split(" ")[1];
                channel.sendMessage("Slapee set to: " + slapee);
            } else {
                displayController.add(new DisplayData(1, 0, event.getActor().getNick() + ": " + event.getMessage(), true));
            }
        }
    }

}

package com.itsonlybinary.lcdbot;

import com.itsonlybinary.lcdbot.Commands.DisplayCommand;
import com.itsonlybinary.lcdbot.Commands.TemperatureCommand;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.Gpio;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ChannelUserMode;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.client.ClientConnectedEvent;
import org.kitteh.irc.client.library.event.client.ClientConnectionClosedEvent;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

import java.util.Optional;
import java.util.Set;

/**
 * @author ItsOnlyBinary
 */
public class LCDbot {
    final static int LCD_ROWS = 2;
    final static int LCD_COLUMNS = 16;
    final static int LCD_BITS = 4;
    final static int PIN_RS = 11;
    final static int PIN_E = 10;
    final static int PIN_DATA1 = 0;
    final static int PIN_DATA2 = 1;
    final static int PIN_DATA3 = 2;
    final static int PIN_DATA4 = 3;
    final static int PIN_LCD = 4;  // LCD Power Control
    final static int PIN_LED = 5;  // LCD Backlight
    final static String W1_DEVICES_PATH = "/sys/bus/w1/devices/";
    final static String W1_SLAVE = "/w1_slave";
    private final static String botNick = "LCDbot";
    private final static String botChannel = "#test";
    private final static String botHost = "irc.someserver.org";
    private final static int botPort = 6667;
    private final static Pin PIN_BUTTON_1 = RaspiPin.GPIO_12;
    private final static Pin PIN_BUTTON_2 = RaspiPin.GPIO_13;
    private final static Pin PIN_BUTTON_3 = RaspiPin.GPIO_14;
    private final DisplayController displayController;
    private final TemperatureController temperatureController;
    private Client client;
    private String slapee = "";
    private boolean connected;

    private LCDbot() throws InterruptedException {
        System.out.println("LCDbot Starting");

        GpioController gpio = GpioFactory.getInstance();

        GpioPinDigitalInput switches[] = {
                gpio.provisionDigitalInputPin(PIN_BUTTON_1, PinPullResistance.PULL_UP),
                gpio.provisionDigitalInputPin(PIN_BUTTON_2, PinPullResistance.PULL_UP),
                gpio.provisionDigitalInputPin(PIN_BUTTON_3, PinPullResistance.PULL_UP)
        };

        gpio.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
            if (event.getState() == PinState.LOW) {
                System.out.println("button press");
                onButtonPress(event);
            }
        }, switches);

        // Enable LED GPIO
        Gpio.pinMode(PIN_LCD, Gpio.OUTPUT);
        Gpio.pinMode(PIN_LED, Gpio.OUTPUT);
        Gpio.digitalWrite(PIN_LCD, false);
        Gpio.digitalWrite(PIN_LED, false);

        temperatureController = new TemperatureController();
        temperatureController.enable();

        Thread.sleep(100);
        displayController = new DisplayController(this);
        displayController.enable();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                displayController.disable();
                displayController.waitForFinish();
            }
        });

        Thread.sleep(20);

        displayController.add(new DisplayData(1, 0, "Connecting...", false));
        botConnect();
        displayController.add(new DisplayData(1, 0, "", false));

    }

    public static void main(String args[]) throws InterruptedException {

        new LCDbot();

    }

    private void botConnect() {
        client = Client.builder().nick(botNick).user(botNick.toLowerCase()).serverHost(botHost).serverPort(botPort).secure(false).build();
        client.getEventManager().registerEventListener(new TemperatureCommand(this));
        client.getEventManager().registerEventListener(new DisplayCommand(this));
        client.getEventManager().registerEventListener(this);
    }

    private void onButtonPress(GpioPinDigitalStateChangeEvent event) {
        Pin pin = event.getPin().getPin();
        if (pin == PIN_BUTTON_1) {
            if (displayController.isRunning()) {
                displayController.disable();
            } else {
                displayController.enable();
            }
            return;
        }
        if (pin == PIN_BUTTON_2) {

            return;
        }
        if (pin == PIN_BUTTON_3 && !slapee.equals("")) {
            String message = "slaps " + slapee + " around with a large trout";
            client.sendCTCPMessage(botChannel, "ACTION " + message);
            return;
        }
        System.out.println("Unexpected event in button listener");
        System.out.println("Pin: " + event.getPin().getPin().toString());
    }

    @Handler
    public void onConnect(ClientConnectedEvent event) {
        connected = true;
        client.addChannel(botChannel);
        client.addChannel("#test");
    }

    @Handler
    public void onDisconnect(ClientConnectionClosedEvent event) {
        connected = false;
    }

    @Handler
    public void onChannelMessage(ChannelMessageEvent event) {
        Channel channel = event.getChannel();
        if ("Hello LCDbot".equalsIgnoreCase(event.getMessage())) {
            channel.sendMessage("Hi " + event.getActor().getNick() + "!");
        }

        if (botChannel.equalsIgnoreCase(channel.getName())) {
            if (event.getMessage().startsWith("!setslapee ")) {
                if (isOp(event.getActor(), event.getChannel())) {
                    slapee = event.getMessage().split(" ")[1];
                } else {
                    slapee = event.getActor().getNick();
                }
                channel.sendMessage("Slapee set to " + slapee);
            } else if (!event.getMessage().startsWith("!")) {
                displayController.add(new DisplayData(1, 0, event.getActor().getNick() + ": " + event.getMessage(), true));
            }
        }
    }

    private boolean isOp(User user, Channel channel) {
        Optional<Set<ChannelUserMode>> userModes = channel.getUserModes(user);
        if (userModes.isPresent()) {
            Set<ChannelUserMode> modes = userModes.get();
            for (ChannelUserMode mode : modes) {
                if (mode.getNickPrefix() == '@') {
                    return true;
                }
            }
        }
        return false;
    }

    public TemperatureController getTemperatureController() {
        return temperatureController;
    }

    public DisplayController getDisplayController() {
        return displayController;
    }

    boolean isConnected() {
        return connected;
    }
}

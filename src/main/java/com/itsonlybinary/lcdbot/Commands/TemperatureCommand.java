package com.itsonlybinary.lcdbot.Commands;

import com.itsonlybinary.lcdbot.LCDbot;
import com.itsonlybinary.lcdbot.TemperatureController;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

import java.text.DecimalFormat;

/**
 * Created by ItsOnlyBinary
 */
public class TemperatureCommand {
    private final DecimalFormat decimalFormat;
    private final TemperatureController temperatureController;

    public TemperatureCommand(LCDbot lcdBot) {
        decimalFormat = new DecimalFormat("#.#");
        temperatureController = lcdBot.getTemperatureController();
    }

    @Handler
    public void onChannelMessage(ChannelMessageEvent event) {
        if (event.getMessage().toLowerCase().startsWith("!temp")) {
            String[] args = event.getMessage().split(" ");
            if (args.length == 2) {
                if (temperatureController.isValidSensor(args[1])) {
                    int sensorInt = temperatureController.getSensorInt(args[1]);
                    sendTemperature(event.getChannel(), sensorInt);
                } else {
                    event.getChannel().sendMessage("Temperature: Invalid Sensor! [1-" + temperatureController.getCount() + "]");
                }
            } else {
                for (int i = 0; i < temperatureController.getCount(); i++) {
                    sendTemperature(event.getChannel(), i);
                }
            }
        }
    }

    private void sendTemperature(Channel channel, int sensorInt) {
        double temp = temperatureController.getTemperature(sensorInt);
        channel.sendMessage(
                "Temperature " + (sensorInt + 1) + ": " +
                decimalFormat.format(temp) + " Celsius | " +
                decimalFormat.format((temp * 1.8) + 32) + " Fahrenheit | " +
                decimalFormat.format(temp + 273.15) + " Kelvin."
        );
    }
}

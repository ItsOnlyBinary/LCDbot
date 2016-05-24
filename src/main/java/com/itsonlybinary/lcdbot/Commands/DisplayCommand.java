package com.itsonlybinary.lcdbot.Commands;

import com.itsonlybinary.lcdbot.DisplayController;
import com.itsonlybinary.lcdbot.DisplayData;
import com.itsonlybinary.lcdbot.LCDbot;
import com.itsonlybinary.lcdbot.Util;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

import java.util.StringJoiner;

/**
 * Created by ItsOnlyBinary
 */
public class DisplayCommand {

    private final DisplayController displayControler;

    public DisplayCommand(LCDbot lcdBot) {
        displayControler = lcdBot.getDisplayController();
    }

    @Handler
    public void onChannelMessage(ChannelMessageEvent event) {
        if (event.getMessage().toLowerCase().startsWith("!display")) {
            System.out.println("host was: " + event.getActor().getHost());
            if (!event.getActor().getHost().equals("admin.chillspot.org")) {
                event.sendReply("Display: Error admin only command!");
                return;
            }

            String[] args = event.getMessage().split(" ");
            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("on")) {
                    if (!displayControler.isRunning()) {
                        displayControler.enable();
                        event.sendReply("Display: Powered ON!");
                    } else {
                        event.sendReply("Display: Error the display is already on!");
                    }
                    return;
                }
                if (args[1].equalsIgnoreCase("off")) {
                    if (displayControler.isRunning()) {
                        displayControler.disable();
                        event.sendReply("Display: Powered OFF!");
                    } else {
                        event.sendReply("Display: Error the display is already off!");
                    }
                    return;
                }
                if (args[1].equalsIgnoreCase("add")) {
                    if (args.length > 2) {
                        StringJoiner stringJoiner = new StringJoiner(" ");
                        for (int i = 2; i < args.length; i++) {
                            stringJoiner.add(args[i]);
                        }
                        displayControler.add(new DisplayData(1, 0, stringJoiner.toString(), true));
                    } else {
                        event.sendReply("Display: Error needs message to add!");
                    }
                    return;
                }
                if (args[1].equalsIgnoreCase("flash")) {
                    if (displayControler.isFlashing()) {
                        event.sendReply("Display: Error screen already flashing!");
                    }
                    int repeat = 3;
                    int delay = 1;
                    if (args.length > 3) {
                        if (!Util.isValidInt(args[2], 1, 10)) {
                            event.sendReply("Display: Invalid Repeat int or int out of range [1-10]");
                            return;
                        }
                        if (!Util.isValidInt(args[3], 1, 10)) {
                            event.sendReply("Display: Invalid Delay int or int out of range [1-10]");
                            return;
                        }
                        repeat = Integer.parseInt(args[2]);
                        delay = Integer.parseInt(args[3]);
                    }
                    displayControler.flash(repeat, delay);
                    return;
                }
            }
        }
    }
}

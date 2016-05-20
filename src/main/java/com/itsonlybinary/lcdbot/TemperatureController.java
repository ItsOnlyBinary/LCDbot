/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itsonlybinary.lcdbot;

import static com.itsonlybinary.lcdbot.LCDbot.DEVICE_NAME;
import static com.itsonlybinary.lcdbot.LCDbot.W1_DEVICES_PATH;
import static com.itsonlybinary.lcdbot.LCDbot.W1_SLAVE;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ItsOnlyBinary
 */
public class TemperatureController implements Runnable {

    private double temperature = 0;

    @Override
    public void run() {
        while (true) {
            try {
                List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(W1_DEVICES_PATH + DEVICE_NAME + W1_SLAVE), Charset.defaultCharset());
                for (String line : lines) {
                    if (line.contains("t=")) {
                        setTemperature(Double.parseDouble(line.substring(line.indexOf("t=") + 2, line.length())) / 1000);
                    }
                }
                Thread.sleep(1000);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(TemperatureController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    synchronized double getTemperature() {
        return temperature;
    }

    synchronized void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}

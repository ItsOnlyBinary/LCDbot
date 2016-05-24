package com.itsonlybinary.lcdbot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.itsonlybinary.lcdbot.LCDbot.W1_DEVICES_PATH;
import static com.itsonlybinary.lcdbot.LCDbot.W1_SLAVE;

/**
 * @author ItsOnlyBinary
 */
public class TemperatureController implements Runnable {
    private final String[] sensors;
    private final double[] temperatures;
    private volatile boolean running;
    private boolean ready = false;

    TemperatureController() {
        // Scan for Devices
        File file = new File(W1_DEVICES_PATH);
        sensors = file.list((current, name) -> {
            File testFile = new File(current, name);
            return testFile.isDirectory() && testFile.getName().startsWith("28-");
        });
        temperatures = new double[sensors.length];
    }

    public void enable() {
        if (sensors.length == 0) return;
        running = true;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void disable() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                for (int i = 0; i < sensors.length; i++) {
                    int finalI = i;
                    List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(W1_DEVICES_PATH + sensors[finalI] + W1_SLAVE), Charset.defaultCharset());
                    lines.stream().filter(line -> line.contains("t=")).forEach(line -> setTemperature(finalI, Double.parseDouble(line.substring(line.indexOf("t=") + 2, line.length())) / 1000));
                }
                if (!ready) ready = true;
                Thread.sleep(1000);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(TemperatureController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean isValidSensor(String sensorString) {
        return Util.isValidInt(sensorString) && isValidSensor(Integer.parseInt(sensorString) - 1);
    }

    private boolean isValidSensor(int sensorInt) {
        return sensorInt >= 0 && sensorInt < temperatures.length;
    }

    public double getTemperature(int sensorInt) {
        return temperatures[sensorInt];
    }

    private void setTemperature(int sensorInt, Double temperature) {
        temperatures[sensorInt] = temperature;
    }

    public int getCount() {
        return sensors.length;
    }

    public int getSensorInt(String sensorString) {
        return Integer.parseInt(sensorString) - 1;
    }

    public boolean isReady() {
        return ready;
    }
}

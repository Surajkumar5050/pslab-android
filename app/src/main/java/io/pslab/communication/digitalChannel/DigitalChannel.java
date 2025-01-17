package io.pslab.communication.digitalChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Created by viveksb007 on 26/3/17.
 */

public class DigitalChannel {

    public static final int EVERY_EDGE = 1;
    public static final int DISABLED = 0;
    private static final int EVERY_SIXTEENTH_RISING_EDGE = 5;
    private static final int EVERY_FOURTH_RISING_EDGE = 4;
    private static final int EVERY_RISING_EDGE = 3;
    private static final int EVERY_FALLING_EDGE = 2;
    private static final int CAPTURE_DELAY = 2;
    public static String[] digitalChannelNames = {"LA1", "LA2", "LA3", "LA4", "RES", "EXT", "FRQ"};
    public String channelName, dataType;
    public int initialStateOverride, channelNumber, length, prescalar, trigger, dlength, plotLength, maxTime, mode;
    public double xAxis[], yAxis[], timestamps[];
    boolean initialState;
    double gain, maxT;

    public DigitalChannel(int channelNumber) {
        this.channelNumber = channelNumber;
        this.channelName = digitalChannelNames[channelNumber];
        this.gain = 0;
        this.xAxis = new double[20000];
        this.yAxis = new double[20000];
        this.timestamps = new double[10000];
        this.length = 100;
        this.initialState = false;
        this.prescalar = 0;
        this.dataType = "int";
        this.trigger = 0;
        this.dlength = 0;
        this.plotLength = 0;
        this.maxT = 0;
        this.maxTime = 0;
        this.initialStateOverride = 0;
        this.mode = EVERY_EDGE;
    }

    void setParams(String channelName, int channelNumber) {
        this.channelName = channelName;
        this.channelNumber = channelNumber;
    }

    void setPrescalar(int prescalar) {
        this.prescalar = prescalar;
    }

    public void loadData(LinkedHashMap<String, Integer> initialStates, double[] timestamps) {
        if (initialStateOverride != 0) {
            this.initialState = (initialStateOverride - 1) == 1;
            this.initialStateOverride = 0;
        } else {
            final Integer s = initialStates.get(channelName);
            this.initialState = s != null && s == 1;
        }
        System.arraycopy(timestamps, 0, this.timestamps, 0, timestamps.length);
        this.dlength = timestamps.length;
        double factor = 64;
        ArrayList<Double> diff = new ArrayList<>();
        for (int i = 0; i < this.timestamps.length - 1; i++) {
            diff.add(this.timestamps[i + 1] - this.timestamps[i]);
        }
        for (int i = 0; i < diff.size(); i++) {
            if (diff.get(i) < 0) {  // Counter has rolled over.
                for (int j = i + 1; j < this.timestamps.length; j++) {
                    this.timestamps[j] += (1 << 16) - 1;
                }
            }
        }
        for (int i = 0; i < this.timestamps.length; i++) {
            this.timestamps[i] = (this.timestamps[i] + (i * CAPTURE_DELAY)) / factor;
        }
        if (dlength > 0)
            this.maxT = this.timestamps[this.timestamps.length - 1];
        else
            this.maxT = 0;
    }

    public void generateAxes() {
        int HIGH = 1, LOW = 0, state;
        if (initialState)
            state = LOW;
        else
            state = HIGH;

        if (this.mode == DISABLED) {
            xAxis[0] = 0;
            yAxis[0] = 0;
            this.plotLength = 1;
        } else if (this.mode == EVERY_EDGE) {
            xAxis[0] = 0;
            yAxis[0] = state;
            int i, j;
            for (i = 1, j = 1; i < this.dlength; i++, j++) {
                xAxis[j] = timestamps[i];
                yAxis[j] = state;
                if (state == HIGH)
                    state = LOW;
                else
                    state = HIGH;
                j++;
                xAxis[j] = timestamps[i];
                yAxis[j] = state;
            }
            plotLength = j;
        } else if (this.mode == EVERY_FALLING_EDGE) {
            xAxis[0] = 0;
            yAxis[0] = HIGH;
            int i, j;
            for (i = 1, j = 1; i < this.dlength; i++, j++) {
                xAxis[j] = timestamps[i];
                yAxis[j] = HIGH;
                j++;
                xAxis[j] = timestamps[i];
                yAxis[j] = LOW;
                j++;
                xAxis[j] = timestamps[i];
                yAxis[j] = HIGH;
            }
            state = HIGH;
            plotLength = j;
        } else if (this.mode == EVERY_RISING_EDGE || this.mode == EVERY_FOURTH_RISING_EDGE || this.mode == EVERY_SIXTEENTH_RISING_EDGE) {
            xAxis[0] = 0;
            yAxis[0] = LOW;
            int i, j;
            for (i = 1, j = 1; i < this.dlength; i++, j++) {
                xAxis[j] = timestamps[i];
                yAxis[j] = LOW;
                j++;
                xAxis[j] = timestamps[i];
                yAxis[j] = HIGH;
                j++;
                xAxis[j] = timestamps[i];
                yAxis[j] = LOW;
            }
            state = LOW;
            plotLength = j;
        }

    }

    public double[] getXAxis() {
        return Arrays.copyOfRange(this.xAxis, 0, plotLength);
    }

    public double[] getYAxis() {
        return Arrays.copyOfRange(this.yAxis, 0, plotLength);
    }
}

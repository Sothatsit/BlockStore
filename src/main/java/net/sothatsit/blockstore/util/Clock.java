package net.sothatsit.blockstore.util;

import java.text.DecimalFormat;

public final class Clock {

    private static final DecimalFormat millisecondsFormat = new DecimalFormat("#.##");

    private final long overallStart;
    private long sectionStart;
    private long end;

    private Clock() {
        this.overallStart = System.nanoTime();
        this.sectionStart = overallStart;
        this.end = -1;
    }

    public boolean hasEnded() {
        return end >= 0;
    }

    public String stop() {
        Checks.ensureTrue(!hasEnded(), "Timer has already been stopped.");

        this.end = System.nanoTime();

        return toString();
    }

    public String nextSection() {
        String timing = toString();

        this.sectionStart = System.nanoTime();

        return timing;
    }

    public double getDuration() {
        return (hasEnded() ? end - overallStart : System.nanoTime() - sectionStart) / 1e6;
    }

    @Override
    public String toString() {
        return "(" + millisecondsFormat.format(getDuration()) + " ms)";
    }

    public static Clock start() {
        return new Clock();
    }

}

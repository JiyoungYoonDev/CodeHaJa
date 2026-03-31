package com.codehaja.domain.gamification;

public class TierCalculator {

    public static final int TURTLE_THRESHOLD = 101;
    public static final int RABBIT_THRESHOLD  = 1001;
    public static final int EAGLE_THRESHOLD   = 5001;
    public static final int LION_THRESHOLD    = 15001;

    public static String getTier(int xp) {
        if (xp < TURTLE_THRESHOLD) return "EGG";
        if (xp < RABBIT_THRESHOLD)  return "TURTLE";
        if (xp < EAGLE_THRESHOLD)   return "RABBIT";
        if (xp < LION_THRESHOLD)    return "EAGLE";
        return "LION";
    }

    private TierCalculator() {}
}

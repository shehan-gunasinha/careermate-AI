package com.careermate.jobs;

public enum ExperienceLevel {
    ENTRY(0), JUNIOR(1), MID(3), SENIOR(6), EXECUTIVE(10);

    private final int minYears;

    ExperienceLevel(int minYears) {
        this.minYears = minYears;
    }

    public int getMinYears() {
        return minYears;
    }
}

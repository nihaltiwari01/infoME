package com.mcafirst.infome;

public class TimetableItem {
    public static final int TYPE_DAY = 0;
    public static final int TYPE_ENTRY = 1;

    private int type; // 0 = day, 1 = entry
    private String day;
    private String timePeriod;
    private TimetableEntry entry;

    public TimetableItem(int type, String day) {
        this.type = type;
        this.day = day;
    }

    public TimetableItem(int type, String timePeriod, TimetableEntry entry) {
        this.type = type;
        this.timePeriod = timePeriod;
        this.entry = entry;
    }

    public int getType() { return type; }
    public String getDay() { return day; }
    public String getTimePeriod() { return timePeriod; }
    public TimetableEntry getEntry() { return entry; }
}

package com.mcafirst.infome;

public class TimetableItem {

    public static final int TYPE_DAY = 0;
    public static final int TYPE_ENTRY = 1;

    public int type;
    public String dayName;        // for TYPE_DAY
    public String period;         // e.g., "10:00-11:00"
    public TimetableEntry entry;  // for TYPE_ENTRY

    // 🔹 Day constructor
    public TimetableItem(int type, String dayName) {
        this.type = type;
        this.dayName = dayName;
    }

    // 🔹 Entry constructor
    public TimetableItem(int type, String period, TimetableEntry entry) {
        this.type = type;
        this.period = period;
        this.entry = entry;
    }
}

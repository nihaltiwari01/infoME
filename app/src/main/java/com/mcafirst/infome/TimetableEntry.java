package com.mcafirst.infome;

public class TimetableEntry {
    private String subjectName;
    private String subjectCode;
    private String roomNumber;

    public TimetableEntry() {
        // Required empty constructor
    }

    public TimetableEntry(String subjectName, String subjectCode, String roomNumber) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.roomNumber = roomNumber;
    }

    public String getSubjectName() { return subjectName; }
    public String getSubjectCode() { return subjectCode; }
    public String getRoomNumber() { return roomNumber; }
}

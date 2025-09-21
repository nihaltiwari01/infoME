package com.mcafirst.infome;

import com.google.firebase.database.IgnoreExtraProperties;

// Ensure Firebase can deserialize
@IgnoreExtraProperties
public class TimetableEntry {

    public String subjectName;
    public String subjectCode;  // optional, if you have codes
    public String roomNumber;

    // 🔹 Default constructor required for Firebase
    public TimetableEntry() {
    }

    // 🔹 Convenience constructor
    public TimetableEntry(String subjectName, String subjectCode, String roomNumber) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.roomNumber = roomNumber;
    }

    // Optional: getters and setters
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
}

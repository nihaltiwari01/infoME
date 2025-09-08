package com.mcafirst.infome;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UploadData extends AppCompatActivity {

        Spinner spinnerDay, spinnerTimePeriod, spinnerSubjectName ,spinnerSubjectCode, spinnerRoomNumber;
        EditText editTimePeriod, editSubjectName, editSubjectCode, editRoomNumber;
        Button btnUpload;

        DatabaseReference databaseRef;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_upload_data);

            MaterialToolbar toolbar = findViewById(R.id.topAppBar);
            setSupportActionBar(toolbar);

            // Handle back arrow click in toolbar
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
//                onBackPressed();
                    // Call dispatcher (modern replacement of onBackPressed)
//                getOnBackPressedDispatcher().onBackPressed();
                    finish();
                }

            });

            // Firebase reference
            databaseRef = FirebaseDatabase.getInstance().getReference("Timetable");

            // Initialize views
            spinnerDay = findViewById(R.id.spinnerDay);
            spinnerTimePeriod = findViewById(R.id.spinnerTimePeriod);
            spinnerSubjectName = findViewById(R.id.spinnerSubjectName);
            spinnerSubjectCode = findViewById(R.id.spinnerSubjectCode);
            spinnerRoomNumber = findViewById(R.id.spinnerRoomNumber);
            btnUpload = findViewById(R.id.btnUpload);

            // Spinner values (Days)
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDay.setAdapter(adapter);

            // Spinner values (Subject Name)
            String[] timePeriod = {"10:00 - 10:55", "11:00 - 11:55", "12:00 - 12:55", "13:00 - 13:55", "14:00 - 14:55", "15:00 - 15:55", "16:00 - 16:55", "Other"};
            ArrayAdapter<String> timePeriodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timePeriod);
            timePeriodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTimePeriod.setAdapter(timePeriodAdapter);

            // Spinner values (Subject Name)
            String[] subjectName = {"Programming Concepts (with C)", "Computer Organization", "Mathematical Foundation of Computer Applications", "Digital logic Design", "Communication Skills", "Programming Lab" , "Computer Organization Lab", "Other"};
            ArrayAdapter<String> subjectNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectName);
            subjectNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSubjectName.setAdapter(subjectNameAdapter);

            // Spinner values (Subject Code)
            String[] subjectCodes = {"CA - 401", "CA - 403", "CA - 405", "CA - 407", "CA - 451", "CA - 453", "HS - 451", "Other"};
            ArrayAdapter<String> subjectCodeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectCodes);
            subjectCodeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSubjectCode.setAdapter(subjectCodeAdapter);

            // Spinner values (Room Number)
            String[] roomNumber = {"CA 201", " CA 301", "CA 302", "Lab1" , "English Lab", "Lecture Hall", "Other"};
            ArrayAdapter<String> roomNumbedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomNumber);
            roomNumbedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRoomNumber.setAdapter(roomNumbedAdapter);

            // Upload button click
            btnUpload.setOnClickListener(v -> uploadData());
        }

        private void uploadData() {
            String day = spinnerDay.getSelectedItem().toString();
            String timePeriod = spinnerTimePeriod.getSelectedItem().toString();
            String subjectName = spinnerSubjectName.getSelectedItem().toString();
            String subjectCode = spinnerSubjectCode.getSelectedItem().toString();
            String roomNumber = spinnerRoomNumber.getSelectedItem().toString();

            if (timePeriod.isEmpty() || subjectName.isEmpty() || subjectCode.isEmpty() || roomNumber.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            TimetableEntry entry = new TimetableEntry(subjectName, subjectCode, roomNumber);

            // Save in Firebase -> Timetable/Day/TimePeriod
            databaseRef.child(day).child(timePeriod).setValue(entry)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(UploadData.this, "Data Uploaded Successfully", Toast.LENGTH_SHORT).show();
                        clearFields();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(UploadData.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        private void clearFields() {
            editTimePeriod.setText("");
            editSubjectName.setText("");
            editSubjectCode.setText("");
            editRoomNumber.setText("");
        }
    }

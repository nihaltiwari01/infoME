package com.mcafirst.infome;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TimetableAdapter adapter;
    List<TimetableItem> itemList = new ArrayList<>();
    DatabaseReference ref;
    private String role;   // null until loaded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch role first
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("MasterData")
                .child("registeredUsers")
                .child(currentUser.getUid());

        userRef.get().addOnSuccessListener(snapshot -> {
            role = snapshot.child("role").getValue(String.class);
            // now that we know the role, trigger menu creation
            invalidateOptionsMenu();
        });

        // 🔹 Create Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "timetableChannel",
                    "Timetable Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // 🔹 Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        DatabaseReference masterRef = FirebaseDatabase.getInstance().getReference("MasterData");
        Query query = masterRef.child("registeredUsers").orderByValue().equalTo("teacher");



        // 🔹 Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.mainTopAppBar);
        setSupportActionBar(toolbar);

        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add_timetable) {

                startActivity(new Intent(MainActivity.this, UploadData.class));
                return true;
            } else if (itemId == R.id.action_sign_out) {
                FirebaseAuth.getInstance().signOut();
                Intent loginIntent = new Intent(MainActivity.this, Login_Page.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                Toast.makeText(MainActivity.this, "Signed out successfully!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // 🔹 Recycler setup
        recyclerView = findViewById(R.id.recyclerTimetable);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableAdapter(itemList);
        recyclerView.setAdapter(adapter);

        ref = FirebaseDatabase.getInstance().getReference("Timetable");
        ref.keepSynced(true);
        fetchData();

        // 🔹 Schedule notifications from Firebase
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot daySnapshot : snapshot.getChildren()) {

//                    String dayName = daySnapshot.getKey(); // e.g. "Monday"
                    String dayName = "Monday"; // e.g. "Monday"

                    for (DataSnapshot periodSnapshot : daySnapshot.getChildren()) {
                        String period = periodSnapshot.getKey(); // e.g. "10:00 - 10:55"
                        String subject = periodSnapshot.child("subjectName").getValue(String.class);
                        String room = periodSnapshot.child("roomNumber").getValue(String.class);

                        if (period != null && subject != null && room != null) {
                            String startTime = period.split("-")[0].trim();
//                            String startTime = "17:27";
                            scheduleWeeklyNotification(dayName, startTime, subject, room);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });

        // 🔹 Ask for exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    // 🔹 Schedule weekly notifications
    private void scheduleWeeklyNotification(String dayName, String startTime, String subject, String room) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = sdf.parse(startTime);
            calendar.set(Calendar.HOUR_OF_DAY, date.getHours());
            calendar.set(Calendar.MINUTE, date.getMinutes());
            calendar.set(Calendar.SECOND, 0);

            int dayOfWeek = getDayOfWeek(dayName);
            calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);

            // 🔹 Subtract 5 minutes from the class start time
            calendar.add(Calendar.MINUTE, 0);

//            // 🔹 Test notification after 10 seconds
//            Calendar testCalendar = Calendar.getInstance();
//            testCalendar.add(Calendar.SECOND, 3);
//
//            AlarmManager testAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//            Intent testIntent = new Intent(MainActivity.this, NotificationReceiver.class);
//            testIntent.putExtra("subjectName", subject);
//            testIntent.putExtra("room", room);
//            testIntent.putExtra("startTime", startTime);
//            testIntent.putExtra("dayName", dayName);
//
//            PendingIntent testPendingIntent = PendingIntent.getBroadcast(
//                    MainActivity.this,
//                    subject.hashCode(), // unique ID
//                    testIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//            );
//            testAlarmManager.setExact(
//                    AlarmManager.RTC_WAKEUP,
//                    testCalendar.getTimeInMillis(),
//                    testPendingIntent
//            );


            // If the time has already passed, schedule for next week
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("subjectName", subject);
        intent.putExtra("room", room);
        intent.putExtra("startTime", startTime);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (subject + room).hashCode(), // unique ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7, // every week
                pendingIntent
        );
    }

    private int getDayOfWeek(String dayName) {
        switch (dayName) {
            case "Sunday":
                return Calendar.SUNDAY;
            case "Monday":
                return Calendar.MONDAY;
            case "Tuesday":
                return Calendar.TUESDAY;
            case "Wednesday":
                return Calendar.WEDNESDAY;
            case "Thursday":
                return Calendar.THURSDAY;
            case "Friday":
                return Calendar.FRIDAY;
            case "Saturday":
                return Calendar.SATURDAY;
            default:
                return Calendar.MONDAY;
        }
    }

    // 🔹   Fetch timetable for RecyclerView
    private void fetchData() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();

                // 1️⃣ Define the desired weekday order
                List<String> dayOrder = new ArrayList<>();
                dayOrder.add("Monday");
                dayOrder.add("Tuesday");
                dayOrder.add("Wednesday");
                dayOrder.add("Thursday");
                dayOrder.add("Friday");
                dayOrder.add("Saturday");
                dayOrder.add("Sunday");

                // 2️⃣ Collect actual day keys that exist in Firebase
                List<String> availableDays = new ArrayList<>();
                for (DataSnapshot daySnap : snapshot.getChildren()) {
                    availableDays.add(daySnap.getKey());
                }

                // 3️⃣ Sort availableDays based on dayOrder
                availableDays.sort((d1, d2) ->
                        Integer.compare(dayOrder.indexOf(d1), dayOrder.indexOf(d2))
                );

                // 4️⃣ Iterate in the custom order and add to the list
                for (String day : availableDays) {
                    DataSnapshot daySnap = snapshot.child(day);

                    // Add the day header
                    itemList.add(new TimetableItem(TimetableItem.TYPE_DAY, day));

                    // If you also want each day's periods sorted by time:
                    List<DataSnapshot> periods = new ArrayList<>();
                    for (DataSnapshot timeSnap : daySnap.getChildren()) {
                        periods.add(timeSnap);
                    }
                    // Sort periods if keys are like "10:00 - 10:55"
                    periods.sort((a, b) -> a.getKey().compareTo(b.getKey()));
                    for (DataSnapshot timeSnap : periods) {
                        String timePeriod = timeSnap.getKey();
                        TimetableEntry entry = timeSnap.getValue(TimetableEntry.class);
                        itemList.add(new TimetableItem(TimetableItem.TYPE_ENTRY, timePeriod, entry));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);

        // Read cached role
        String role = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("user_role", "");

        if ("student".equalsIgnoreCase(role)) {
            menu.findItem(R.id.action_add_timetable).setVisible(false);
        }
        return true;
    }
}

package com.mcafirst.infome;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TimetableAdapter adapter;
    List<TimetableItem> itemList = new ArrayList<>();
    DatabaseReference ref;
    private String role;
    private static final boolean DEBUG_MODE = false;  // false for production

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch role
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("MasterData")
                    .child("registeredUsers")
                    .child(currentUser.getUid());

            userRef.get().addOnSuccessListener(snapshot -> {
                role = snapshot.child("role").getValue(String.class);
                invalidateOptionsMenu();
            });
        }

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "timetableChannel",
                    "Timetable Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.mainTopAppBar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add_timetable) {
                startActivity(new Intent(MainActivity.this, UploadTimetableData.class));
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

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerTimetable);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableAdapter(itemList);
        recyclerView.setAdapter(adapter);

        // Firebase reference
        ref = FirebaseDatabase.getInstance().getReference("Timetable");
        ref.keepSynced(true);

        // Fetch data and schedule notifications
        fetchData();
        rescheduleNotifications();

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
// 🔹 Switch from debug to production
        if (!DEBUG_MODE) {
            cancelDebugAlarms();  // remove leftover test alarms
            rescheduleNotifications();  // schedule real alarms
        }

    }
    // 🔹 Cancel all previous debug alarms
    private void cancelDebugAlarms() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        Set<String> scheduledClasses = prefs.getStringSet("scheduled_classes", new HashSet<>());
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        for (String key : scheduledClasses) {
            Intent intent = new Intent(this, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    key.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d("AlarmScheduler", "[CANCELLED] " + key);
            }
        }

        // Clear saved keys
        prefs.edit().putStringSet("scheduled_classes", new HashSet<>()).apply();
    }


    // Reschedule notifications
    private void rescheduleNotifications() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Cancel all old alarms
        for (String key : prefs.getStringSet("scheduled_classes", new HashSet<>())) {
            Intent intent = new Intent(this, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    key.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
        }

        // Clear old set
        prefs.edit().putStringSet("scheduled_classes", new HashSet<>()).apply();

        // If DEBUG_MODE, schedule only one test notification
        if (DEBUG_MODE) {
            scheduleWeeklyNotification("Monday", "10:00", "TEST SUBJECT", "Room 101");
            return; // skip the rest
        }

        // 🔹 Production mode: fetch all timetable entries
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> newSet = new HashSet<>();
                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    String dayName = daySnapshot.getKey();
                    for (DataSnapshot periodSnapshot : daySnapshot.getChildren()) {
                        String period = periodSnapshot.getKey();
                        String subject = periodSnapshot.child("subjectName").getValue(String.class);
                        String room = periodSnapshot.child("roomNumber").getValue(String.class);

                        if (period != null && subject != null && room != null) {
                            String startTime = period.split("-")[0].trim();
                            scheduleWeeklyNotification(dayName, startTime, subject, room);
                            newSet.add(subject + "|" + room + "|" + startTime + "|" + dayName);
                        }
                    }
                }
                prefs.edit().putStringSet("scheduled_classes", newSet).apply();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });
    }


    private void scheduleWeeklyNotification(String dayName,
                                            String startTime,
                                            String subject,
                                            String room) {

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("subjectName", subject);
        intent.putExtra("room", room);
        intent.putExtra("startTime", startTime);
        intent.putExtra("dayName", dayName);

        int requestCode = (subject + room + startTime + dayName).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel any existing alarm for this PendingIntent
        alarmManager.cancel(pendingIntent);

        if (DEBUG_MODE) {
            // 🔹 Only 1 test notification for debug
            Calendar testCalendar = Calendar.getInstance();
            testCalendar.add(Calendar.SECOND, 30); // fires in 30 seconds

            PendingIntent testIntent = PendingIntent.getBroadcast(
                    this,
                    12345, // fixed request code for DEBUG
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(testIntent);
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    testCalendar.getTimeInMillis(),
                    testIntent
            );

            Log.d("AlarmScheduler", "[DEBUG] Scheduled TEST notification for: " + subject);
            return; // stop scheduling other alarms in DEBUG
        }

        // 🔹 Production mode: schedule weekly repeating alarm
        int hour = Integer.parseInt(startTime.split(":")[0]);
        int minute = Integer.parseInt(startTime.split(":")[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, getDayOfWeek(dayName));
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute - 5); // 5 minutes early

        // If the time is in the past for this week, move to next week
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );

        Log.d("AlarmScheduler", "Scheduled: " + subject + " on " + dayName +
                " at " + startTime + " (next fire: " + calendar.getTime() + ")");
    }



    private int getDayOfWeek(String dayName) {
        switch (dayName) {
            case "Sunday": return Calendar.SUNDAY;
            case "Monday": return Calendar.MONDAY;
            case "Tuesday": return Calendar.TUESDAY;
            case "Wednesday": return Calendar.WEDNESDAY;
            case "Thursday": return Calendar.THURSDAY;
            case "Friday": return Calendar.FRIDAY;
            case "Saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    // Fetch timetable for RecyclerView
    private void fetchData() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                List<String> dayOrder = List.of("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday");
                List<String> availableDays = new ArrayList<>();
                for (DataSnapshot daySnap : snapshot.getChildren()) availableDays.add(daySnap.getKey());
                availableDays.sort((d1,d2) -> Integer.compare(dayOrder.indexOf(d1), dayOrder.indexOf(d2)));

                for (String day : availableDays) {
                    DataSnapshot daySnap = snapshot.child(day);
                    itemList.add(new TimetableItem(TimetableItem.TYPE_DAY, day));

                    List<DataSnapshot> periods = new ArrayList<>();
                    for (DataSnapshot timeSnap : daySnap.getChildren()) periods.add(timeSnap);
                    periods.sort((a,b) -> a.getKey().compareTo(b.getKey()));

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
        String rolePref = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("user_role", "");
        if ("student".equalsIgnoreCase(rolePref)) {
            menu.findItem(R.id.action_add_timetable).setVisible(false);
        }
        return true;
    }
}

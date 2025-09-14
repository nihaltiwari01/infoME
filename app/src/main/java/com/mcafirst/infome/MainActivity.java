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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "timetableChannel",
                    "Timetable Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        MaterialToolbar toolbar = findViewById(R.id.mainTopAppBar);
        setSupportActionBar(toolbar);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add_timetable) {
                // Handle click here
                Toast.makeText(MainActivity.this, "Settings clicked!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, UploadData.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add_timetable) {
                // Handle click here
                Toast.makeText(MainActivity.this, "Settings clicked!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, UploadData.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.action_sign_out) {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();

                // Optional: redirect to login screen after sign out
                Intent loginIntent = new Intent(MainActivity.this, Login_Page.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);

                Toast.makeText(MainActivity.this, "Signed out successfully!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });


        recyclerView = findViewById(R.id.recyclerTimetable);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TimetableAdapter(itemList);
        recyclerView.setAdapter(adapter);

        ref = FirebaseDatabase.getInstance().getReference("Timetable");
        fetchData();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    String dayName = daySnapshot.getKey(); // e.g. "Monday"

                    for (DataSnapshot periodSnapshot : daySnapshot.getChildren()) {
                        String period = periodSnapshot.getKey(); // e.g. "10:00 - 10:55"
                        String subject = periodSnapshot.child("subjectName").getValue(String.class);
                        String room = periodSnapshot.child("roomNumber").getValue(String.class);

                        if (period != null && subject != null && room != null) {
                            String startTime = period.split("-")[0].trim();
//                            String startTime = "01:46";
                            scheduleWeeklyNotification(dayName, startTime, subject, room);

                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.SECOND, 5); // fire after 10 seconds

                            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            Intent intent = new Intent(MainActivity.this, NotificationReceiver.class);
                            intent.putExtra("subjectName", subject);
                            intent.putExtra("room", room);

                            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                    MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );

                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent); // Opens system settings where user enables it
            }
        }




    }

    private void scheduleWeeklyNotification(String dayName, String time, String subject, String room) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = sdf.parse(time);
            calendar.set(Calendar.HOUR_OF_DAY, date.getHours());
            calendar.set(Calendar.MINUTE, date.getMinutes());
            calendar.set(Calendar.SECOND, 0);

            int dayOfWeek = getDayOfWeek(dayName);
            calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("subjectName", subject);
        intent.putExtra("room", room);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
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


    private void fetchData() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();

                for (DataSnapshot daySnap : snapshot.getChildren()) {
                    String day = daySnap.getKey();
                    itemList.add(new TimetableItem(TimetableItem.TYPE_DAY, day));

                    for (DataSnapshot timeSnap : daySnap.getChildren()) {
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
        return true;
    }

}

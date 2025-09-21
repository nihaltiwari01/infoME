package com.mcafirst.infome.Fragments;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
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
import com.mcafirst.infome.Login_Page;
import com.mcafirst.infome.NotificationReceiver;
import com.mcafirst.infome.R;
import com.mcafirst.infome.TimetableAdapter;
import com.mcafirst.infome.TimetableEntry;
import com.mcafirst.infome.TimetableItem;
import com.mcafirst.infome.UploadTimetableData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScheduleFragment extends Fragment {

    private RecyclerView recyclerView;
    private TimetableAdapter adapter;
    private List<TimetableItem> itemList = new ArrayList<>();
    private DatabaseReference ref;
    private String role;
    private static final boolean DEBUG_MODE = false;

    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Toolbar
        MaterialToolbar toolbar = view.findViewById(R.id.mainTopAppBar);
        if (getActivity() != null) {
            ((androidx.appcompat.app.AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            setHasOptionsMenu(true);
        }

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add_timetable) {
                startActivity(new Intent(requireContext(), UploadTimetableData.class));
                return true;
            } else if (id == R.id.action_sign_out) {
                FirebaseAuth.getInstance().signOut();
                Intent loginIntent = new Intent(requireContext(), Login_Page.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                Toast.makeText(requireContext(), "Signed out successfully!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("MasterData")
                    .child("registeredUsers")
                    .child(currentUser.getUid());

            userRef.get().addOnSuccessListener(snapshot -> {
                role = snapshot.child("role").getValue(String.class);
                if (getActivity() != null) getActivity().invalidateOptionsMenu();
            });
        }

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "timetableChannel",
                    "Timetable Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        // Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerTimetable);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimetableAdapter(itemList);
        recyclerView.setAdapter(adapter);

        // Firebase ref
        ref = FirebaseDatabase.getInstance().getReference("Timetable");
        ref.keepSynced(true);

        fetchData();
        rescheduleNotifications();

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        if (!DEBUG_MODE) {
            cancelDebugAlarms();
            rescheduleNotifications();
        }
    }

    private void cancelDebugAlarms() {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        Set<String> scheduledClasses = prefs.getStringSet("scheduled_classes", new HashSet<>());
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        for (String key : scheduledClasses) {
            Intent intent = new Intent(requireContext(), NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
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

        prefs.edit().putStringSet("scheduled_classes", new HashSet<>()).apply();
    }

    private void rescheduleNotifications() {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        // Cancel old alarms
        for (String key : prefs.getStringSet("scheduled_classes", new HashSet<>())) {
            Intent intent = new Intent(requireContext(), NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    key.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        }

        prefs.edit().putStringSet("scheduled_classes", new HashSet<>()).apply();

        if (DEBUG_MODE) {
            scheduleWeeklyNotification("Monday", "10:00", "TEST SUBJECT", "Room 101");
            return;
        }

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

    private void scheduleWeeklyNotification(String dayName, String startTime, String subject, String room) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("subjectName", subject);
        intent.putExtra("room", room);
        intent.putExtra("startTime", startTime);
        intent.putExtra("dayName", dayName);

        int requestCode = (subject + room + startTime + dayName).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager == null) return;

        alarmManager.cancel(pendingIntent);

        if (DEBUG_MODE) {
            Calendar testCalendar = Calendar.getInstance();
            testCalendar.add(Calendar.SECOND, 30);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, testCalendar.getTimeInMillis(), pendingIntent);
            return;
        }

        int hour = Integer.parseInt(startTime.split(":")[0]);
        int minute = Integer.parseInt(startTime.split(":")[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, getDayOfWeek(dayName));
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute - 5);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );
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

    private void fetchData() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();

                List<String> dayOrder = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
                List<String> availableDays = new ArrayList<>();
                for (DataSnapshot daySnap : snapshot.getChildren()) {
                    availableDays.add(daySnap.getKey());
                }

                availableDays.sort((d1, d2) -> Integer.compare(dayOrder.indexOf(d1), dayOrder.indexOf(d2)));

                for (String day : availableDays) {
                    DataSnapshot daySnap = snapshot.child(day);
                    itemList.add(new TimetableItem(TimetableItem.TYPE_DAY, day));

                    List<DataSnapshot> periods = new ArrayList<>();
                    for (DataSnapshot timeSnap : daySnap.getChildren()) {
                        periods.add(timeSnap);
                    }
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
                Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String rolePref = prefs.getString("user_role", "");

        if ("student".equalsIgnoreCase(rolePref)) {
            menu.findItem(R.id.action_add_timetable).setVisible(false);
        }
    }
}

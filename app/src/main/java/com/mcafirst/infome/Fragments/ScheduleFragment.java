package com.mcafirst.infome.Fragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import android.graphics.Color;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.mcafirst.infome.NotificationReceiver;
import com.mcafirst.infome.R;
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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
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
import com.google.firebase.database.*;
import com.mcafirst.infome.Login_Page;
import com.mcafirst.infome.TimetableAdapter;
import com.mcafirst.infome.TimetableEntry;
import com.mcafirst.infome.TimetableItem;
import com.mcafirst.infome.UploadTimetableData;

import java.util.*;

public class ScheduleFragment extends Fragment {

    private RecyclerView recyclerView;
    private TimetableAdapter adapter;
    private final List<TimetableItem> itemList = new ArrayList<>();
    private DatabaseReference ref;
    private String role;
    private static final boolean DEBUG_MODE = false; // false for production

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);   // fragment will provide its own menu
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Transparent status bar
        if (getActivity() != null) {
            WindowCompat.setDecorFitsSystemWindows(getActivity().getWindow(), false);
            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        // Toolbar
        MaterialToolbar toolbar = view.findViewById(R.id.mainTopAppBar);
        if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
            ((androidx.appcompat.app.AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        // CollapsingToolbar title behavior
        CollapsingToolbarLayout collapsingToolbar = view.findViewById(R.id.collapsingToolbar);
        collapsingToolbar.setTitleEnabled(false);

             toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add_timetable) {
                startActivity(new Intent(requireContext(), UploadTimetableData.class));
                return true;
            } else if (itemId == R.id.action_sign_out) {
                FirebaseAuth.getInstance().signOut();
                Intent loginIntent = new Intent(requireContext(), Login_Page.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                Toast.makeText(requireContext(), "Signed out successfully!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerTimetable);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimetableAdapter(itemList);
        recyclerView.setAdapter(adapter);

        // Firebase reference
        ref = FirebaseDatabase.getInstance().getReference("Timetable");
        ref.keepSynced(true);

        // Fetch role
        fetchUserRole();

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "timetableChannel",
                    "Timetable Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Fetch data and schedule notifications
        fetchData();
        rescheduleNotifications();

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        // Switch from debug to production
        if (!DEBUG_MODE) {
            cancelDebugAlarms();
            rescheduleNotifications();
        }
    }

    private void fetchUserRole() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("MasterData")
                .child("registeredUsers")
                .child(currentUser.getUid());

        userRef.get().addOnSuccessListener(snapshot -> {
            role = snapshot.child("role").getValue(String.class);
            requireActivity().invalidateOptionsMenu();
        });
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

        for (String key : prefs.getStringSet("scheduled_classes", new HashSet<>())) {
            Intent intent = new Intent(requireContext(), NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    key.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
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

    private void scheduleWeeklyNotification(String dayName,
                                            String startTime,
                                            String subject,
                                            String room) {
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

        alarmManager.cancel(pendingIntent);

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
        Log.d("AlarmScheduler", "Scheduled: " + subject + " on " + dayName);
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
                List<String> dayOrder = Arrays.asList("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday");
                List<String> availableDays = new ArrayList<>();
                for (DataSnapshot daySnap : snapshot.getChildren()) {
                    availableDays.add(daySnap.getKey());
                }
                availableDays.sort(Comparator.comparingInt(dayOrder::indexOf));

                for (String day : availableDays) {
                    DataSnapshot daySnap = snapshot.child(day);
                    itemList.add(new TimetableItem(TimetableItem.TYPE_DAY, day));

                    List<DataSnapshot> periods = new ArrayList<>();
                    for (DataSnapshot timeSnap : daySnap.getChildren()) periods.add(timeSnap);
                    periods.sort(Comparator.comparing(DataSnapshot::getKey));

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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.top_app_bar_menu, menu);
        String rolePref = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("user_role", "");
        if ("student".equalsIgnoreCase(rolePref)) {
            menu.findItem(R.id.action_add_timetable).setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }
}

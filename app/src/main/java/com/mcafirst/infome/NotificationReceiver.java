package com.mcafirst.infome;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String subjectName = intent.getStringExtra("subjectName");
        String room = intent.getStringExtra("room");
        String startTime = intent.getStringExtra("startTime");
        String dayName = intent.getStringExtra("dayName");
        // Build proper message
        String message;
        if (startTime != null) {
            message = subjectName + " in " + room + " at " + startTime;
        } else {
            message = subjectName + " in " + room;
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        // 🔹 Create intent when user taps notification → opens MainActivity
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (subjectName + room).hashCode(), // unique ID
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 🔹 Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "timetableChannel")
                .setSmallIcon(R.drawable.appiconsl) // must be white-only icon for status bar
                .setContentTitle("Upcoming Class on " +  dayName)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent) // open app on click
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false);

        // 🔹 Show notification if permission granted
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.notify((subjectName + room).hashCode(), builder.build());
    }
}

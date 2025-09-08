package com.mcafirst.infome;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String subjectName = intent.getStringExtra("subjectName");
        String room = intent.getStringExtra("room");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "timetableChannel")
                .setSmallIcon(R.drawable.appiconsl) // replace with your app's icon
                .setContentTitle("Upcoming Class")
                .setContentText(subjectName + " in " + room)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}

package com.mcafirst.infome;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "timetableChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String subject = intent.getStringExtra("subjectName");
        String room = intent.getStringExtra("room");
        String startTime = intent.getStringExtra("startTime");
        String dayName = intent.getStringExtra("dayName");

        if (subject == null || room == null) return;

        int notificationId = (subject + room + startTime + dayName).hashCode();

        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_infome)
                .setContentTitle(subject)
                .setContentText("Room: " + room + " | Starts at: " + startTime)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}

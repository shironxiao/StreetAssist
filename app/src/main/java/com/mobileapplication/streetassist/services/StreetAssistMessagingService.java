package com.mobileapplication.streetassist.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminNotificationActivity;

import java.util.HashMap;
import java.util.Map;

public class StreetAssistMessagingService extends FirebaseMessagingService {
    private static final String TAG = "SAMessagingService";
    private static final String CHANNEL_ID = "admin_notifications_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // We only care about notifications if the user is an admin
        // This is a safety check; server-side logic should handle targeting
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        } else if (remoteMessage.getData().size() > 0) {
            // Handle data-only messages (often used by Cloud Functions to trigger custom logic)
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            if (title != null && message != null) {
                showNotification(title, message, remoteMessage.getData());
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        updateTokenInFirestore(token);
    }

    private void updateTokenInFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated for user: " + uid))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM Token: " + e.getMessage()));
        }
    }

    private void showNotification(String title, String messageBody, Map<String, String> data) {
        Intent intent = new Intent(this, AdminNotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Pass data if needed for direct navigation
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.badge_notification) // Use existing badge or app icon
                        .setContentTitle(title != null ? title : "StreetAssist Alert")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Admin Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}

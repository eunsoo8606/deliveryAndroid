package com.delivery.admin.push

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.delivery.admin.MainActivity
import com.delivery.admin.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")
        val link = remoteMessage.data["link"] ?: "https://default.link"

        // 메시지의 알림 부분이 있는지 확인 (notification 필드)
        if (remoteMessage.notification != null) {
            Log.d("FCM", "Message Notification Body: ${remoteMessage.notification?.body}")
            sendNotification(
                remoteMessage.notification?.title ?: "FCM Message",
                remoteMessage.notification?.body ?: "No message body",
                link
            )
        } else if (remoteMessage.data.isNotEmpty()) {
            // 알림 메시지가 없고 데이터 메시지만 있는 경우
            val title = remoteMessage.data["title"] ?: "Default Title"
            val messageBody = remoteMessage.data["body"] ?: "No message body"

            Log.d("FCM", "Data 메시지로 전달된 제목: $title")
            Log.d("FCM", "Data 메시지로 전달된 내용: $messageBody")
            Log.d("FCM", "Data 메시지로 전달된 링크: $link")

            // Data 메시지를 통해 알림을 생성
            sendNotification(title, messageBody, link)
        }
    }


    // 기존 sendNotification 메서드를 그대로 사용
    private fun sendNotification(title: String, messageBody: String, link: String) {
        val channelId = "default_channel_id"
        val notificationId = 1

        // MainActivity를 실행하는 Intent 생성
        // MainActivity를 실행하는 Intent 생성
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "OPEN_LINK_ACTIVITY"  // Intent 액션 설정
            putExtra("link", link)  // 링크 데이터 전달
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // PendingIntent 생성
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // 알림 빌더 설정
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 앱 아이콘 설정
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // 우선순위 설정
            .setContentIntent(pendingIntent)  // 알림 클릭 시 실행될 인텐트 설정


        // NotificationManager를 통해 알림 발송
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

}

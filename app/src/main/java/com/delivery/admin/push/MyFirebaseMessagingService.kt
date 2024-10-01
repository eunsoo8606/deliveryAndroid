package com.delivery.admin.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.delivery.admin.MainActivity
import com.delivery.admin.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // FCM 메시지를 수신했을 때 실행되는 메서드
        Log.d("FCM", "From: ${remoteMessage.from}")

        // 메시지의 알림 부분이 있는지 확인
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
        }

        // Data 메시지에서 링크 데이터가 있는지 확인
        remoteMessage.data["link"]?.let { link ->
            Log.d("FCM", "Data 메시지로 전달된 링크: $link")

            // 웹뷰 Activity로 링크를 전달하는 Intent 생성
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("link", link)  // 링크 데이터를 인텐트에 추가
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // 웹뷰 Activity 실행
            startActivity(intent)
        }
    }


    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "default_channel_id"
        val notificationId = 1

        // Android 8.0 이상에서는 알림 채널을 만들어야 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 앱 아이콘 설정
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notificationBuilder.build())
        }
    }



}

package com.delivery.admin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // SplashActivity를 종료해서 뒤로 가기 버튼을 눌러도 다시 나오지 않도록 함
        }, 3000) // 3초 동안 스플래시 화면을 보여줌
    }
}

package com.delivery.admin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.messaging.FirebaseMessaging
import java.net.URISyntaxException
import android.Manifest
import com.delivery.admin.utils.NotificationUtils
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    // 멤버 변수 선언
    private lateinit var webView: WebView
    private lateinit var childView: WebView
    private lateinit var webSettings: WebSettings
    private var lastTimeBackPressed: Long = 0
    private lateinit var progressDialog: ProgressDialog
    //https://delivery-admin.com/
    private var myURL: String = "https://delivery-admin.com/"
    private var childURL: String = ""
    private var fcmToken: String? = null
    private var count = 1
    private val REQUEST_NOTIFICATION_PERMISSION = 1
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 앱이 처음 실행되었을 때만 인텐트 처리
        if (savedInstanceState == null) {
            handleIntent(intent)
        }


        // Android 13 이상에서 알림 권한 요청
        checkNotificationPermission();
        defalutSetting();
    }


    private fun defalutSetting(){
        // Android 13 이상에서 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        // FCM 토큰 가져오기
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                Log.d("FCM", "FCM Token: $fcmToken")
            } else {
                Log.w("FCM", "FCM Token 가져오기 실패", task.exception)
            }
        }

        // Android 8.0 이상에서 알림 채널 설정
        // 앱이 실행될 때 알림 채널을 생성
        NotificationUtils.createNotificationChannel(
            this,
            "default_channel_id",
            "Default Channel"
        )

        // WebView 초기화
        webView = findViewById(R.id.webView)

        // 쿠키 허용 설정
        val cookieManager: CookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // WebView 자바스크립트 활성화
        webView.settings.javaScriptEnabled = true

        // WebView 설정
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        // User-Agent 설정
        webView.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.152 Mobile Safari/537.36"

        // 멀티 윈도우 설정
        webView.settings.setSupportMultipleWindows(true)

        // ProgressDialog 설정
        progressDialog = ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)

        // 앱이 처음 실행되었을 때만 인텐트 처리

        val link = intent?.getStringExtra("link")
        if (!link.isNullOrEmpty()) {
            Log.d("MainActivity", "Loading Link in WebView from onCreate: $link")
            webView.loadUrl(link)
        } else {
            Log.d("MainActivity", "No link found, loading default URL")
            webView.loadUrl(myURL)
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)


        // SwipeRefreshLayout 새로고침 리스너 설정
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()  // WebView 새로고침
            swipeRefreshLayout.isRefreshing = false  // 새로고침 애니메이션 종료
        }

        // WebChromeClient를 설정 (필요 시)
        webView.webViewClient = MyWebViewClient()  // 페이지 로딩 및 URL 관련 이벤트 처리
        webView.webChromeClient = MyWebChromeClient()  // 자바스크립트 대화상자 및 브라우저 기능 처리
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "알림 권한이 허용되었습니다.")
            } else {
                Log.d("Permission", "알림 권한이 거부되었습니다.")
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상인지 확인
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                Log.d("Permission", "알림 권한이 이미 허용되었습니다.")
            }
        } else {
            Log.d("Permission", "알림 권한 요청이 필요하지 않은 Android 버전입니다.")
        }
    }



    // WebView 타이머 재개
    override fun onResume() {
        super.onResume()
        webView.resumeTimers()
    }

    // WebView 타이머 일시정지
    override fun onPause() {
        super.onPause()
        webView.pauseTimers()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)  // 새로운 인텐트를 설정

        // 기존 URL과 새로운 Intent의 URL이 다른 경우에만 처리
        val newLink = intent?.getStringExtra("link")
        if (newLink != null && newLink != myURL) {
            Log.d("MainActivity", "New Intent Received with new link: $newLink")
            handleIntent(intent)
        } else {
            Log.d("MainActivity", "동일한 링크가 전달되었거나 링크가 없음")
        }
    }


    private fun handleIntent(intent: Intent?) {
        val link = intent?.getStringExtra("link") ?: myURL
        if (!link.isNullOrEmpty()) {
            Log.d("MainActivity", "Received link: $link")
            defalutSetting();
        }
    }

    // WebViewClient 클래스 정의
    inner class MyWebViewClient : WebViewClient() {

        // 상수 정의
        val INTENT_URI_START = "intent:"
        val INTENT_FALLBACK_URL = "browser_fallback_url"
        val URI_SCHEME_MARKET = "market://details?id="

        // URL 로딩 처리
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url != null && (url.lowercase().startsWith(INTENT_URI_START) || !url.lowercase().startsWith("http"))) {
                var parsedIntent: Intent? = null
                try {
                    parsedIntent = Intent.parseUri(url, 0)
                    view?.context?.let { startActivity(it, parsedIntent, null) }
                } catch (e: ActivityNotFoundException) {
                    return doFallback(view, parsedIntent)
                } catch (e: URISyntaxException) {
                    return doFallback(view, parsedIntent)
                }
            } else {
                view?.loadUrl(url ?: "")
            }
            return true
        }

        // URL이 로드되지 않을 경우 Fallback 처리
        private fun doFallback(view: WebView?, parsedIntent: Intent?): Boolean {
            if (parsedIntent == null) return false
            val fallbackUrl = parsedIntent.getStringExtra(INTENT_FALLBACK_URL)
            if (fallbackUrl != null) {
                view?.loadUrl(fallbackUrl)
                return true
            }
            val packageName = parsedIntent.`package`
            if (packageName != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(URI_SCHEME_MARKET + packageName))
                view?.context?.let { startActivity(it, intent, null) }
                return true
            }
            return false
        }

        // 페이지 로딩 시작 시 처리
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            myURL = url ?: ""

            // ProgressDialog 표시
            progressDialog.setTitle("")
            progressDialog.setMessage("Loading")
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog.setCancelable(true)
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()
        }

        // 페이지 로딩 종료 시 처리
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressDialog.dismiss()

            // WebView에서 JavaScript 함수 호출
            view?.evaluateJavascript("menuClose();", null)

            myURL = url ?: ""

            // 페이지가 로드된 후 FCM 토큰 전달
            fcmToken?.let { token ->
                view?.evaluateJavascript("javascript:setFcmToken('$token')", null)
            }

            if (::childView.isInitialized) {  // childView 초기화 여부 체크
                if (!childURL.equals("") || !childURL.equals(null) || !childURL.isEmpty()) {
                    childURL = ""
                    webView.removeView(childView)
                }
            } else {
                Log.w("MainActivity", "childView has not been initialized")
            }
        }
    }

    // WebChromeClient 클래스 정의
    inner class MyWebChromeClient : WebChromeClient() {

        var count = 1
        private lateinit var childView: WebView

        // 새 창 열기 처리
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            count = 1
            webView.removeAllViews()

            // 새로운 WebView 생성 및 설정
            childView = WebView(this@MainActivity)
            childView.settings.javaScriptEnabled = true
            childView.webChromeClient = this

            // WebViewClient 설정
            childView.webViewClient = object : WebViewClient() {

                // 페이지 로딩 시작 시 처리
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    childURL = url ?: ""

                    if (count == 1) {
                        count = 0
                        if (childURL.contains("칠브라우저로 띄우실 url")) {
                            webView.removeView(childView)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(childURL))
                            startActivity(intent)
                            childURL = ""
                        }
                    }
                }

                // 페이지 로딩 종료 시 처리
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    count = 1
                }
            }

            // childView 레이아웃 설정
            childView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // WebViewTransport를 통해 childView를 설정하고 창을 열기
            webView.addView(childView)
            val transport = resultMsg?.obj as? WebView.WebViewTransport
            transport?.webView = childView
            resultMsg?.sendToTarget()
            return true
        }

        // 창 닫기 처리
        override fun onCloseWindow(window: WebView?) {
            super.onCloseWindow(window)
        }

        // 자바스크립트 Alert 처리
        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            myURL = url ?: ""

            // AlertDialog 생성 및 설정
            val finalRes: JsResult? = result
            AlertDialog.Builder(view?.context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setMessage(message)
                .setPositiveButton("확인") { _, _ ->
                    finalRes?.confirm()
                }
                .setCancelable(false)
                .create()
                .show()
            return true
        }

        // 자바스크립트 Prompt 처리
        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            myURL = url ?: ""

            // AlertDialog 생성
            val finalRes: JsPromptResult? = result
            AlertDialog.Builder(view?.context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setMessage(message)
                .setPositiveButton("확인") { _, _ ->
                    finalRes?.confirm()
                }
                .setCancelable(false)
                .create()
                .show()
            return true
        }

        // 자바스크립트 Confirm 처리
        override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            myURL = url ?: ""

            // AlertDialog 생성
            val finalRes: JsResult? = result
            AlertDialog.Builder(view?.context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setMessage(message)
                .setPositiveButton("확인") { _, _ ->
                    finalRes?.confirm()
                }
                .setNegativeButton("취소") { _, _ ->
                    finalRes?.cancel()
                }
                .setCancelable(false)
                .create()
                .show()
            return true
        }
    }

    // 뒤로 가기 버튼 처리
    override fun onBackPressed() {
        if (myURL == "첫시작 url" && (childURL == "" || childURL == null || childURL.isEmpty())) {
            if (System.currentTimeMillis() - lastTimeBackPressed < 2000) {
                finish()
                return
            } else {
                Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
                lastTimeBackPressed = System.currentTimeMillis()
            }
        } else if (webView.canGoBack() && (childURL == "" || childURL == null || childURL.isEmpty())) {
            webView.goBack()
        } else if (childURL != "" || childURL != null || !childURL.isEmpty()) {
            webView.removeView(childView)
            childURL = ""
            webView.reload()
        }
    }
}

package com.example.callscreeningapp

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.* // 코루틴 사용
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import org.jsoup.Jsoup // 웹 크롤링 사용
import kotlin.coroutines.resume

class MyCallScreeningService : CallScreeningService() {

    // 비동기 작업(검색)을 위한 스코프
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val db = Firebase.firestore // Firestore 초기화

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. 전화번호 추출
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        Log.d("SpamApp", "Incoming call from: $phoneNumber")

        // 2. [자동 차단] 전화가 오자마자 DB 확인 (비동기)
        serviceScope.launch {
            val isBlocked = checkIsBlocked(phoneNumber)

            if (isBlocked) {
                // 이미 차단/신고된 번호라면? -> 팝업 없이 즉시 거절 (Auto Block)
                Log.d("SpamApp", "자동 차단된 번호입니다: $phoneNumber")
                Toast.makeText(applicationContext, "스팸 번호 자동 차단됨: $phoneNumber", Toast.LENGTH_LONG).show()
                blockCallImmediately(callDetails)
                // 토스트는 안 뜰 수도 있음 (백그라운드 제약)
            } else {
                // 차단되지 않은 번호라면 -> 팝업 표시
                if (Settings.canDrawOverlays(this@MyCallScreeningService)) {
                    showOverlay(callDetails, phoneNumber)
                }
            }
        }
    }

    // DB에서 이 번호가 차단 목록(spam_numbers)에 있는지 확인
    private suspend fun checkIsBlocked(number: String): Boolean = suspendCancellableCoroutine { continuation ->
        db.collection("spam_numbers").document(number).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // 문서가 존재하면 차단된 것으로 간주
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
            .addOnFailureListener {
                // 에러 나면 차단 안 함(안전하게 팝업 띄움)
                continuation.resume(false)
            }
    }

    // *실제 자동 차단 로직은 아래 showOverlay 안에서 버튼을 눌렀을 때 저장된 데이터로 작동합니다.*
    // *이번 단계에서는 '버튼이 안 먹히는 문제' 해결에 집중하겠습니다.*

    @SuppressLint("MissingPermission") // ANSWER_PHONE_CALLS 권한 체크 억제 (MainActivity에서 받았다고 가정)
    private fun showOverlay(callDetails: Call.Details, phoneNumber: String) {
        // 1. 윈도우 매니저 불러오기 (화면을 관리하는 시스템 서비스)
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 2. 팝업창의 속성 설정 (위치, 크기, 타입 등)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, // 너비: 화면 꽉 차게
            WindowManager.LayoutParams.WRAP_CONTENT, // 높이: 내용물만큼만
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // 키보드 입력은 받되(Focusable), 팝업 바깥 터치는 시스템(전화앱)으로 넘김(Not Touch Modal)
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or // 팝업 뒤의 전화 받기 버튼도 눌려야 함
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or // 잠금 화면 위에도 뜨게
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or // 화면이 꺼져있으면 켜게
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT // 배경 투명 처리
        )

        // 위치 설정: 화면 중앙
        params.gravity = Gravity.CENTER

        // 3. XML 디자인을 실제 뷰(View)로 만들기 (Inflate)
        val view = LayoutInflater.from(this).inflate(R.layout.item_call_popup, null)

        // 4. 데이터 연결하기 (전화번호 텍스트 넣기)
        val layoutMain = view.findViewById<android.widget.LinearLayout>(R.id.layout_main)
        val layoutGuide = view.findViewById<android.widget.LinearLayout>(R.id.layout_guide)
        val btnGuideGo = view.findViewById<Button>(R.id.btn_guide_go)
        val btnGuideClose = view.findViewById<Button>(R.id.btn_guide_close)

        val tvPhoneNumber = view.findViewById<TextView>(R.id.tv_phone_number)
        val tvInfo = view.findViewById<TextView>(R.id.tv_spam_info) // 검색 결과 띄울 곳
        val etReason = view.findViewById<EditText>(R.id.et_spam_reason) // 사유 입력칸 찾기
        tvPhoneNumber.text = phoneNumber // 걸려온 번호 표시

        // 5. 실시간 웹 검색 시작 (비동기)
        serviceScope.launch {
            // [자동 차단 기능] 팝업이 떴지만, DB에 차단된 기록이 있으면 바로 닫고 끊어버림
            val doc = db.collection("spam_numbers").document(phoneNumber).get().await() // await() 확장함수 필요할 수 있음

            // *await()가 없으면 그냥 검색 로직만 수행
            val searchResult = searchPhoneNumberInfo(phoneNumber) // 아래에 만든 함수 호출
            tvInfo.text = searchResult // 검색 결과로 텍스트 변경

            // 검색 결과가 '광고'나 '스팸'을 포함하면 빨간색으로 강조
            if (searchResult.contains("광고") || searchResult.contains("스팸")) {
                tvInfo.setTextColor(android.graphics.Color.RED)
            }
        }

        // 6-1. [무시] 버튼: 팝업만 닫고 전화는 계속 울리게 둠
        val btnIgnore = view.findViewById<Button>(R.id.btn_popup_ignore)
        btnIgnore.setOnClickListener {
            val responseBuilder = CallResponse.Builder()

            // 안드로이드 10(API 29) 이상에서만 지원하는 '무음 처리' 기능
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                responseBuilder.setSilenceCall(true) // 벨소리만 뚝 그침
            }

            // 전화는 끊지 않고(Allow) 그냥 둠
            respondToCall(callDetails, CallResponse.Builder().build())

            // 팝업을 닫음 -> 이제 사용자는 홈 버튼을 누르거나 다른 앱을 쓸 수 있음
            windowManager.removeView(view)
            Toast.makeText(applicationContext, "벨소리를 껐습니다.", Toast.LENGTH_SHORT).show()
        }

        // 6-2. [신고만 하기] 버튼 (차단 X, DB 저장 O)
        val btnReport = view.findViewById<Button>(R.id.btn_popup_report)
        btnReport.setOnClickListener {
            val reason = etReason.text.toString() // 입력한 사유 가져오기
            reportSpam(phoneNumber, reason)       // DB 저장 함수 호출

            // 전화는 계속 울리게 둠 (Allow) + 팝업 닫기
            respondToCall(callDetails, CallResponse.Builder().build())
            windowManager.removeView(view)
            Toast.makeText(applicationContext, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 6-3. [거절] 버튼: 전화만 딱 끊음 (기록은 남음)
        val btnReject = view.findViewById<Button>(R.id.btn_popup_reject)
        btnReject.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                tm.endCall()
            }
            Toast.makeText(applicationContext, "전화를 거절했습니다.", Toast.LENGTH_SHORT).show()
        }

        // 6-4. [차단 및 신고] 버튼: 끊고 + 기록 삭제 + (내부적으로 차단 처리) (차단 O, DB 저장 O)
        val btnBlock = view.findViewById<Button>(R.id.btn_popup_block)
        btnBlock.setOnClickListener {
            val reason = etReason.text.toString()
            reportSpam(phoneNumber, reason) // DB 저장

            // 전화 끊기 (거절)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                try { tm.endCall() } catch (e: Exception) {}
            } else {
                val response = CallResponse.Builder().setDisallowCall(true).setRejectCall(true).build()
                respondToCall(callDetails, response)
            }

            // 스팸 번호 복사
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Blocked Number", phoneNumber)
            clipboard.setPrimaryClip(clip)

            // 바로 이동하지 않고 안내 화면 보여주기!
            layoutMain.visibility = android.view.View.GONE
            layoutGuide.visibility = android.view.View.VISIBLE
        }

        // 안내 화면의 '설정으로 이동하기' 버튼
        btnGuideGo.setOnClickListener {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    val intent = tm.createManageBlockedNumbersIntent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("SpamApp", "설정 이동 실패: ${e.message}")
            }
            windowManager.removeView(view) // 이동하면서 팝업 닫기
        }

        // 안내 화면의 '닫기' 버튼 (설정 이동 안 함)
        btnGuideClose.setOnClickListener {
            windowManager.removeView(view)
        }

        // 7. 최종적으로 화면에 추가!
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            Log.e("SpamApp", "Error adding view: ${e.message}")
            // 에러가 나면 전화를 막지 않도록 허용 처리
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }

    private fun blockCallImmediately(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(true)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)
    }

    // Firebase Firestore에 스팸 정보 저장하는 함수
    private fun reportSpam(number: String, reason: String) {
        val spamRef = db.collection("spam_numbers").document(number)

        // 사유가 비어있으면 기본값
        val finalReason = if (reason.isBlank()) "사유 없음" else reason

        spamRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // 이미 신고된 번호라면? -> 신고 횟수(count) + 1, 사유 목록에 추가
                spamRef.update(
                    "spam_count", FieldValue.increment(1),
                    "reasons", FieldValue.arrayUnion(finalReason),
                    "last_reported", System.currentTimeMillis()
                )
            } else {
                // 처음 신고된 번호라면? -> 새로 생성
                val data = hashMapOf(
                    "number" to number,
                    "spam_count" to 1,
                    "reasons" to arrayListOf(finalReason),
                    "last_reported" to System.currentTimeMillis()
                )
                spamRef.set(data)
            }
        }.addOnFailureListener { e ->
            Log.e("SpamApp", "Error writing document", e)
        }
    }

    private suspend fun searchPhoneNumberInfo(number: String): String = withContext(Dispatchers.IO) {
        try {
            // Firestore에서 먼저 조회해볼 수도 있음 (나중에 구현)
            val url = "https://www.google.com/search?q=$number+스팸"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(5000).get()
            val elements = doc.select("div").filter { it.text().contains("스팸") || it.text().contains("광고") }
            if (elements.isNotEmpty()) "검색 결과: " + elements.first().text().take(30) + "..." else "검색 결과 없음"
        } catch (e: Exception) {
            "정보 없음"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 서비스 종료 시 작업 취소
    }
}
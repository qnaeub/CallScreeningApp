package com.example.callscreeningapp

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.* // 코루틴 사용
import kotlinx.coroutines.tasks.await

class MyCallScreeningService : CallScreeningService() {

    // 비동기 작업(검색)을 위한 스코프
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val db = Firebase.firestore // Firestore 초기화

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. 전화번호 추출
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        Log.d("SpamApp", "Incoming call from: $phoneNumber")

        // 2. 전화가 오자마자 DB 확인
        serviceScope.launch {
            val spamInfo = getSpamInfoFromFirestore(phoneNumber)

            if (Settings.canDrawOverlays(this@MyCallScreeningService)) {
                showOverlay(callDetails, phoneNumber, spamInfo)
            }
        }
    }

    // Firestore에서 신고 내역 가져오기
    private suspend fun getSpamInfoFromFirestore(number: String): String = withContext(Dispatchers.IO) {
        try {
            val document = db.collection("spam_numbers").document(number).get().await()
            if (document.exists()) {
                val count = document.getLong("spam_count") ?: 0
                val reasons = document.get("reasons") as? List<String> ?: emptyList()

                // 사유 중복 제거 및 최신 3개만 보여주기 (예: "대출, 도박")
                val reasonText = reasons.distinct().take(3).joinToString(", ")

                return@withContext "🚨 신고 ${count}건 ($reasonText)"
            } else {
                return@withContext "✅ 신고된 이력이 없는 번호입니다."
            }
        } catch (e: Exception) {
            return@withContext "정보를 가져올 수 없습니다."
        }
    }

    @SuppressLint("MissingPermission") // ANSWER_PHONE_CALLS 권한 체크 억제 (MainActivity에서 받았다고 가정)
    private fun showOverlay(callDetails: Call.Details, phoneNumber: String, spamInfo: String) {
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

        // 4. View 찾기
        val layoutMain = view.findViewById<android.widget.LinearLayout>(R.id.layout_main)
        val layoutGuide = view.findViewById<android.widget.LinearLayout>(R.id.layout_guide)

        val tvPhoneNumber = view.findViewById<TextView>(R.id.tv_phone_number)
        val tvInfo = view.findViewById<TextView>(R.id.tv_spam_info) // 검색 결과 띄울 곳
        val etReason = view.findViewById<EditText>(R.id.et_spam_reason) // 사유 입력칸 찾기

        // 버튼들
        val btnIgnore = view.findViewById<Button>(R.id.btn_popup_ignore)
        val btnReport = view.findViewById<Button>(R.id.btn_popup_report)
        val btnReject = view.findViewById<Button>(R.id.btn_popup_reject)
        val btnBlock = view.findViewById<Button>(R.id.btn_popup_block)

        // 가이드 화면 버튼들
        val btnGuideGo = view.findViewById<Button>(R.id.btn_guide_go)
        val btnGuideClose = view.findViewById<Button>(R.id.btn_guide_close)

        tvPhoneNumber.text = phoneNumber // 걸려온 번호 표시

        // 5. DB에서 가져온 정보 표시
        tvInfo.text = spamInfo
        if (spamInfo.contains("신고")) {
            tvInfo.setTextColor(android.graphics.Color.RED) // 신고 내역 있으면 빨간색
        } else {
            tvInfo.setTextColor(android.graphics.Color.parseColor("#388E3C")) // 없으면 초록색
        }

        // 6. 버튼 리스너 연결
        // 6-1. [무시] 버튼: 팝업만 닫고 전화는 계속 울리게 둠
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
        btnReport.setOnClickListener {
            val reason = etReason.text.toString() // 입력한 사유 가져오기
            reportSpam(phoneNumber, reason)       // DB 저장 함수 호출

            // 전화는 계속 울리게 둠 (Allow) + 팝업 닫기
            respondToCall(callDetails, CallResponse.Builder().build())
            windowManager.removeView(view)
            Toast.makeText(applicationContext, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 6-3. [거절] 버튼: 전화만 딱 끊음 (기록은 남음)
        btnReject.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                try { tm.endCall() } catch (e: Exception) {}
            }
            windowManager.removeView(view)
            Toast.makeText(applicationContext, "전화를 거절했습니다.", Toast.LENGTH_SHORT).show()
        }

        // 6-4. [차단 및 신고] 버튼: 끊고 + 기록 삭제 + (내부적으로 차단 처리) (차단 O, DB 저장 O)
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

            // 안내 화면(Guide)으로 전환
            layoutMain.visibility = android.view.View.GONE
            layoutGuide.visibility = android.view.View.VISIBLE
        }

        // 6-4-1. 안내 화면의 '설정으로 이동하기' 버튼
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

        // 6-4-2. 안내 화면의 '닫기' 버튼 (설정 이동 안 함)
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 서비스 종료 시 작업 취소
    }
}
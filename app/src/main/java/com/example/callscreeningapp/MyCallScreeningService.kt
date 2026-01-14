package com.example.callscreeningapp

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.* // 코루틴 사용
import org.jsoup.Jsoup // 웹 크롤링 사용

class MyCallScreeningService : CallScreeningService() {

    // 비동기 작업(검색)을 위한 스코프
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. 전화번호 추출
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        Log.d("SpamApp", "Incoming call from: $phoneNumber")

        // 2. 오버레이 권한 확인
        if (Settings.canDrawOverlays(this)) {
            // 권한이 있으면 팝업을 띄우고, 사용자가 버튼을 누를 때까지 응답(respondToCall)을 보류합니다.
            // callDetails 객체를 showOverlay로 넘겨줍니다.
            showOverlay(callDetails, phoneNumber)
        } else {
            // 권한이 없으면 그냥 평소처럼 전화가 울리게 허용(Pass)해야 합니다.
            val response = CallResponse.Builder().build()
            respondToCall(callDetails, response)
        }
    }

    private fun showOverlay(callDetails: Call.Details, phoneNumber: String) {
        // 1. 윈도우 매니저 불러오기 (화면을 관리하는 시스템 서비스)
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 2. 팝업창의 속성 설정 (위치, 크기, 타입 등)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, // 너비: 화면 꽉 차게
            WindowManager.LayoutParams.WRAP_CONTENT, // 높이: 내용물만큼만
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // 타입: 앱 위에 둥둥 뜨는 타입
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // 중요: 팝업 뒤의 전화 받기 버튼도 눌려야 함
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
        val tvPhoneNumber = view.findViewById<TextView>(R.id.tv_phone_number)
        val tvInfo = view.findViewById<TextView>(R.id.tv_spam_info) // 검색 결과 띄울 곳
        tvPhoneNumber.text = phoneNumber // 걸려온 번호 표시

        // 5. 실시간 웹 검색 시작 (비동기)
        serviceScope.launch {
            val searchResult = searchPhoneNumberInfo(phoneNumber) // 아래에 만든 함수 호출
            tvInfo.text = searchResult // 검색 결과로 텍스트 변경
        }

        // 6-1. [무시] 버튼: 팝업만 닫고 전화는 계속 울리게 둠
        val btnIgnore = view.findViewById<Button>(R.id.btn_popup_ignore)
        btnIgnore.setOnClickListener {
            // 아무 응답도 안 보내면(allow 기본), 전화는 계속 울림
            respondToCall(callDetails, CallResponse.Builder().build())
            windowManager.removeView(view)
        }

        // 6-2. [거절] 버튼: 전화만 딱 끊음 (기록은 남음)
        val btnReject = view.findViewById<Button>(R.id.btn_popup_reject)
        btnReject.setOnClickListener {
            val response = CallResponse.Builder()
                .setDisallowCall(true) // 연결 불허
                .setRejectCall(true)   // 거절 신호 보냄
                .build()
            respondToCall(callDetails, response)
            windowManager.removeView(view)
            Toast.makeText(applicationContext, "전화를 거절했습니다.", Toast.LENGTH_SHORT).show()
        }

        // 6-3. [차단] 버튼: 끊고 + 기록 삭제 + (내부적으로 차단 처리)
        val btnBlock = view.findViewById<Button>(R.id.btn_popup_block)
        btnBlock.setOnClickListener {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)     // 통화 기록에서 삭제
                .setSkipNotification(true) // 알림도 삭제
                .build()
            respondToCall(callDetails, response)
            windowManager.removeView(view)
            Toast.makeText(applicationContext, "차단했습니다.", Toast.LENGTH_SHORT).show()
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

    // 🌐 실제 구글 검색을 시뮬레이션하는 함수
    private suspend fun searchPhoneNumberInfo(number: String): String = withContext(Dispatchers.IO) {
        try {
            // [실제 구현 방법]
            // 구글에 전화번호를 검색해서 제목을 긁어옵니다. (User-Agent 설정 필수)
            // 주의: 너무 많이 요청하면 구글이 차단할 수 있으므로, 실제 앱에선 '더치트'나 스팸 API를 쓰는 게 좋습니다.
            val url = "https://www.google.com/search?q=$number"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            // 검색 결과 중 첫 번째 제목 등을 가져옴 (구조는 구글 마음에 따라 바뀔 수 있음)
            val title = doc.select("h3").firstOrNull()?.text()

            if (title != null) {
                return@withContext "검색 결과: $title"
            } else {
                return@withContext "검색 결과가 없습니다."
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // 에러 나면 테스트용 가짜 데이터 리턴 (테스트 할 때 편하시라고)
            return@withContext "스팸 신고가 많은 번호입니다 (대출 권유)"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 서비스 종료 시 작업 취소
    }
}
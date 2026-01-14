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

class MyCallScreeningService : CallScreeningService() {

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

        // 3. 일단은 전화를 허용함 (전화 앱이 울리게 둠)
        val response = CallResponse.Builder().build()
        respondToCall(callDetails, response)
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
        tvPhoneNumber.text = phoneNumber // 걸려온 번호 표시

        // 텍스트뷰 ID를 찾아 스팸 여부 표시 (여기서는 예시로 무조건 표시하지만, 나중에 로직 추가 가능)
        val tvTitle = view.findViewById<TextView>(R.id.tv_popup_title)
        tvTitle.text = "실시간 수신 전화"

        // 5-1. 차단 버튼 (Reject)
        val btnBlock = view.findViewById<Button>(R.id.btn_popup_block)
        btnBlock.setOnClickListener {
            // 1. 시스템에 "이 전화 거절해(Disallow)"라고 명령
            val response = CallResponse.Builder()
                .setDisallowCall(true)      // 전화 연결 불허
                .setRejectCall(true)        // 상대방에게 '통화 거절' 신호 보냄 (전화 끊김)
                .setSkipCallLog(false)      // 통화 기록에는 남김 (true면 기록도 안 남음)
                .setSkipNotification(true)  // 부재중 알림 안 띄움
                .build()

            respondToCall(callDetails, response)

            // 2. 팝업 닫기
            windowManager.removeView(view)
            Toast.makeText(applicationContext, "전화를 차단했습니다.", Toast.LENGTH_SHORT).show()
        }

        // 5-2. 통화(수락) 버튼 (Allow)
        val btnCall = view.findViewById<Button>(R.id.btn_popup_call)
        btnCall.text = "통화 허용" // 버튼 텍스트를 '통화' -> '통화 허용'으로 개념 변경
        btnCall.setOnClickListener {
            // 1. 시스템에 "이 전화 연결해(Allow)"라고 명령
            // (안드로이드 정책상 앱이 직접 '전화 받기'를 수행할 수는 없고, '울리도록 허용'만 가능합니다)
            val response = CallResponse.Builder().build() // 기본값이 Allow
            respondToCall(callDetails, response)

            // 2. 팝업 닫기 (이제 기본 전화 앱 화면이 보일 것입니다)
            windowManager.removeView(view)
        }

        // 6. 최종적으로 화면에 추가!
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            Log.e("SpamApp", "Error adding view: ${e.message}")
            // 에러가 나면 전화를 막지 않도록 허용 처리
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }
}
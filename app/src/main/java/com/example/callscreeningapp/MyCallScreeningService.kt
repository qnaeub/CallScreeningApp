package com.example.callscreeningapp

import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class MyCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. 전화번호 추출 (없으면 "알 수 없음" 표시)
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        Log.d("SpamApp", "Incoming call from: $phoneNumber")

        // 2. 오버레이 팝업 표시 (권한이 있을 때만)
        if (Settings.canDrawOverlays(this)) {
            showOverlay(phoneNumber)
        } else {
            Log.d("SpamApp", "Overlay permission not granted.")
        }

        // 3. 일단은 전화를 허용함 (전화 앱이 울리게 둠)
        val response = CallResponse.Builder().build()
        respondToCall(callDetails, response)
    }

    private fun showOverlay(phoneNumber: String) {
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

        // 3. 아까 만든 XML 디자인을 실제 뷰(View)로 만들기 (Inflate)
        val view = LayoutInflater.from(this).inflate(R.layout.item_call_popup, null)

        // 4. 데이터 연결하기 (전화번호 텍스트 넣기)
        val tvPhoneNumber = view.findViewById<TextView>(R.id.tv_phone_number)
        tvPhoneNumber.text = phoneNumber // 걸려온 번호 표시

        // 5. 닫기 버튼 누르면 팝업 사라지게 하기
        val btnClose = view.findViewById<Button>(R.id.btn_close)
        btnClose.setOnClickListener {
            windowManager.removeView(view) // 화면에서 제거
        }

        // 6. 최종적으로 화면에 추가!
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            Log.e("SpamApp", "Error adding view: ${e.message}")
        }
    }
}
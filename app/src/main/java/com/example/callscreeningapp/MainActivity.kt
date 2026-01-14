package com.example.callscreeningapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 오버레이 권한 확인 및 요청 코드
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                // 권한이 없으면 설정 화면으로 이동시킴
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 1234)
                android.widget.Toast.makeText(this, "팝업을 띄우려면 '다른 앱 위에 표시' 권한을 허용해주세요.", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        // 기본 스팸 앱 설정 요청 코드
        val roleManager = getSystemService(ROLE_SERVICE) as android.app.role.RoleManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                    startActivityForResult(intent, 1234)
                }
            }
        }

        // 전화 끊기 권한(ANSWER_PHONE_CALLS) 요청
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ANSWER_PHONE_CALLS), 101)
            }
        }

        // 1. RecyclerView 찾아오기
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // 2. 가짜 데이터(Mock Data) 만들기 - 테스트용
        val dummyList = mutableListOf(
            CallLogItem("010-9876-5432", "스팸 의심 (대출)", "방금 전", true),
            CallLogItem("02-1234-5678", "안전 (택배)", "1시간 전", false),
            CallLogItem("070-1111-2222", "스팸 의심 (보이스피싱)", "어제", true),
            CallLogItem("031-444-5555", "안전", "2일 전", false),
            CallLogItem("010-1111-2222", "스팸 의심", "3일 전", true)
        )

        // 3. 어댑터 연결하기
        val adapter = CallLogAdapter(dummyList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
}
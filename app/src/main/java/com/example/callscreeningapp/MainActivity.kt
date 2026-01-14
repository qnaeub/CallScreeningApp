package com.example.callscreeningapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val db = Firebase.firestore // DB 인스턴스

    // 클래스 멤버 변수로 선언 (다른 함수에서도 쓰기 위해)
    private val callLogList = mutableListOf<CallLogItem>()
    private lateinit var adapter: CallLogAdapter

    // 데이터베이스 변경을 감지하는 '감시자' 정의
    private val callLogObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            // DB가 변하면 자동으로 이 함수가 실행됩니다.
            // 여기서 데이터를 다시 불러옵니다!
            loadRealCallLogs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // RecyclerView & Adapter 설정
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = CallLogAdapter(callLogList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 권한 체크 후 데이터 불러오기
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 있으면 바로 불러오기
            loadRealCallLogs()
        } else {
            // 권한이 없으면 사용자에게 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALL_LOG), 100
            )
        }
    }

    // 실제 통화 기록 불러오는 함수
    private fun loadRealCallLogs() {
        callLogList.clear() // 기존 데이터 초기화

        // 1. 가져올 컬럼 정의 (전화번호, 날짜)
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE
        )

        // 2. ContentResolver로 쿼리 실행 (최신순 정렬)
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        // 3. 커서(Cursor)를 통해 데이터 한 줄씩 읽기
        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                val dateLong = it.getLong(dateIndex)

                // 날짜 변환 (예: 2024-05-20 14:00)
                val dateString = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(dateLong))

                // 4. 스팸 여부 판단 (여러분이 만든 DB나 로직을 여기에 연결!)
                // 지금은 예시로 '070'으로 시작하면 스팸으로 간주합니다.
                val spamInfo = checkSpamDatabase(number)

                // 5. 리스트에 추가
                callLogList.add(
                    CallLogItem(
                        phoneNumber = number,
                        date = dateString,
                        isSpam = spamInfo.isSpam, // 빨간색 표시 여부
                        spamInfo = spamInfo.spamInfo, // "스팸 의심" 등
                    )
                )
            }
        }

        // 6. 어댑터에 데이터 변경 알림 (화면 갱신)
        adapter.notifyDataSetChanged()
    }

    // 스팸인지 확인하는 함수 (나중에 DB 연동 부분)
    data class SpamCheckResult(val spamInfo: String, val isSpam: Boolean)

    private fun checkSpamDatabase(number: String): SpamCheckResult {
        // TODO: 나중에 실제 SQLite DB나 서버 API를 통해 확인하는 로직을 넣으세요.

        return if (number.startsWith("070")) {
            SpamCheckResult("스팸 의심 (광고)", true)
        } else if (number.startsWith("02")) {
            SpamCheckResult("일반 전화", false)
        } else {
            SpamCheckResult("휴대전화", false)
        }
    }

    // (권한 요청 결과 처리)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadRealCallLogs() // 권한 허용되면 로드 시작
        } else {
            Toast.makeText(this, "통화 기록 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // 1. 앱이 다시 활성화될 때 리스트 한 번 갱신 (전화하고 돌아왔을 때 즉시 반영)
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            loadRealCallLogs()

            // 2. 감시자 등록 (이제부터 DB를 지켜보고 있어라!)
            contentResolver.registerContentObserver(
                android.provider.CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // 앱이 백그라운드로 가면 감시 해제
        contentResolver.unregisterContentObserver(callLogObserver)
    }
}
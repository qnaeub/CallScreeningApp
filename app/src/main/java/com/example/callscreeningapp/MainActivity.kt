package com.example.callscreeningapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
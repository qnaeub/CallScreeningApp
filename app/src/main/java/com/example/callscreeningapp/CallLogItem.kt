package com.example.callscreeningapp

data class CallLogItem(
    val phoneNumber: String,  // 전화번호 (예: 010-1234-5678)
    val spamTag: String,      // 태그 내용 (예: 스팸 의심, 안전)
    val date: String,         // 날짜
    val isSpam: Boolean       // 스팸 여부 (이걸로 글자 색깔 바꿀 예정)
)
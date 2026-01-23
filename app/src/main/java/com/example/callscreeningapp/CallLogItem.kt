package com.example.callscreeningapp

// 번호의 상태를 정의하는 열거형 (Enum) 클래스
enum class CallType {
    NORMAL, // 일반
    SPAM,   // 스팸 (Firestore DB에 있음)
    BLOCKED // 시스템 차단 (연락처 앱에서 차단됨)
}

data class CallLogItem(
    val phoneNumber: String,  // 전화번호
    val date: String,         // 날짜
    val type: CallType,
    val spamInfo: String? = null,   // 태그 내용
    val count: Int = 1              //중복된 횟수 저장
)
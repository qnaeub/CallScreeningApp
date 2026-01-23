package com.example.callscreeningapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.recyclerview.widget.RecyclerView

// List -> MutableList로 변경
class CallLogAdapter(
    private val items: MutableList<CallLogItem>,
    private val onDeleteClicked: (String) -> Unit
) : RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    inner class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPhoneNumber: TextView = itemView.findViewById(R.id.tv_phone_number)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        // 태그 뷰 찾기
        val tvSpamTag: TextView = itemView.findViewById(R.id.tv_spam_tag)

        fun bind(item: CallLogItem) {
            // 건수가 1보다 크면 괄호로 표시 (예: 010-1234-5678 (3))
            if (item.count > 1) {
                tvPhoneNumber.text = "${item.phoneNumber} (${item.count})"
            } else {
                tvPhoneNumber.text = item.phoneNumber
            }
            tvDate.text = item.date
            tvSpamTag.text = item.spamInfo

            // 뷰가 재사용되므로, 일단 취소선 효과를 초기화(제거)하고 시작
            tvPhoneNumber.paintFlags = tvPhoneNumber.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

            // Enum 상태(CallType)에 따라 디자인 분기
            when (item.type) {
                CallType.SPAM -> {
                    // [스팸] 빨간색
                    tvPhoneNumber.setTextColor(Color.parseColor("#E53935")) // Red
                    tvSpamTag.setTextColor(Color.parseColor("#E53935"))
                    tvSpamTag.background?.setTint(Color.parseColor("#FFEBEE")) // Light Red
                    tvSpamTag.visibility = View.VISIBLE
                }
                CallType.BLOCKED -> {
                    // [차단됨] 연한 회색 + 취소선
                    tvPhoneNumber.setTextColor(Color.parseColor("#9E9E9E")) // 더 연한 회색

                    // 글자에 취소선 긋기 (가운데 줄)
                    tvPhoneNumber.paintFlags = tvPhoneNumber.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

                    tvSpamTag.setTextColor(Color.parseColor("#757575"))
                    tvSpamTag.background?.setTint(Color.parseColor("#F5F5F5")) // 아주 연한 회색 배경
                    tvSpamTag.visibility = View.VISIBLE
                }
                CallType.NORMAL -> {
                    // [일반] 검은색
                    tvPhoneNumber.setTextColor(Color.parseColor("#212121")) // 진한 검정
                    tvSpamTag.setTextColor(Color.parseColor("#2E7D32"))
                    tvSpamTag.background?.setTint(Color.parseColor("#E8F5E9"))

                    // 일반 번호 태그 숨김
                    tvSpamTag.visibility = View.GONE
                }
            }

            // 삭제 버튼 클릭 이벤트
            btnDelete.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDeleteClicked(item.phoneNumber)
                    items.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return CallLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        // 항목 클릭 시 커스텀 팝업(Dialog) 띄우기
        holder.itemView.setOnClickListener {
            // 1. 팝업창 디자인(xml) 가져오기
            val dialogView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_call_popup, null)

            // 2. 팝업창 생성 및 설정
            val mBuilder = androidx.appcompat.app.AlertDialog.Builder(holder.itemView.context)
                .setView(dialogView)

            // 3. 팝업창 띄우기 (이때 화면에 나타남)
            val mAlertDialog = mBuilder.show()

            // 4. 팝업창 배경 투명하게 만들기 (CardView의 둥근 모서리를 예쁘게 보이게 하기 위함)
            mAlertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // 5. 팝업창 내부의 요소들 찾기 (findViewById)
            val tvPopupPhone = dialogView.findViewById<TextView>(R.id.tv_phone_number)
            val tvPopupTitle = dialogView.findViewById<TextView>(R.id.tv_popup_title)
            val etReason = dialogView.findViewById<EditText>(R.id.et_spam_reason)   // 입력칸

            val btnIgnore = dialogView.findViewById<Button>(R.id.btn_popup_ignore)
            val btnReport = dialogView.findViewById<Button>(R.id.btn_popup_report)
            val btnReject = dialogView.findViewById<Button>(R.id.btn_popup_reject)  // 이걸 '통화' 버튼으로 쓸 겁니다.
            val btnBlock = dialogView.findViewById<Button>(R.id.btn_popup_block)    // 팝업 요소 찾기

            // 6. 기본 데이터 세팅
            tvPopupPhone.text = item.phoneNumber

            // 7. UI 설정
            btnIgnore.visibility = View.GONE    // '무시' 버튼 숨기기

            etReason.visibility = View.VISIBLE   // 사유 입력칸 보이게 하기
            btnReport.visibility = View.VISIBLE  // '신고만 하기' 버튼 보이게 하기

            // '거절' -> '통화' 버튼으로 변경
            btnReject.text = "통화"
            btnReject.setBackgroundColor(Color.parseColor("#388E3C")) // 초록색

            // 스팸 여부 UI 표시
            if (item.type == CallType.SPAM) {
                tvPopupTitle.text = "🚨 스팸 의심 번호 감지!"
                tvPopupTitle.setTextColor(Color.parseColor("#E53935"))
            } else {
                tvPopupTitle.text = "✅ 안전한 번호입니다"
                tvPopupTitle.setTextColor(Color.parseColor("#388E3C"))
            }

            // 8. [기능 연결]
            // 8-1. [기능 1] 신고만 하기 버튼 (DB 저장 O, 차단 X)
            btnReport.setOnClickListener {
                val inputReason = etReason.text.toString()
                val finalReason = if (inputReason.isBlank()) "통화 기록에서 신고" else inputReason

                // Firestore 저장 로직 호출
                saveSpamToFirestore(item.phoneNumber, finalReason)

                android.widget.Toast.makeText(holder.itemView.context, "신고가 접수되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                mAlertDialog.dismiss()
            }

            // 8-2. [기능 2] 통화 버튼 (전화 앱 연결)
            btnReject.setOnClickListener {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:${item.phoneNumber}")
                }
                holder.itemView.context.startActivity(intent)
                mAlertDialog.dismiss()
            }

            // 8-3. [기능 3] 차단 및 신고 버튼 (DB 저장 O, 차단 메시지)
            btnBlock.setOnClickListener {
                val inputReason = etReason.text.toString()
                val finalReason = if (inputReason.isBlank()) "통화 기록에서 차단" else inputReason

                // Firestore 저장 로직 호출
                saveSpamToFirestore(item.phoneNumber, finalReason)

                android.widget.Toast.makeText(holder.itemView.context, "차단 및 신고 완료", android.widget.Toast.LENGTH_SHORT).show()
                mAlertDialog.dismiss()
            }
        }
    }

    // [보조 함수] Firestore 저장 코드가 중복되므로 함수로 분리했습니다. (클래스 내부에 추가하세요)
    private fun saveSpamToFirestore(phoneNumber: String, reason: String) {
        val db = Firebase.firestore
        val spamRef = db.collection("spam_numbers").document(phoneNumber)

        spamRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                spamRef.update(
                    "spam_count", FieldValue.increment(1),
                    "reasons", FieldValue.arrayUnion(reason),
                    "last_reported", System.currentTimeMillis()
                )
            } else {
                val data = hashMapOf(
                    "number" to phoneNumber,
                    "spam_count" to 1,
                    "reasons" to arrayListOf(reason),
                    "last_reported" to System.currentTimeMillis()
                )
                spamRef.set(data)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
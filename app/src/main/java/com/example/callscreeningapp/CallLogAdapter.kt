package com.example.callscreeningapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.recyclerview.widget.RecyclerView

// List -> MutableList로 변경
class CallLogAdapter(private val items: MutableList<CallLogItem>) :
    RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return CallLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        // 삭제 버튼 클릭 이벤트
        // 여기서 'btnDelete'는 아래 ViewHolder에서 찾아놓은 변수 이름입니다.
        holder.btnDelete.setOnClickListener {
            // 1. 데이터 리스트에서 현재 위치(position)의 아이템 제거
            // 주의: 클릭하는 순간의 정확한 위치를 알기 위해 holder.adapterPosition을 쓰는 게 더 안전합니다.
            val currentPos = holder.bindingAdapterPosition

            if (currentPos != RecyclerView.NO_POSITION) { // 위치가 유효하다면
                items.removeAt(currentPos)           // 데이터 삭제
                notifyItemRemoved(currentPos)        // "이 자리 아이템 사라졌어!" 알림
                notifyItemRangeChanged(currentPos, items.size) // "나머지 순서 다시 매겨!" 알림
            }
        }

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
            if (item.isSpam) {
                tvPopupTitle.text = "스팸 의심 번호 감지!"
                tvPopupTitle.setTextColor(Color.parseColor("#E53935"))
            } else {
                tvPopupTitle.text = "안전한 번호입니다"
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

    class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 기존 뷰들
        private val tvPhoneNumber: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        private val tvSpamTag: TextView = itemView.findViewById(R.id.tvSpamTag)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        // 삭제 버튼(X) 찾아오기
        val btnDelete: View = itemView.findViewById(R.id.ivStatus)

        fun bind(item: CallLogItem) {
            tvPhoneNumber.text = item.phoneNumber
            tvDate.text = item.date
            tvSpamTag.text = item.spamTag

            if (item.isSpam) {
                tvSpamTag.setTextColor(Color.parseColor("#D32F2F"))
                tvSpamTag.setBackgroundResource(R.drawable.bg_tag_spam)
            } else {
                tvSpamTag.setTextColor(Color.parseColor("#388E3C"))
                tvSpamTag.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
}
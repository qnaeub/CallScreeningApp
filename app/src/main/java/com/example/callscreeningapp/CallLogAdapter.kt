package com.example.callscreeningapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
            val btnClose = dialogView.findViewById<Button>(R.id.btn_close)
            val tvPopupTitle = dialogView.findViewById<TextView>(R.id.tv_popup_title)

            // 6. 데이터 넣기 (클릭한 아이템의 전화번호 표시)
            tvPopupPhone.text = item.phoneNumber

            // 스팸 여부에 따라 제목과 색상 바꾸기
            if (item.isSpam) {
                tvPopupTitle.text = "스팸 의심 번호 감지!"
                tvPopupTitle.setTextColor(Color.parseColor("#E53935")) // 빨간색
            } else {
                tvPopupTitle.text = "안전한 번호입니다"
                tvPopupTitle.setTextColor(Color.parseColor("#388E3C")) // 초록색
            }

            // 7. 닫기 버튼 누르면 팝업 끄기 (dismiss)
            btnClose.setOnClickListener {
                mAlertDialog.dismiss()
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
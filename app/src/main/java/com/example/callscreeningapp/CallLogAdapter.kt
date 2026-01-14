package com.example.callscreeningapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// [변경 1] List -> MutableList로 변경 (내용을 지워야 하니까요!)
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

        // [변경 2] 삭제 버튼 클릭 이벤트 추가
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
    }

    override fun getItemCount(): Int = items.size

    class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 기존 뷰들
        private val tvPhoneNumber: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        private val tvSpamTag: TextView = itemView.findViewById(R.id.tvSpamTag)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        // [변경 3] 삭제 버튼(X) 찾아오기
        // 보통 ImageView일 수도 있고 ImageButton일 수도 있으니 View 타입으로 받으면 편합니다.
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
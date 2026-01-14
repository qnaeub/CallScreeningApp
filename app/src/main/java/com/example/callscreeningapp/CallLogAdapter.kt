package com.example.callscreeningapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // 항목(Item) 자체 클릭 기능
        holder.itemView.setOnClickListener {
            // 안드로이드의 '토스트(Toast)' 메시지 띄우기
            android.widget.Toast.makeText(
                holder.itemView.context,
                "${item.phoneNumber} 번호로 연결하시겠습니까?", // 띄울 메시지
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
package com.example.callscreeningapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CallLogAdapter(private val items: List<CallLogItem>) :
    RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    // 1. 화면(Layout)을 생성하는 부분
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return CallLogViewHolder(view)
    }

    // 2. 데이터와 화면을 연결(Binding)하는 부분
    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    // 3. 리스트의 총 개수
    override fun getItemCount(): Int = items.size

    // 뷰홀더: 화면 요소들을 잡고 있는 클래스
    class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPhoneNumber: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        private val tvSpamTag: TextView = itemView.findViewById(R.id.tvSpamTag)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(item: CallLogItem) {
            tvPhoneNumber.text = item.phoneNumber
            tvDate.text = item.date
            tvSpamTag.text = item.spamTag

            // 스팸 여부에 따라 색상 다르게 표시
            if (item.isSpam) {
                tvSpamTag.setTextColor(Color.parseColor("#D32F2F")) // 빨간색
                tvSpamTag.setBackgroundResource(R.drawable.bg_tag_spam) // 아까 만든 배경
            } else {
                tvSpamTag.setTextColor(Color.parseColor("#388E3C")) // 초록색
                tvSpamTag.setBackgroundColor(Color.TRANSPARENT) // 배경 없음
            }
        }
    }
}
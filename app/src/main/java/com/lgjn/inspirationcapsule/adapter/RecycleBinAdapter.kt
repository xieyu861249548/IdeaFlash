package com.lgjn.inspirationcapsule.adapter

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.lgjn.inspirationcapsule.R
import com.lgjn.inspirationcapsule.data.Inspiration

class RecycleBinAdapter(
    private var items: List<Inspiration> = emptyList(),
    private val onRestore: (Inspiration) -> Unit,
    private val onDelete: (Inspiration) -> Unit
) : RecyclerView.Adapter<RecycleBinAdapter.ViewHolder>() {

    /** 当前展开按钮行的 position，-1 表示全部收起 */
    var expandedPosition: Int = -1
        private set

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: ConstraintLayout = view.findViewById(R.id.cardContainer)
        val tvContent: TextView = view.findViewById(R.id.tvRbContent)
        val tvDate: TextView = view.findViewById(R.id.tvRbDate)
        val actionsRow: LinearLayout = view.findViewById(R.id.actionsRow)
        val btnRestore: TextView = view.findViewById(R.id.btnRbRestore)
        val btnDelete: TextView = view.findViewById(R.id.btnRbDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recycle_bin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvContent.text = item.content
        holder.tvDate.text = item.formattedDate()

        // 是否展开按钮行
        val isExpanded = position == expandedPosition
        holder.actionsRow.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // 长按展开/收起
        holder.cardContainer.setOnLongClickListener {
            val adapterPos = holder.bindingAdapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnLongClickListener true

            val oldExpanded = expandedPosition
            if (oldExpanded == adapterPos) {
                // 再次长按同一项 → 收起
                expandedPosition = -1
                notifyItemChanged(adapterPos)
            } else {
                // 展开新的，收起旧的
                expandedPosition = adapterPos
                if (oldExpanded != -1) notifyItemChanged(oldExpanded)
                notifyItemChanged(adapterPos)
            }
            // 震动反馈
            holder.cardContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true
        }

        // 点击卡片区域：如果有展开的，则收起
        holder.cardContainer.setOnClickListener {
            if (expandedPosition != -1) {
                val old = expandedPosition
                expandedPosition = -1
                notifyItemChanged(old)
            }
        }

        // 展开时的按钮动画
        if (isExpanded) {
            holder.actionsRow.alpha = 0f
            holder.actionsRow.translationY = -10f
            holder.actionsRow.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // 按钮事件
        holder.btnRestore.setOnClickListener {
            val adapterPos = holder.bindingAdapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener
            expandedPosition = -1
            onRestore(items[adapterPos])
        }

        holder.btnDelete.setOnClickListener {
            val adapterPos = holder.bindingAdapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener
            expandedPosition = -1
            onDelete(items[adapterPos])
        }
    }

    override fun getItemCount(): Int = items.size

    /** 收起所有展开的按钮行 */
    fun collapseAll() {
        if (expandedPosition != -1) {
            val old = expandedPosition
            expandedPosition = -1
            notifyItemChanged(old)
        }
    }

    fun updateData(newItems: List<Inspiration>) {
        expandedPosition = -1
        items = newItems
        notifyDataSetChanged()
    }
}

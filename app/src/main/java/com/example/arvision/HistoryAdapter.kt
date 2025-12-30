package com.example.arvision

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private var historyItems: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var filteredItems: List<HistoryItem> = historyItems

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.history_item_icon)
        val title: TextView = view.findViewById(R.id.history_item_title)
        val data: TextView = view.findViewById(R.id.history_item_data)
        val timestamp: TextView = view.findViewById(R.id.history_item_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.title.text = getTitleForEventType(item.eventType)
        holder.data.text = item.data
        holder.timestamp.text = formatTimestamp(item.timestamp)
        holder.icon.setImageResource(getIconForEventType(item.eventType))
    }

    override fun getItemCount() = filteredItems.size

    fun updateData(newItems: List<HistoryItem>) {
        this.historyItems = newItems
    }

    fun filter(query: String?, eventTypes: Set<HistoryEventType>) {
        filteredItems = historyItems.filter { item ->
            val matchesQuery = query.isNullOrEmpty() || item.data.contains(query, ignoreCase = true) || getTitleForEventType(item.eventType).contains(query, ignoreCase = true)
            val matchesEventType = eventTypes.isEmpty() || eventTypes.contains(item.eventType)
            matchesQuery && matchesEventType
        }
        notifyDataSetChanged()
    }

    private fun getTitleForEventType(eventType: HistoryEventType): String {
        return when (eventType) {
            HistoryEventType.OBJECT_DESCRIBED -> "Object Described"
            HistoryEventType.TEXT_SELECTED -> "Text Selected"
            HistoryEventType.TEXT_COPIED -> "Text Copied"
            HistoryEventType.TEXT_TRANSLATED -> "Text Opened in Translate"
            HistoryEventType.TEXT_SEARCHED -> "Text Searched"
            HistoryEventType.TEXT_SHARED -> "Text Shared"
            HistoryEventType.DISTANCE_MEASURED -> "Distance Measured"
        }
    }

    private fun getIconForEventType(eventType: HistoryEventType): Int {
        return when (eventType) {
            HistoryEventType.OBJECT_DESCRIBED -> android.R.drawable.ic_menu_camera
            HistoryEventType.TEXT_SELECTED, HistoryEventType.TEXT_COPIED, HistoryEventType.TEXT_TRANSLATED, HistoryEventType.TEXT_SEARCHED, HistoryEventType.TEXT_SHARED -> android.R.drawable.ic_menu_edit
            HistoryEventType.DISTANCE_MEASURED -> android.R.drawable.ic_menu_sort_by_size
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }
}

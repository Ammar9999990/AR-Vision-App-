package com.example.arvision

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup

class HistoryFragment : Fragment() {

    private lateinit var adapter: HistoryAdapter
    private lateinit var searchBar: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var refreshButton: ImageButton
    private var allHistoryItems = mutableListOf<HistoryItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.history_recycler_view)
        searchBar = view.findViewById(R.id.history_search_bar)
        chipGroup = view.findViewById(R.id.history_chip_group)
        refreshButton = view.findViewById(R.id.refresh_button)

        adapter = HistoryAdapter(allHistoryItems)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        setupFiltering()
        setupRefresh()

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun setupFiltering() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performFiltering()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroup.setOnCheckedChangeListener { _, _ ->
            performFiltering()
        }
    }

    private fun setupRefresh() {
        refreshButton.setOnClickListener { refreshHistory() }
    }

    private fun refreshHistory() {
        allHistoryItems = HistoryManager.getHistory(requireContext())
        adapter.updateData(allHistoryItems)
        performFiltering() // Re-apply filters after refreshing data
    }

    private fun performFiltering() {
        val query = searchBar.text.toString()
        val selectedEventTypes = mutableSetOf<HistoryEventType>()

        when (chipGroup.checkedChipId) {
            R.id.chip_all, View.NO_ID -> {
                // No specific filter or 'All' is selected
            }
            R.id.chip_objects -> {
                selectedEventTypes.add(HistoryEventType.OBJECT_DESCRIBED)
            }
            R.id.chip_text -> {
                selectedEventTypes.addAll(setOf(HistoryEventType.TEXT_SELECTED, HistoryEventType.TEXT_COPIED, HistoryEventType.TEXT_SHARED, HistoryEventType.TEXT_SEARCHED, HistoryEventType.TEXT_TRANSLATED))
            }
            R.id.chip_distance -> {
                selectedEventTypes.add(HistoryEventType.DISTANCE_MEASURED)
            }
        }

        adapter.filter(query, selectedEventTypes)
    }
}

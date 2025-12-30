package com.example.arvision

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AiInsightsActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var insightsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_insights)

        progressBar = findViewById(R.id.ai_insights_progress_bar)
        insightsTextView = findViewById(R.id.ai_insights_textview)

        generateInsights()
    }

    private fun generateInsights() {
        progressBar.visibility = View.VISIBLE
        insightsTextView.visibility = View.GONE

        lifecycleScope.launch {
            val history = HistoryManager.getHistory(this@AiInsightsActivity)
            if (history.isEmpty()) {
                insightsTextView.text = "You don\'t have any history yet. Start exploring to get your personalized AI insights!"
                progressBar.visibility = View.GONE
                insightsTextView.visibility = View.VISIBLE
                return@launch
            }

            val summary = GeminiPro.generateActivitySummary(history)

            progressBar.visibility = View.GONE
            insightsTextView.text = summary
            insightsTextView.visibility = View.VISIBLE
        }
    }
}

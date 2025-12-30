package com.example.arvision

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val greetingTextView = view.findViewById<TextView>(R.id.home_greeting_title)
        val avatarTextView = view.findViewById<View>(R.id.home_top_bar)?.findViewById<TextView>(R.id.top_bar_user_avatar)

        // --- Make the UI Dynamic ---
        val currentUser = UserManager.getCurrentUser(requireContext())
        if (currentUser != null) {
            val firstName = currentUser.name.split(" ").firstOrNull() ?: "Explorer"
            greetingTextView.text = "Hello, $firstName 👋"
            avatarTextView?.text = currentUser.name.firstOrNull()?.toString()?.uppercase() ?: "U"
        } else {
            greetingTextView.text = "Hello, Explorer 👋"
            avatarTextView?.text = "U"
        }

        // --- Set up Click Listeners ---
        val startScanCard = view.findViewById<MaterialCardView>(R.id.home_start_scan_card)
        val objectDetectionCard = view.findViewById<MaterialCardView>(R.id.feature_object_detection)
        val liveTextCard = view.findViewById<MaterialCardView>(R.id.feature_live_text)
        val distanceMeasureCard = view.findViewById<MaterialCardView>(R.id.feature_distance_measure)
        val translationCard = view.findViewById<MaterialCardView>(R.id.feature_translation)
        val historyCard = view.findViewById<MaterialCardView>(R.id.feature_history)
        val aiInsightsCard = view.findViewById<MaterialCardView>(R.id.feature_ai_insights)

        startScanCard.setOnClickListener {
            val intent = Intent(activity, ObjectDetectionActivity::class.java)
            startActivity(intent)
        }

        historyCard.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.navigation_history)
        }

        objectDetectionCard.setOnClickListener {
            val intent = Intent(activity, ObjectDetectionActivity::class.java)
            startActivity(intent)
        }

        distanceMeasureCard.setOnClickListener {
            val intent = Intent(activity, DistanceMeasureActivity::class.java)
            startActivity(intent)
        }

        liveTextCard.setOnClickListener { 
            val intent = Intent(activity, TextRecognitionActivity::class.java)
            startActivity(intent)
        }
        
        translationCard.setOnClickListener { 
            val intent = Intent(activity, TranslationActivity::class.java)
            startActivity(intent)
        }

        aiInsightsCard.setOnClickListener {
            val intent = Intent(activity, AiInsightsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showComingSoon(feature: String) {
        Toast.makeText(context, "$feature: Coming soon!", Toast.LENGTH_SHORT).show()
    }
}

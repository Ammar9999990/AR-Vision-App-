package com.example.arvision

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signOutButton = view.findViewById<TextView>(R.id.sign_out_button)
        val editProfileButton = view.findViewById<Button>(R.id.profile_edit_button)
        val darkModeSwitch = view.findViewById<SwitchMaterial>(R.id.dark_mode_switch)
        val darkModeIcon = view.findViewById<ImageView>(R.id.dark_mode_icon)

        // Set initial state of the dark mode switch and icon
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        darkModeSwitch.isChecked = isNightMode
        updateDarkModeIcon(darkModeIcon, isNightMode)

        // Load and display user data
        updateUI()

        signOutButton.setOnClickListener {
            signOut()
        }

        editProfileButton.setOnClickListener {
            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            updateDarkModeIcon(darkModeIcon, isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when the fragment becomes visible
        updateUI()
    }

    private fun updateUI() {
        val currentUser = UserManager.getCurrentUser(requireContext())
        val history = HistoryManager.getHistory(requireContext())

        view?.let {
            val nameTextView = it.findViewById<TextView>(R.id.profile_name)
            val emailTextView = it.findViewById<TextView>(R.id.profile_email)
            val scansTextView = it.findViewById<TextView>(R.id.stats_scans_value)
            val objectsTextView = it.findViewById<TextView>(R.id.stats_objects_value)
            val daysActiveTextView = it.findViewById<TextView>(R.id.stats_days_active_value)

            if (currentUser != null) {
                nameTextView.text = currentUser.name
                emailTextView.text = currentUser.email

                // Set stats from HistoryManager
                scansTextView.text = history.size.toString()
                objectsTextView.text = history.count { item -> item.eventType == HistoryEventType.OBJECT_DESCRIBED }.toString()
                daysActiveTextView.text = calculateDaysActive(history).toString()

            } else {
                nameTextView.text = "Guest User"
                emailTextView.text = "guest@example.com"
                scansTextView.text = "0"
                objectsTextView.text = "0"
                daysActiveTextView.text = "0"
            }
        }
    }

    private fun calculateDaysActive(history: List<HistoryItem>): Int {
        if (history.isEmpty()) return 0
        return history.map { item ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = item.timestamp
            // Create a unique key for each day of the year
            cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR) * 1000
        }.toSet().size
    }

    private fun signOut() {
        // Sign out from Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // Clear the local user session and history
            UserManager.logout(requireContext())
            HistoryManager.clearHistory(requireContext())

            // Navigate back to LoginActivity
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun updateDarkModeIcon(iconView: ImageView, isNightMode: Boolean) {
        if (isNightMode) {
            iconView.setImageResource(R.drawable.ic_dark_mode_moon)
        } else {
            iconView.setImageResource(R.drawable.ic_light_mode_sun)
        }
    }
}

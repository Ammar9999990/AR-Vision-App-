package com.example.arvision

import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var historyFragment: HistoryFragment
    private lateinit var profileFragment: ProfileFragment
    private var activeFragment: Fragment? = null

    private val HOME_TAG = "home_fragment"
    private val HISTORY_TAG = "history_fragment"
    private val PROFILE_TAG = "profile_fragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            historyFragment = HistoryFragment()
            profileFragment = ProfileFragment()
            activeFragment = homeFragment

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, profileFragment, PROFILE_TAG).hide(profileFragment)
                add(R.id.fragment_container, historyFragment, HISTORY_TAG).hide(historyFragment)
                add(R.id.fragment_container, homeFragment, HOME_TAG)
            }.commit()
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag(HOME_TAG) as HomeFragment
            historyFragment = supportFragmentManager.findFragmentByTag(HISTORY_TAG) as HistoryFragment
            profileFragment = supportFragmentManager.findFragmentByTag(PROFILE_TAG) as ProfileFragment
            activeFragment = supportFragmentManager.fragments.find { it.isVisible } ?: homeFragment
        }

        bottomNavView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.navigation_scan) {
                // Launch ObjectDetectionActivity directly
                startActivity(Intent(this, ObjectDetectionActivity::class.java))
                false // Do not select the item, as we are leaving the activity
            } else {
                switchToTab(item.itemId)
                true
            }
        }
    }

    fun switchToTab(@IdRes tabId: Int) {
        val selectedFragment = when (tabId) {
            R.id.navigation_home -> homeFragment
            R.id.navigation_history -> historyFragment
            R.id.navigation_profile -> profileFragment
            else -> homeFragment
        }

        if (activeFragment != selectedFragment) {
            supportFragmentManager.beginTransaction().let { transaction ->
                activeFragment?.let { transaction.hide(it) }
                transaction.show(selectedFragment)
                transaction.commit()
            }
            activeFragment = selectedFragment
            findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = tabId
        }
    }

}

package com.example.arvision

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object DatabaseManager {

    private const val TAG = "DatabaseManager"
    private val db: FirebaseFirestore = Firebase.firestore

    fun saveHistoryItem(item: HistoryItem) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val userHistoryCollection = db.collection("users").document(currentUser.uid).collection("history")

        userHistoryCollection.document(item.timestamp.toString())
            .set(item)
            .addOnSuccessListener { Log.d(TAG, "HistoryItem saved to Firestore successfully.") }
            .addOnFailureListener { e -> Log.w(TAG, "Error saving HistoryItem to Firestore", e) }
    }

    fun syncHistoryOnLogin(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        Log.d(TAG, "Starting history sync for user: ${currentUser.uid}")

        val userHistoryCollection = db.collection("users").document(currentUser.uid).collection("history")

        userHistoryCollection
            .orderBy("timestamp", Query.Direction.DESCENDING) // Get newest items first
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No history found in Firestore for this user.")
                    // If there's no history online, we should clear any old local history
                    HistoryManager.clearHistory(context)
                    return@addOnSuccessListener
                }

                val historyItems = documents.map { it.toObject(HistoryItem::class.java) }
                Log.d(TAG, "Successfully fetched ${historyItems.size} items from Firestore.")
                HistoryManager.saveHistory(context, historyItems)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting history documents from Firestore: ", exception)
            }
    }
}

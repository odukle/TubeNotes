package com.odukle.ytnotes

import android.util.Log
import androidx.core.text.isDigitsOnly
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

const val USERS = "users"
const val NOTES = "notes"
const val MULTI_NOTES = "mNotes"
private const val TAG = "DatabaseHelper"

class DatabaseHelper {

    private lateinit var database: FirebaseFirestore

    fun open() {
        database = FirebaseFirestore.getInstance()
    }

    fun notesRef(): CollectionReference {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        return database.collection(USERS).document(userId!!).collection(NOTES)
    }

    fun addUser(
        userId: String,
        email: String,
        phoneNumber: String
    ) {
        Log.d(TAG, "addUser called")
        val dataUser = hashMapOf(
            "email" to email,
            "phoneNumber" to phoneNumber
        )
        database.collection(USERS).document(userId).set(dataUser, SetOptions.merge())
    }

    fun addNote(
        userId: String,
        title: String,
        timestamp: String,
        note: String,
        dateAddedLong: Long,
        url: String
    ): String {
        Log.d(TAG, "addNote called")
        val titleList = stringToList(title)
        val titleListNoCase = stringToListNoCase(title)
        val noteList = stringToList(note)
        val noteListNoCase = stringToListNoCase(note)
        val dataNote = hashMapOf(
            "id" to getVideoId(url),
            "title" to titleList,
            "titleNoCase" to titleListNoCase,
            "timestamp" to timestamp,
            "note" to noteList,
            "noteNoCase" to noteListNoCase,
            "dateAddedLong" to dateAddedLong,
            "url" to url
        )

        val docRef =
            database.collection(USERS).document(userId).collection(NOTES).document(getVideoId(url))
        docRef.get().continueWith { it ->
            if (it.result?.exists()!!) {
                Log.d(TAG, "document exists, add multi note")
                docRef.collection(MULTI_NOTES).document(timestamp).set(
                    hashMapOf(
                        timestamp to note
                    )
                )

                val noteListUpdate = it.result!!["noteNoCase"] as MutableList<String>
                noteListUpdate.addAll(noteListNoCase)
                docRef.update("noteNoCase", noteListUpdate)

            } else {
                Log.d(TAG, "document does not exist add new doc")
                docRef.set(dataNote)
                docRef.collection(MULTI_NOTES).document(timestamp).set(
                    hashMapOf(
                        timestamp to note
                    )
                )
            }.addOnCompleteListener {
                Log.d(TAG, "${it.isSuccessful}")
            }
        }
        return getVideoId(url)
    }

    fun updateNote(
        userId: String,
        id: String,
        title: String,
        timestamp: String,
        note: String,
        dateAddedLong: Long,
        url: String
    ) {
        Log.d(TAG, "updateNote called")
        val myRef = database.collection(USERS).document(userId).collection(NOTES).document(id)
        val titleList = stringToList(title)
        val titleListNoCase = stringToListNoCase(title)
        val noteList = stringToList(note)
        val noteListNoCase = stringToListNoCase(note)
        val dataNote = hashMapOf(
            "id" to getVideoId(url),
            "title" to titleList,
            "titleNoCase" to titleListNoCase,
            "timestamp" to timestamp,
            "note" to noteList,
            "noteNoCase" to noteListNoCase,
            "dateAddedLong" to dateAddedLong,
            "url" to url
        )

        myRef.set(dataNote, SetOptions.merge())

        val docRef =
            database.collection(USERS).document(userId).collection(NOTES).document(getVideoId(url))
        docRef.get().continueWith { it ->
            if (it.result?.exists()!!) {
                Log.d(TAG, "document exists, add multi note")
                docRef.collection(MULTI_NOTES).document(ogts ?: "0").delete()
                docRef.collection(MULTI_NOTES).document(timestamp).set(
                    hashMapOf(
                        timestamp to note
                    )
                )

                val noteListUpdate = it.result!!["noteNoCase"] as MutableList<String>
                noteListUpdate.addAll(noteListNoCase)
                docRef.update("noteNoCase", noteListUpdate)

            } else {
                Log.d(TAG, "document does not exist add new doc")
                docRef.set(dataNote)
                docRef.collection(MULTI_NOTES).document(timestamp).set(
                    hashMapOf(
                        timestamp to note
                    )
                )
            }.addOnCompleteListener {
                Log.d(TAG, "${it.isSuccessful}")
            }
        }

        if (id.isDigitsOnly()) {
            database.collection(USERS).document(userId).collection(NOTES).document(id).delete()
            database.collection(USERS).document(userId).collection(NOTES).document(id).collection(
                MULTI_NOTES
            ).get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (it.result != null) {
                            for (doc in it.result!!) {
                                doc.reference.delete()
                            }
                        }
                    }
                    Log.d(TAG, "deleted note id is $id")
                }
        }

    }
}

fun stringToList(s: String): List<String> {
    return s.split(" ").toList()
}

fun stringToListNoCase(s: String): MutableList<String> {
    return s.toLowerCase(Locale.ROOT).split(" ").toMutableList()
}

fun listToString(list: List<String>?): String {
    val sb = StringBuilder()
    list?.forEach {
        sb.append(it)
        if (it != list[list.size - 1]) {
            sb.append(" ")
        }
    }

    return sb.toString()
}
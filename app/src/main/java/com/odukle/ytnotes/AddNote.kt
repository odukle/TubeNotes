package com.odukle.ytnotes

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.add_note.*
import kotlinx.android.synthetic.main.add_note_dialog_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddNote : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var currentUser: FirebaseUser? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.add_note_dialog_layout)

        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView_dialog.loadAd(adRequest)

        progress_bar_dialog.visibility = View.GONE
        setFinishOnTouchOutside(false)

        val receivedTextAppStarting = intent?.getStringExtra(Intent.EXTRA_TEXT)

        val url = intent?.getStringExtra(SEND_INTENT_EXTRA) ?: receivedTextAppStarting
        yt_url.text = url
        currentUser = FirebaseAuth.getInstance().currentUser
        dbHelper = DatabaseHelper()
        dbHelper.open()

        if (url != null) {
            CoroutineScope(IO).launch {
                val ytTitle = getTitle(url, this@AddNote)
                withContext(Main) {
                    title = ytTitle
                }
            }
        }

        addNote_save_dialog.setOnClickListener {
            CoroutineScope(IO).launch {
                val sn = saveNote()
                if (sn) {
                    withContext(Main) {
                        Toast.makeText(this@AddNote, "Note saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }.invokeOnCompletion {
                finish()
            }
        }

        addNote_addMore_dialog.setOnClickListener {
            CoroutineScope(IO).launch {
                if (saveNote()) {
                    withContext(Main) {
                        Toast.makeText(this@AddNote, "Note saved, Add more", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }.invokeOnCompletion {
                addNote_note_dialog.text.clear()
                addNote_hours_dialog.text.clear()
                addNote_minutes_dialog.text.clear()
                addNote_sec_dialog.text.clear()
            }

        }

        addNote_share_dialog_dialog.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                val timestamp = toTimestamp(Integer.parseInt(getTimestamp()))
                putExtra(
                    Intent.EXTRA_TEXT,
                    urlWithTimestamp(
                        yt_url.text.toString(),
                        getTimestamp()
                    ) + "\n\nat $timestamp" + "\n${addNote_note_dialog.text}"
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            if (yt_url.text.toString().isNotEmpty()) {
                startActivity(shareIntent)
            } else {
                Toast.makeText(this, "Title is empty", Toast.LENGTH_SHORT).show()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun saveNote(): Boolean {

        withContext(Main) {
            progress_bar_dialog.visibility = View.VISIBLE
        }

        val newNote = noteFromUi()
        if (newNote != null) {
            if (isOnline(this)) {
                val videoTitle = getTitle(yt_url.text.toString(), this)
                dbHelper.addNote(
                    currentUser?.uid ?: "0",
                    videoTitle,
                    getTimestamp(),
                    addNote_note_dialog.text.toString(),
                    getDateTime(),
                    yt_url.text.toString()
                )

                withContext(Main) {
                    progress_bar_dialog.visibility = View.GONE
                }

                return true
            } else {
                withContext(Main) {
                    progress_bar_dialog.visibility = View.GONE
                    Toast.makeText(this@AddNote, "No internet!", Toast.LENGTH_SHORT).show()
                }

                return false
            }
        }

        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun noteFromUi(): Note? {
        return if (yt_url.text.isNotEmpty()) {
            val videoTitle = getTitle(yt_url.text.toString(), this)
            val noteNote = addNote_note_dialog.text.toString()
            Note(
                stringToList(videoTitle),
                stringToListNoCase(videoTitle),
                getTimestamp(),
                stringToList(noteNote),
                stringToListNoCase(noteNote),
                getDateTime().toString(),
                getDateTime(),
                yt_url.text.toString()
            )
        } else {
            null
        }
    }

    private fun getTimestamp(): String {
        val hh: Int = if (addNote_hours_dialog.text.isNotEmpty()) {
            addNote_hours_dialog.text.toString().toInt()
        } else {
            0
        }

        val mm: Int = if (addNote_minutes_dialog.text.isNotEmpty()) {
            addNote_minutes_dialog.text.toString().toInt()
        } else {
            0
        }
        val ss: Int = if (addNote_sec_dialog.text.isNotEmpty()) {
            addNote_sec_dialog.text.toString().toInt()
        } else {
            0
        }
        return ((hh * 3600 + mm * 60 + ss) * 1000).toString()
    }

}
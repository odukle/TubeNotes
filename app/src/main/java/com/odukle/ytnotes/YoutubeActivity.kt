package com.odukle.ytnotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.player_view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


private const val TAG = "YouTubeActivity"
private const val DIALOG_REQUEST_CODE = 1
var ya: YoutubeActivity? = null

class YoutubeActivity : YouTubeBaseActivity(), YouTubePlayer.OnInitializedListener {

    private var player: YouTubePlayer? = null
    private var adapter: NoteListAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.player_view)

        ya = this
        val note = intent.getParcelableExtra<Note>(NOTE_OBJECT_KEY)

        val playerView: YouTubePlayerView = playerView
        playerView_title.text = listToString(note?.title)
        playerView_note.text = listToString(note?.note)
        note_list_rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        playerView.initialize(getString(R.string.GOOGLE_API_KEY), this)

        play_on_youtube.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(urlWithTimestamp(note?.url ?: "", note?.timestamp ?: ""))
            startActivity(intent)
        }
        Log.d(TAG, "onCreate: ends")
    }

    override fun onInitializationSuccess(
        provider: YouTubePlayer.Provider?,
        youTubePlayer: YouTubePlayer?,
        wasRestored: Boolean
    ) {
        Log.d(TAG, "onInitializationSuccess: provider is ${provider?.javaClass}")
        Log.d(TAG, "onInitializationSuccess: youTubePlayer is ${youTubePlayer?.javaClass}")
        loadNoteList()
        player = youTubePlayer
        val note = intent.getParcelableExtra<Note>(NOTE_OBJECT_KEY)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        FirebaseFirestore.getInstance()
        adapter =
            note?.let { NoteListAdapter(userId!!, mutableListOf(), player!!, it, playerView_note) }

        loadNoteList()
        if (!wasRestored) {
            youTubePlayer?.loadVideo(
                note?.url?.let { getVideoId(it) },
                Integer.parseInt(note?.timestamp ?: "0")
            )
            Toast.makeText(
                this,
                "Playing from ${toTimestamp(Integer.parseInt(note?.timestamp ?: "0"))}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            youTubePlayer?.play()
        }
        Log.d(TAG, "onInitializationSuccess ends")
    }

    override fun onInitializationFailure(
        provider: YouTubePlayer.Provider?,
        youTubeInitializationResult: YouTubeInitializationResult?
    ) {
        loadNoteList()
        if (youTubeInitializationResult?.isUserRecoverableError == true) {
            youTubeInitializationResult.getErrorDialog(this, DIALOG_REQUEST_CODE).show()
        } else {
            val errorMessage =
                "There was an error initializing the YouTubePlayer $youTubeInitializationResult"
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
        Log.d(TAG, "onInitializationFailure ends")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(
            TAG,
            "onActivityResult called with response code $resultCode for request $requestCode"
        )
        if (requestCode == DIALOG_REQUEST_CODE) {
            Log.d(TAG, intent.toString())
            Log.d(TAG, intent.extras.toString())
            playerView.initialize(getString(R.string.GOOGLE_API_KEY), this)
        }
    }

    private fun loadNoteList() {
        CoroutineScope(IO).launch {
            val note = intent.getParcelableExtra<Note>(NOTE_OBJECT_KEY)

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val db = FirebaseFirestore.getInstance()
            if (note != null) {
                if (userId != null) {
                    Log.d(TAG, "note id is ${note.id}")
                    db.collection(USERS).document(userId).collection(NOTES).document(note.id)
                        .collection(
                            MULTI_NOTES
                        ).get().addOnCompleteListener { task ->
                            val mNoteList = mutableListOf<String>()
                            if (task.isSuccessful) {
                                if (task.result != null) {
                                    for (doc in task.result!!) {
                                        val mNote = doc.data
                                        mNote.forEach {
                                            mNoteList.add("at ${toTimestamp(Integer.parseInt(it.key))}: \n${it.value} \n `${it.key}".trim())
                                        }
                                    }
                                    adapter = player?.let {
                                        NoteListAdapter(
                                            userId,
                                            mNoteList,
                                            it,
                                            note,
                                            playerView_note
                                        )
                                    }
                                    note_list_rv.layoutManager =
                                        LinearLayoutManager(this@YoutubeActivity)
                                    note_list_rv.adapter = adapter
                                    Log.d(TAG, "adapter attached")
                                    adapter?.notifyDataSetChanged()
                                }
                            }
                        }
                }

            }
            this@YoutubeActivity.runOnUiThread {
                progress_bar_pv.visibility = View.GONE
            }

        }
    }

}
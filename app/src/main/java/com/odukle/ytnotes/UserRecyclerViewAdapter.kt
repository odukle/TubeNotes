package com.odukle.ytnotes

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.odukle.ytnotes.MainActivity.Companion.adCount
import com.odukle.ytnotes.MainActivity.Companion.mInterstitialAd
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

private const val TAG = "UserRecyclerViewAdapter"

open class UserRecyclerViewAdapter(
    options: FirestoreRecyclerOptions<Note>,
    private var listener: OnNoteClickListener,
    val context: Context
) : FirestoreRecyclerAdapter<Note, UserRecyclerViewAdapter.NoteViewHolder>(options) {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val videoTitle: TextView = view.findViewById(R.id.video_title)
        private val videoNote: TextView = view.findViewById(R.id.video_note)
        private val videoThumbnail: ImageView = view.findViewById(R.id.youtube_thumbnail)
        private val editButton: ImageButton = view.findViewById(R.id.btn_edit)
        private val shareButton: ImageButton = view.findViewById(R.id.btn_share)
        private val mSpinner: Spinner = view.findViewById(R.id.mSpinner)
        private val progressBar: ProgressBar = view.findViewById(R.id.progress_bar_rvi)

        @SuppressLint("SetTextI18n")
        fun bind(note: Note, listener: OnNoteClickListener, context: Context) {
            Log.d(TAG, "bind called")
            videoTitle.text = listToString(note.title)
            videoNote.text =
                "at ${toTimestamp(Integer.parseInt(note.timestamp ?: "0"))} :\n${listToString(note.note)}"

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val noteId = this@UserRecyclerViewAdapter.getItem(this.adapterPosition).id
            val db = FirebaseFirestore.getInstance()
            var adapter = ArrayAdapter(
                fa?.applicationContext!!,
                R.layout.spinner_layout,
                mutableListOf<String>()
            )
            mSpinner.adapter = adapter

            CoroutineScope(IO).launch {
                fa?.runOnUiThread {
                    mSpinner.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                }
                db.collection(USERS).document(userId!!).collection(NOTES).document(noteId)
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
                                adapter = ArrayAdapter(
                                    fa?.applicationContext!!,
                                    R.layout.spinner_layout,
                                    mNoteList
                                )
                                mSpinner.adapter = adapter
                                adapter.notifyDataSetChanged()
                                fa?.runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    mSpinner.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
            }

            if (note.url != null) {
                Picasso.get().load(getThumbnailUrl(getVideoId(note.url!!)))
                    .error(R.drawable.tube_placeholder)
                    .placeholder(R.drawable.tube_placeholder)
                    .into(videoThumbnail)
            }

            videoThumbnail.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {

                    adCount++
                    context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                        .edit().putInt(I_AD_COUNT, adCount).apply()
                    if (adCount > 3) {
                        if (mInterstitialAd.isLoaded) {
                            mInterstitialAd.show()
                            adCount = 0
                            context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                                .edit().putInt(I_AD_COUNT, adCount).apply()
                        }
                    }

                    listener.onItemClick(note, position)
                }
            }

            videoNote.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {

                    adCount++
                    context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                        .edit().putInt(I_AD_COUNT, adCount).apply()
                    if (adCount > 3) {
                        if (mInterstitialAd.isLoaded) {
                            mInterstitialAd.show()
                            adCount = 0
                            context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                                .edit().putInt(I_AD_COUNT, adCount).apply()
                        }
                    }

                    listener.onItemClick(note, position)
                }
            }

            videoTitle.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {

                    adCount++
                    context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                        .edit().putInt(I_AD_COUNT, adCount).apply()
                    if (adCount > 3) {
                        if (mInterstitialAd.isLoaded) {
                            mInterstitialAd.show()
                            adCount = 0
                            context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                                .edit().putInt(I_AD_COUNT, adCount).apply()
                        }
                    }

                    listener.onItemClick(note, position)
                }
            }

            editButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(snapshots.getSnapshot(position), position)
                }
            }

            shareButton.setOnClickListener {
                listener.onShareClick(note)
            }

            mSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val noteNote = parent?.getItemAtPosition(position).toString()
                    val si = noteNote.indexOf("`") + 1
                    val ts = noteNote.substring(si, noteNote.lastIndex) + 1
                    videoNote.text = noteNote
                    val mNote =
                        this@UserRecyclerViewAdapter.getItem(this@NoteViewHolder.adapterPosition)
                    mNote.timestamp = ts
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        }

    }

    interface OnNoteClickListener {
        fun onItemClick(note: Note, position: Int)
        fun onEditClick(documentSnapshot: DocumentSnapshot, position: Int)
        fun onShareClick(note: Note)
    }

    fun setOnEditClickListener(listener: OnNoteClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteViewHolder {
        Log.d(TAG, "onCreateViewHolder: new view requested")
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int,
        model: Note
    ) {
        Log.d(TAG, "onBindViewHolder called")
        holder.bind(model, listener, context)
    }

    fun deleteItem(position: Int) {
        snapshots.getSnapshot(position).reference.delete()
    }

    fun addItem(note: Note) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val dbHelper = DatabaseHelper()
        dbHelper.open()
        dbHelper.addNote(
            currentUser!!.uid,
            listToString(note.title),
            note.timestamp!!,
            listToString(note.note),
            note.dateAddedLong!!,
            note.url!!
        )
    }
}

private fun getThumbnailUrl(id: String): String {
    Log.d(TAG, "getThumbnailUrl called")
    return "http://img.youtube.com/vi/$id/sddefault.jpg"
}
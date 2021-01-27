package com.odukle.ytnotes

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.youtube.player.YouTubePlayer
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "NoteListAdapter"

class NoteListAdapter(
    val userId: String,
    private val noteList: MutableList<String>,
    val player: YouTubePlayer,
    val note: Note,
    val textView: TextView
) : RecyclerView.Adapter<NoteListAdapter.NLViewHolder>() {

    inner class NLViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deleteButton: ImageButton = view.findViewById(R.id.nlv_deleteButton)
        private val playButton: ImageButton = view.findViewById(R.id.nlv_playButton)
        private val noteListNote: TextView = view.findViewById(R.id.nlv_note)

        fun bind(position: Int) {

            noteListNote.text = noteList[position]

            deleteButton.setOnClickListener {
                val dialog = MaterialAlertDialogBuilder(ya!!)
                dialog.setTitle("Confirm Delete")
                    .setBackground(ContextCompat.getDrawable(ya!!, R.color.primary_text))
                    .setPositiveButton(
                        "Yes"
                    ) { dialog, which ->
                        val db = FirebaseFirestore.getInstance()
                        val ts = noteList[position].substring(noteList[position].indexOf("`") + 1)
                        db.collection(USERS)
                            .document(userId)
                            .collection(NOTES)
                            .document(note.id)
                            .collection(MULTI_NOTES)
                            .document(ts)
                            .delete()

                        this@NoteListAdapter.noteList.removeAt(position)
                        this@NoteListAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            ya!!,
                            "Deleted note at ${toTimestamp(ts.toInt())}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.setNegativeButton(
                        "Cancel"
                    ) { dialog, which -> dialog?.dismiss() }.show()


            }

            noteListNote.setOnClickListener {
                textView.text = noteList[position].substring(
                    noteList[position].indexOf(":") + 2,
                    noteList[position].indexOf("`") - 1
                ).trim()
            }

            playButton.setOnClickListener {
                note.timestamp = noteList[position].substring(noteList[position].indexOf("`") + 1)
                player.seekToMillis(Integer.parseInt(note.timestamp ?: "0"))
                Toast.makeText(
                    ya?.applicationContext,
                    "Playing from ${toTimestamp(Integer.parseInt(note.timestamp ?: "0"))}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteListAdapter.NLViewHolder {
        Log.d(TAG, "onCreateViewHolder: new view requested")
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.note_list_item, parent, false)
        return NLViewHolder(view)
    }

    override fun onBindViewHolder(holder: NLViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return noteList.size
    }


}
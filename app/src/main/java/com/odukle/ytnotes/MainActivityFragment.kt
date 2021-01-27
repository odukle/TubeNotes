package com.odukle.ytnotes

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_instructions.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.util.*


private const val TAG = "MainActivityFragment"
const val NOTE_OBJECT_KEY = "keynote"
const val NOTE_TIMESTAMP = "noteStamp"
const val ARG_NOTE_UPDATE = "updateNote"
const val ARG_NOTE_NOTE = "noteNote"

class MainActivityFragment : Fragment(), UserRecyclerViewAdapter.OnNoteClickListener {

    interface OnNoteEdit {
        fun onNoteEdit(note: Note)
    }

    private var mAdapter: UserRecyclerViewAdapter? = null

    private fun getAdapter(options: FirestoreRecyclerOptions<Note>): UserRecyclerViewAdapter {
        return object : UserRecyclerViewAdapter(options, this, requireContext()) {
            override fun onDataChanged() {
                if (itemCount == 0) {
                    Log.d(TAG, "$itemCount : inst visible")
                    instructions_fragment.visibility = View.VISIBLE
                    input_searchByTitle.visibility = View.INVISIBLE
                    swipe_delete.visibility = View.GONE
                } else {
                    Log.d(TAG, "$itemCount : inst gone")
                    instructions_fragment.visibility = View.GONE
                    input_searchByTitle.visibility = View.VISIBLE

                    if (swipe_delete != null) {
                        swipe_delete?.animate()?.translationX(200f)?.setDuration(1000)
                            ?.withEndAction {
                                swipe_delete?.postDelayed({
                                    swipe_delete?.animate()?.translationX(2000f)?.setDuration(1000)
                                        ?.withEndAction {
                                            swipe_delete?.visibility = View.GONE
                                        }
                                }, 1000)
                            }
                    }
                }
            }
        }
    }

    private fun loadNotes(searchText: String?) {
        Log.d(TAG, "LoadNotes called")
        val currentUser = FirebaseAuth.getInstance().currentUser
        note_list.layoutManager = LinearLayoutManager(context)
        mAdapter?.stopListening()
        val db = FirebaseFirestore.getInstance()
        val noteRef = db.collection(USERS).document(currentUser?.uid ?: "0").collection(NOTES)
        if (searchText != null) {
            var query: Query? = null
            if (input_searchByTitle.isFocused) {
                query = noteRef.orderBy("titleNoCase", Query.Direction.DESCENDING)
                    .whereArrayContainsAny("titleNoCase", stringToListNoCase(searchText))
            } else if (input_searchByNote.isFocused) {
                query = noteRef.orderBy("noteNoCase", Query.Direction.DESCENDING)
                    .whereArrayContainsAny("noteNoCase" , stringToListNoCase(searchText))
            }
            val options =
                query?.let {
                    FirestoreRecyclerOptions.Builder<Note>().setQuery(it, Note::class.java)
                        .build()
                }
//            mAdapter = options?.let { UserRecyclerViewAdapter(it, this) }
            mAdapter = options?.let { getAdapter(it) }
            note_list.adapter = mAdapter
            mAdapter?.startListening()
        } else {
            val query = noteRef.orderBy("dateAddedLong", Query.Direction.DESCENDING)
            val options =
                FirestoreRecyclerOptions.Builder<Note>().setQuery(query, Note::class.java).build()
//            mAdapter = UserRecyclerViewAdapter(options, this)
            mAdapter = getAdapter(options)
            note_list.adapter = mAdapter
            mAdapter?.startListening()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: starts")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView called")
        return layoutInflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttach called")
        super.onAttach(context)

        if (context !is OnNoteEdit) {
            throw RuntimeException("$context must implement OnNoteEdit")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated called")
        super.onViewCreated(view, savedInstanceState)


        input_searchByTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    loadNotes(s.toString().toLowerCase(Locale.ROOT))
                } else {
                    loadNotes(null)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) {
                    loadNotes(s.toString().toLowerCase(Locale.ROOT))
                } else {
                    loadNotes(null)
                }
            }

        })

//        input_searchByNote.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (s.toString().isNotEmpty()) {
//                    loadNotes(s.toString().toLowerCase(Locale.ROOT))
//                } else {
//                    loadNotes(null)
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//                if (s.toString().isNotEmpty()) {
//                    loadNotes(s.toString().toLowerCase(Locale.ROOT))
//                } else {
//                    loadNotes(null)
//                }
//            }
//
//        })

        loadNotes(null)

        val itemTouchCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            val icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.twotone_delete_forever_white_48dp
            )

            val backgroundLeft = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.left_swipe_bg
            )
            val backgroundRight = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.right_swipe_bg
            )

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val note = mAdapter!!.getItem(viewHolder.adapterPosition)
                mAdapter!!.deleteItem(viewHolder.adapterPosition)
                val snackbar = Snackbar.make(mainFragment, "Note deleted", Snackbar.LENGTH_LONG)
                snackbar.setAction("Undo") {
                    mAdapter!!.addItem(note)
                }.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (event == DISMISS_EVENT_TIMEOUT) {
                            val dbHelper = DatabaseHelper()
                            dbHelper.open()
                            dbHelper.notesRef().document(note.id).collection(MULTI_NOTES).get()
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        if (it.result != null) {
                                            for (doc in it.result!!) {
                                                doc.reference.delete()
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }).show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )

                val itemView = viewHolder.itemView
                val backgroundCornerOffset = 20
                val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight
                if (dX > 0) { // Swiping to the right
                    val iconLeft = itemView.left + iconMargin / 2
                    val iconRight = itemView.left + iconMargin / 2 + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    backgroundRight?.setBounds(
                        itemView.left, itemView.top,
                        itemView.left + dX.toInt() + backgroundCornerOffset,
                        itemView.bottom
                    )
                    backgroundRight?.draw(c)
                    icon.draw(c)
                } else if (dX < 0) { // Swiping to the left
                    val iconLeft = itemView.right - iconMargin / 2 - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin / 2
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    backgroundLeft?.setBounds(
                        itemView.right + dX.toInt() - backgroundCornerOffset,
                        itemView.top, itemView.right, itemView.bottom
                    )
                    backgroundLeft?.draw(c)
                    icon.draw(c)
                } else { // view is unSwiped
                    backgroundRight?.setBounds(0, 0, 0, 0)
                }

            }


        }

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(note_list)

        mAdapter!!.setOnEditClickListener(this)
    }

    override fun onStart() {
        Log.d(TAG, "onStart called")
        super.onStart()
        mAdapter!!.startListening()
    }

    override fun onStop() {
        Log.d(TAG, "onStop called")
        super.onStop()
        mAdapter!!.stopListening()
    }

    override fun onItemClick(note: Note, position: Int) {
        try {
            Log.d(TAG, "onRecyclerViewItemClicked called")
            val intent = Intent(context, YoutubeActivity::class.java)
            val view = note_list.layoutManager?.findViewByPosition(position)
            val videoNote = view?.findViewById<TextView>(R.id.video_note)
            val vnt = videoNote?.text.toString()
            val ts = vnt.substring(vnt.indexOf("`") + 1)
            val noteNote = vnt.substring(vnt.indexOf(":") + 2, vnt.indexOf("`") - 1).trim()
            note.note = stringToList(noteNote)
            note.timestamp = ts
            intent.putExtra(NOTE_OBJECT_KEY, note)
            intent.putExtra(NOTE_TIMESTAMP, ts)
            startActivity(intent)
        } catch (e: StringIndexOutOfBoundsException) {
            Toast.makeText(
                requireContext(),
                "Fetching notes, please wait",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    override fun onEditClick(documentSnapshot: DocumentSnapshot, position: Int) {
        try {
            Log.d(TAG, "onEditClick called")
            val note = documentSnapshot.toObject(Note::class.java)
            val view = note_list.layoutManager?.findViewByPosition(position)
            val videoNote = view?.findViewById<TextView>(R.id.video_note)
            val vnt = videoNote?.text.toString()
            val ts = vnt.substring(vnt.indexOf("`") + 1)
            val noteNote = vnt.substring(vnt.indexOf(":") + 2, vnt.indexOf("`") - 1).trim()
            note?.note = stringToList(noteNote)
            note?.timestamp = ts
            val id = documentSnapshot["id"]
            arguments = Bundle().apply {
                putString(ARG_NOTE_UPDATE, id as String?)
            }
            if (note != null) {
                (activity as OnNoteEdit).onNoteEdit(note)
            }
        } catch (e: StringIndexOutOfBoundsException) {
            Toast.makeText(
                requireContext(),
                "Fetching notes, please wait",
                Toast.LENGTH_SHORT
            ).show()
        }


    }

    override fun onShareClick(note: Note) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            val timestamp = toTimestamp(Integer.parseInt(note.timestamp ?: "0"))
            putExtra(
                Intent.EXTRA_TEXT,
                urlWithTimestamp(
                    note.url ?: "",
                    note.timestamp ?: ""
                ) + "\n\nat $timestamp" + "\n${listToString(note.note)}"
            )
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

}

fun toTimestamp(ms: Int): String {
    val t = ms / 1000
    val hh = t / 3600
    val mm = (t % 3600) / 60
    val ss = t % 60

    val hr = when (hh) {
        0 -> ""
        1 -> "$hh hr"
        else -> "$hh hrs"
    }

    val min = when (mm) {
        0 -> ""
        1 -> "$mm min"
        else -> "$mm mins"
    }

    val sec = when (ss) {
        0 -> ""
        1 -> "$ss sec"
        else -> "$ss secs"
    }

    return if (hh == 0 && mm == 0 && ss == 0) {
        "0 hr 0 min 0 sec"
    } else {
        "$hr $min $sec"
    }
}

fun urlWithTimestamp(url: String, ts: String): String {

    val sb = StringBuilder()
    val indexV = url.indexOf("?v=")
    val time = Integer.parseInt(ts)
    if (indexV > 0) {
        sb.append(url).append("&t=").append(time / 1000)
    } else {
        sb.append(url).append("?t=").append(time / 1000)
    }
    return sb.toString()
}
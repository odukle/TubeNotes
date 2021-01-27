package com.odukle.ytnotes


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.add_note.*
import kotlinx.android.synthetic.main.recycler_view_item.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val TAG = "AddEditFragment"
private const val ARG_NOTE = "note"
private const val ARG_NOTE_URL = "noteUrl"
var ogts: String? = null
var noteUrl: String? = null

/**
 * ANY ACTIVITY THAT WANTS TO USE THIS FRAGMENT MUST IMPLEMENT OnSaveClicked() interface
 * A simple [Fragment] subclass.
 * Use the [AddEditNote.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddEditNote : Fragment() {
    private var listener: OnSaveClicked? = null
    private var note: Note? = null
    private var dbHelper: DatabaseHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: starts")
        super.onCreate(savedInstanceState)
        note = arguments?.getParcelable(ARG_NOTE)
        if (note != null) {
            noteUrl = note!!.url
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: starts")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.add_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated starts")
        progress_bar.visibility = View.GONE
        addNote_hours.minValue = 0
        addNote_hours.maxValue = 571
        addNote_minutes.minValue = 0
        addNote_minutes.maxValue = 59
        addNote_sec.minValue = 0
        addNote_sec.maxValue = 59

        adView.loadAd(fa?.adRequest)

        val note = note
        dbHelper = DatabaseHelper()
        dbHelper!!.open()
        if (savedInstanceState == null) {

            if (note != null) {
                Log.d(TAG, "onViewCreated: note details found, editing note ${note.id}")
                addNote_url.setText(note.url)
                addNote_note.setText(listToString(note.note))
                ogts = note.timestamp
                Log.d(TAG, "addNote_note text is ${addNote_note.text}")
                if (toHH(note.timestamp).isNotEmpty()) {
                    addNote_hours.value = Integer.parseInt(toHH(note.timestamp))
                } else {
                    addNote_hours.value = 0
                }
                if (toMM(note.timestamp).isNotEmpty()) {
                    addNote_minutes.value = Integer.parseInt(toMM(note.timestamp))
                } else {
                    addNote_minutes.value = 0
                }
                if (toSS(note.timestamp).isNotEmpty()) {
                    addNote_sec.value = Integer.parseInt(toSS(note.timestamp))
                } else {
                    addNote_sec.value = 0
                }

            } else {
                addNote_url.setText(noteUrl)
                Log.d(TAG, "onViewCreated: no  arguments, adding new record")
                val receivedTextAppRunning = activity?.intent?.getStringExtra(SEND_INTENT_EXTRA)
                val receivedTextAppStarting = activity?.intent?.getStringExtra(Intent.EXTRA_TEXT)
                if (receivedTextAppRunning == null) {
                    if (receivedTextAppStarting != null) {
                        addNote_url.setText(receivedTextAppStarting)
                    }
                } else {
                    addNote_url.setText(receivedTextAppRunning)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun noteFromUi(): Note? {
        Log.d(TAG, "noteFromUi called")

        return if (addNote_url.text.toString().isNotEmpty()) {
            val title = getTitle(addNote_url.text.toString(), requireContext())
            val noteNote = addNote_note.text.toString()
            Note(
                stringToList(title),
                stringToListNoCase(title),
                getTimestamp(),
                stringToList(noteNote),
                stringToListNoCase(noteNote),
                getDateTime().toString(),
                getDateTime(),
                addNote_url.text.toString()
            )
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun saveNote(): Boolean {
        Log.d(TAG, "saveNote called")
        var newNote: Note? = null
        if (measureTimeMillis {
                activity?.runOnUiThread {
                    progress_bar.visibility = View.VISIBLE
                }
                newNote = noteFromUi()
            } >= 5000L) {
            activity?.runOnUiThread {
                Toast.makeText(context, "Check your internet connection", Toast.LENGTH_SHORT).show()
                progress_bar.visibility = View.GONE
            }
            return true
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (note != null) {
            if (newNote != null) {
                if (newNote!!.note != note!!.note || newNote!!.timestamp != note!!.timestamp) {
                    val noteId = getVideoId(note!!.url!!)
                    Log.d(TAG, "saveNote: updating note id is $noteId")

                    return if (isOnline(requireContext())) {
                        dbHelper?.updateNote(
                            currentUser?.uid ?: "0",
                            note!!.id,
                            listToString(newNote!!.title),
                            getTimestamp(),
                            listToString(newNote!!.note),
                            getDateTime(),
                            newNote!!.url!!
                        )
                        Log.d(TAG, "saveNote: note updated id is $noteId")
                        true
                    } else {
                        activity?.runOnUiThread {
                            progress_bar.visibility = View.GONE
                        }
                        true
                    }
                }
                return true
            }
            return false
        }
        Log.d(TAG, "saveNote: adding note id is ${newNote?.id}")
        if (newNote != null) {
            if (isOnline(requireContext())) {
                val title = getTitle(addNote_url.text.toString(), requireContext())
                val id = dbHelper?.addNote(
                    currentUser?.uid ?: "0",
                    title,
                    getTimestamp(),
                    addNote_note.text.toString(),
                    getDateTime(),
                    addNote_url.text.toString()
                )

                val titleList = stringToList(title)
                val titleListNoCase = stringToListNoCase(title)
                val noteList = stringToList(addNote_note.text.toString())
                val noteListNoCase = stringToListNoCase(addNote_note.text.toString())
                note = Note(
                    titleList,
                    titleListNoCase,
                    getTimestamp(),
                    noteList,
                    noteListNoCase,
                    getDateTime().toString(),
                    getDateTime(),
                    addNote_url.text.toString(),
                    id = id ?: "0"
                )
                Log.d(TAG, "saveNote: passing url ${addNote_url.text}")
                note?.id = id ?: "id is null check addNote"
                Log.d(TAG, "saveNote: note added id is ${note?.id}")
                return true
            } else {
                activity?.runOnUiThread {
                    progress_bar.visibility = View.GONE
                }
                return true
            }

        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated: starts")
        super.onActivityCreated(savedInstanceState)

        if (listener is AppCompatActivity) {
//        val actionBar = (listener as AppCompatActivity).supportActionBar   this could have also be done but the below method is recommended
            val actionBar =
                (listener as AppCompatActivity?)?.supportActionBar   // refer to lecture 254 at 3:00 if needed
            actionBar?.setDisplayHomeAsUpEnabled(true)
            actionBar?.setHomeAsUpIndicator(R.drawable.home_as_up)
            actionBar?.title = "Add/Edit note"
        }

        CoroutineScope(IO).launch {
            addNote_save.setOnClickListener {
                CoroutineScope(IO).launch {
                    val sn = saveNote()
                    activity?.runOnUiThread {
                        if (sn) {
                            listener?.onSaveClicked()
                        } else {
                            progress_bar.visibility = View.GONE
                            Toast.makeText(context, "URL field empty", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }
        }

        addNote_addMore.setOnClickListener {
            CoroutineScope(IO).launch {
                saveNote()
                activity?.runOnUiThread {
                    progress_bar.visibility = View.GONE
                    requireArguments().putString(ARG_NOTE_URL, note?.url)
                    val newFragment = newInstance(null)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.note_details_container, newFragment)
                        .commit()

                    Toast.makeText(requireContext(), "Note saved! Adding more.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        addNote_share.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                val timestamp = toTimestamp(Integer.parseInt(getTimestamp()))
                putExtra(
                    Intent.EXTRA_TEXT,
                    urlWithTimestamp(
                        addNote_url.text.toString(),
                        getTimestamp()
                    ) + "\n\nat $timestamp" + "\n${addNote_note.text}"
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            if (addNote_url.text.toString().isNotEmpty()) {
                startActivity(shareIntent)
                listener?.onShareClicked()
            } else {
                Toast.makeText(context, "URL field is empty", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttach: starts")

        super.onAttach(context)
        if (context is OnSaveClicked) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach: starts")
        super.onDetach()
        if (mSpinner != null) {
            (mSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }
        listener = null
    }

    interface OnSaveClicked {
        fun onSaveClicked()
        fun onShareClicked()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param note the note to be edited, or null to add a new note.
         * @return A new instance of fragment AddEditNote.
         */
        @JvmStatic
        fun newInstance(note: Note?) =
            AddEditNote().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE, note)
                }
            }
    }

    private fun toHH(ms: String?): String {
        return TimeUnit.MILLISECONDS.toHours(ms?.toLong() ?: 0L).toString()
    }

    private fun toMM(ms: String?): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms?.toLong() ?: 0L)
        return (minutes % 60).toString()
    }

    private fun toSS(ms: String?): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms?.toLong() ?: 0L)
        return ((seconds % 3600) % 60).toString()
    }


    private fun getTimestamp(): String {
        Log.d(TAG, "getTimeStamp called")
        val hh: Int = addNote_hours.value

        val mm: Int = addNote_minutes.value
        val ss: Int = addNote_sec.value
        return ((hh * 3600 + mm * 60 + ss) * 1000).toString()
    }

}

@SuppressLint("SimpleDateFormat")
fun getDateTime(): Long {
    Log.d(TAG, "getDateTime called")
    val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss")
    return sdf.format(Date()).replace("[^\\d]".toRegex(), "").trim().toLong()
}

@RequiresApi(Build.VERSION_CODES.M)
fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
            return true
        }
    }
    return false
}

@RequiresApi(Build.VERSION_CODES.M)
suspend fun getTitle(youtubeUrl: String, context: Context): String {
    Log.d(TAG, "getTitle called")
    if (isOnline(context)) {
        for (i in 0..4) {
            try {
                val embeddedURL = URL(
                    "https://www.youtube.com/oembed?url=" +
                            youtubeUrl + "&format=json"
                )
                
                val title = CoroutineScope(IO).async {
                    JSONObject(IOUtils.toString(embeddedURL, "UTF-8")).getString("title")
                }.await()

                return title
            } catch (e: Exception) {
                Log.w(TAG, "getTitle: ${e.stackTraceToString()}")
                continue
            }
        }

    }
    return "No internet, check your internet connection"
}
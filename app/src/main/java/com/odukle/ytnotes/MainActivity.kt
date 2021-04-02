package com.odukle.ytnotes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlin.properties.Delegates

private const val TAG = "MainActivity"
const val SEND_INTENT_EXTRA = "youtubeUrl"
private const val SHOW_DIALOG = "showD"
private const val MY_PREFS = "myPrefs"
const val I_AD_COUNT = "iCount"
const val INTERSTITIAL_ID = "ca-app-pub-9193191601772541/3583713135"
var fa: MainActivity? = null

class MainActivity : AppCompatActivity(), AddEditNote.OnSaveClicked,
    MainActivityFragment.OnNoteEdit {

    private var mTwoPane = false
    lateinit var adRequest: AdRequest
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        lateinit var mInterstitialAd: InterstitialAd
        var adCount by Delegates.notNull<Int>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("pref", Context.MODE_PRIVATE)
        adCount = sharedPreferences.getInt(I_AD_COUNT, 0)

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = INTERSTITIAL_ID
        mInterstitialAd.loadAd(adRequest)
        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                mInterstitialAd.loadAd(adRequest)
            }
        }

        setSupportActionBar(toolbar)
        fa = this
        mTwoPane = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val fragment = supportFragmentManager.findFragmentById(R.id.note_details_container)
        if (fragment != null) {
            // There was an existing fragment to edit the task. Make sure the panes are set correctly
            showEditPane()
        } else {
            note_details_container.visibility = if (mTwoPane) View.INVISIBLE else View.GONE
            mainFragment.view?.visibility = View.VISIBLE
        }

        val receivedAction = intent?.action
        val receivedType = intent?.type
        if (receivedAction == Intent.ACTION_SEND) {
            if (receivedType != null) {
                if (receivedType.startsWith("text/")) {
                    noteEditRequest(null)
                }
            }
        }

        val sharedPref = getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE)
        val showD = sharedPref.getBoolean(SHOW_DIALOG, true)
        if (showD) {
            val dialog = MaterialAlertDialogBuilder(this)
            dialog.setTitle("What's New!")
                .setCancelable(false)
                .setMessage(
                    "- Now you can add multiple timestamps to the same video.\n\n" +
                            "- Just click on \"save and add more\" to add multiple notes for a single video"
                )
                .setPositiveButton(
                    "Got it!"
                ) { dialog, _ ->
                    dialog?.dismiss()
                    val editor = sharedPref.edit()
                    editor.putBoolean(SHOW_DIALOG, false)
                    editor.apply()
                }.show()
        }

    }

    private fun showEditPane() {
        note_details_container.visibility = View.VISIBLE
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)

        // Hide the left pane if in single pane view
        mainFragment.view?.visibility = if (mTwoPane) View.VISIBLE else View.GONE
    }

    private fun removeEditPane(fragment: Fragment? = null) {
        Log.d(TAG, "removeEditPane called")
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .setCustomAnimations(R.anim.slide_from_left, R.anim.slide_to_right)
                .commit()

            supportActionBar?.title = "Tube Notes"
        }

        // set the visibility of teh right hand pane
        note_details_container.visibility = if (mTwoPane) View.INVISIBLE else View.GONE
        // and show the left hand pane
        mainFragment.view?.visibility = View.VISIBLE
        note_list.adapter?.notifyDataSetChanged()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSaveClicked() {
        Log.d(TAG, "onSaveClicked called")
        if (isOnline(this)) {
            val fragment = supportFragmentManager.findFragmentById(R.id.note_details_container)
            removeEditPane(fragment)
        } else {
            Toast.makeText(
                this,
                "Can't connect, please check your internet connection",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onShareClicked() {
        Log.d(TAG, "onShareClicked called")
        val fragment = supportFragmentManager.findFragmentById(R.id.note_details_container)
        removeEditPane(fragment)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.menuMain_AddNote -> {
                noteEditRequest(null)
            }
//            R.id.menuMain_settings -> true
            android.R.id.home -> {
                Log.d(TAG, "onOptionsItemSelected: home button pressed")
                val fragment = supportFragmentManager.findFragmentById(R.id.note_details_container)
                removeEditPane(fragment)
            }
            R.id.user_info -> {
                startActivity(Intent(this, UserInfo::class.java))
                overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
            }
            R.id.about_app -> {
                startActivity(Intent(this, AboutApp::class.java))
                overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d(TAG, "onNewIntent Called")
        super.onNewIntent(intent)
        val receivedAction = intent?.action
        val receivedType = intent?.type
        if (receivedAction == Intent.ACTION_SEND) {
            if (receivedType != null) {
                if (receivedType.startsWith("text/")) {
                    getIntent().putExtra(
                        SEND_INTENT_EXTRA,
                        intent.getStringExtra(Intent.EXTRA_TEXT)
                    )
                    noteEditRequest(null)
                }
            }
        }
    }

    override fun onNoteEdit(note: Note) {
        noteEditRequest(note)
    }

    private fun noteEditRequest(note: Note?) {
        Log.d(TAG, "noteEditRequest: starts")
        // create a new fragment to edit the note
        val newFragment = AddEditNote.newInstance(note)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.note_details_container, newFragment)
            .setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left)
            .commitAllowingStateLoss()
        showEditPane()

        Log.d(TAG, "Exiting taskEditRequest")
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.note_details_container)
        if (fragment == null || mTwoPane) {
            super.onBackPressed()
        } else {
            removeEditPane(fragment)
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
package com.odukle.ytnotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_about_app.*


class AboutApp : AppCompatActivity() {

    private lateinit var adRequest: AdRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.home_as_up)
        supportActionBar?.title = "About this app"

        adRequest = AdRequest.Builder().build()
        adView_aa.loadAd(adRequest)

        privacy_policy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicy::class.java))
        }

        test_btn.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection(USERS).get().addOnSuccessListener {
                    it.documents.forEach { doc ->
                        doc.reference.collection(NOTES).get().addOnSuccessListener { collection ->
                            collection.documents.forEach { mDoc ->
                                var date = mDoc["dateAdded"]
                                if (date is String) {
                                    date = date.replace("[^\\d]".toRegex(), "").trim()
                                        .replace(" +".toRegex(), "")
                                    val dateLong = date.toLong()

                                    mDoc.reference.set(
                                        hashMapOf(
                                            "dateAddedLong" to dateLong
                                        ), SetOptions.merge()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
        }
        return true
    }

    fun openLink(view: View) {
        //Get url from tag
        val url = view.tag as String
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addCategory(Intent.CATEGORY_BROWSABLE)

        //pass the url to intent data
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    fun openEmail(view: View) {
        val id = view.tag as String

        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse("mailto:$id")
        startActivity(intent)
    }
}


package com.odukle.ytnotes

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

const val MY_REQUEST_CODE = 111
private const val TAG = "SignInActivity"

class SignInActivity : AppCompatActivity() {

    private val dbHelper = DatabaseHelper()
    private val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.PhoneBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build()
    )

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            dbHelper.open()
            dbHelper.notesRef().get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null) {
                        for (doc in task.result!!) {
                            doc.reference.get().addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val note = it.result?.toObject(Note::class.java)
                                    val ts = note?.timestamp ?: "0"
                                    val noteNote = listToString(note?.note)
                                    Log.d(TAG, "$ts : $noteNote")
                                    doc.reference.collection(MULTI_NOTES).document(ts).set(
                                        hashMapOf(
                                            ts to noteNote
                                        )
                                    )

                                }
                            }
                        }
                    }
                }
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            showSignInOptions()
        }

    }

    private fun showSignInOptions() {
        Log.d(TAG, "showSignInOptions called")
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setAlwaysShowSignInMethodScreen(true)
                .setLogo(R.drawable.tube_note_logo_small)
                .setIsSmartLockEnabled(false, true)
                .setTheme(R.style.AppTheme_NoActionBar)
                .build(),
            MY_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult called")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                val currentUser = FirebaseAuth.getInstance().currentUser!!
                Toast.makeText(
                    this,
                    "Signed in as ${currentUser.email ?: currentUser.phoneNumber ?: currentUser.displayName}",
                    Toast.LENGTH_SHORT
                ).show()

                dbHelper.open()
                dbHelper.addUser(
                    currentUser.uid,
                    currentUser.email ?: "sign in",
                    currentUser.phoneNumber ?: "sign in"
                )

                dbHelper.notesRef().get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (task.result != null) {
                            for (doc in task.result!!) {
                                doc.reference.get().addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        val note = it.result?.toObject(Note::class.java)
                                        val ts = note?.timestamp ?: "0"
                                        val noteNote = listToString(note?.note)
                                        Log.d(TAG, "$ts : $noteNote")
                                        doc.reference.collection(MULTI_NOTES).document(ts).set(
                                            hashMapOf(
                                                ts to noteNote
                                            )
                                        )
                                        Log.d(TAG, "added mNoteList")
                                    }
                                }
                            }
                        }
                    }
                    Log.d(TAG, "onActivityResult ends")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            } else {
                Toast.makeText(
                    this,
                    "Press back again to exit: ${response?.error?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}
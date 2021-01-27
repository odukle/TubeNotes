package com.odukle.ytnotes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_user_info.*


private const val TAG = "UserInfo"
class UserInfo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.home_as_up)
        supportActionBar?.title = "User info"

        val currentUser = FirebaseAuth.getInstance().currentUser
        Picasso.get().load(currentUser?.photoUrl)
            .error(R.drawable.ic_baseline_perm_identity_24)
            .placeholder(R.drawable.ic_baseline_perm_identity_24)
            .transform(object : CropCircleTransformation() {})
            .into(user_dp)

        user_name.text = currentUser?.displayName ?: currentUser?.phoneNumber
        user_email.text = currentUser?.email ?: ""

        btn_logout.setOnClickListener {
            Log.d(TAG, "UserInfo: logout clicked")
            val snackbar = Snackbar.make(user_info, "Confirm logout", Snackbar.LENGTH_LONG)
            snackbar.setAction("Yes") {
                AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener {
                        startActivity(Intent(this, SignInActivity::class.java))
                        finish()
                        fa?.finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }
            }.show()

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }
}
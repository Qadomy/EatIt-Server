package com.qadomy.eatitserver

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.model.ServerUserModel
import dmax.dialog.SpotsDialog
import java.util.*

class MainActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null
    private var listener: FirebaseAuth.AuthStateListener? = null
    private var dialog: AlertDialog? = null
    private var serverRef: DatabaseReference? = null
    private var provider: List<AuthUI.IdpConfig>? = null


    companion object {
        private val APP_REQUEST_CODE = 7171
    }

    // onStart, add firebase auth when start app
    override fun onStart() {
        super.onStart()
        firebaseAuth!!.addAuthStateListener(listener!!)
    }

    // onStop, remove firebase auth when exit from app
    override fun onStop() {
        firebaseAuth!!.removeAuthStateListener(listener!!)
        super.onStop()

    }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        init()
    }


    /**
     * init
     */
    private fun init() {
        provider = Arrays.asList(AuthUI.IdpConfig.PhoneBuilder().build())
        serverRef = FirebaseDatabase.getInstance().getReference(Common.SERVER_REF)

        firebaseAuth = FirebaseAuth.getInstance()

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        listener = object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                val user = firebaseAuth.currentUser
                if (user != null) {

                    checkServerUserFromFirebase(user)
                } else {
                    phoneLogin()
                }
            }
        }
    }

    /**
     *
     * Functions
     */

    // function for check if user have a permission from admin on server firebase
    private fun checkServerUserFromFirebase(user: FirebaseUser) {
        dialog!!.show()

        serverRef!!.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    dialog!!.dismiss()
                    Toast.makeText(this@MainActivity, "" + p0.message, Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {

                        val userModel = dataSnapshot.getValue(ServerUserModel::class.java)

                        if (userModel!!.isActive) {
                            // if is Activer true can go to Home Activity
                            goToHomeActivity(userModel)

                        } else {
                            // if is not Active "False", meaning can't access with Admin permission
                            dialog!!.dismiss()
                            Toast.makeText(
                                this@MainActivity,
                                "You must be allowed from Admin to access this app",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    } else {

                        dialog!!.dismiss()
                        showRegisterDialog(user)
                    }
                }
            })
    }

    // function send you to Home Activity
    private fun goToHomeActivity(userModel: ServerUserModel?) {
        dialog!!.dismiss()
        Common.currentServerUser = userModel

        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    // showing register dialog
    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Register")
        builder.setMessage("Please fill information \n Admin will accept your accout alte")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edtName = itemView.findViewById<View>(R.id.edt_name) as EditText
        val edtPhone = itemView.findViewById<View>(R.id.edt_phone) as EditText


        // set data
        edtPhone.setText(user.phoneNumber)

        builder.setNegativeButton("CANCEL") { dialgInterface, _ -> dialgInterface.dismiss() }
            .setPositiveButton("REGISTER") { _, _ ->
                if (TextUtils.isEmpty(edtName.text)) {
                    Toast.makeText(this@MainActivity, "Please enter your name", Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }

                val serverUserModel = ServerUserModel()
                serverUserModel.uid = user.uid
                serverUserModel.name = edtName.text.toString()
                serverUserModel.isActive =
                    false // Default fail, we must active user by manual on Firebase


                dialog!!.show()
                serverRef!!.child(serverUserModel.uid!!)
                    .setValue(serverUserModel)
                    .addOnFailureListener { e ->
                        // if happen an error
                        dialog!!.dismiss()
                        Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener { task ->

                        // if listerer complete
                        dialog!!.dismiss()
                        Toast.makeText(
                            this,
                            "Register success ! Admin will check and active user account",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

        builder.setView(itemView)

        val registerDialog = builder.create()
        registerDialog.show()
    }

    // make phone login
    private fun phoneLogin() {
        startActivityForResult(
            AuthUI
                .getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(provider!!)
                .build(), APP_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == APP_REQUEST_CODE) {


            if (resultCode == Activity.RESULT_OK) {

                val user = FirebaseAuth.getInstance().currentUser
            } else {

                Toast.makeText(this@MainActivity, "Failed to sign in", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

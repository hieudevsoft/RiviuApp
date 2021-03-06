package com.authencation.cloneriviu.networks

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.authencation.cloneriviu.R
import com.authencation.cloneriviu.model.BaseLogin
import com.authencation.cloneriviu.support.Constants
import com.authencation.cloneriviu.support.LoginResultClient
import com.authencation.cloneriviu.support.LogoutResultClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignInWithGoogle private constructor() : BaseLogin {
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var TAG = "SignInWithGoogle"
    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient
    private object Holder {
        val instance = SignInWithGoogle()
    }

    companion object {
        fun getInstance() = Holder.instance
    }

    fun createRequest(activity: Activity) {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun signIn(activity: Activity) {
        activity.startActivityForResult(
            Intent(googleSignInClient.signInIntent),
            Constants.LOGIN_GOOGLE
        )
    }




    override fun login(
        context: Activity,
        data: Intent?
    ): LiveData<LoginResultClient<Boolean>?> {
        val loginResult = MutableLiveData<LoginResultClient<Boolean>?>()
        val signInGoogleTask = GoogleSignIn.getSignedInAccountFromIntent(data)
        Log.d(TAG, "login: ${signInGoogleTask.result.toString()}")
        if (signInGoogleTask.isSuccessful) {
            val googleSingInAccount = signInGoogleTask.getResult(ApiException::class.java)
            googleSingInAccount.let {
                val authCredential =
                    GoogleAuthProvider.getCredential(googleSingInAccount?.idToken, null)
                firebaseAuth.signInWithCredential(authCredential)
                    .addOnCompleteListener(
                        context
                    ) { p0 ->
                        if (p0.isSuccessful) {
                        loginResult.value = LoginResultClient.Success(true)
                        } else {
                            loginResult.value = LoginResultClient.Error("Error",false)
                        }
                    }
            }
        }else{
            loginResult.value = LoginResultClient.Error("Error Stranger",false)
        }
        return loginResult
    }

    override fun logout(context: Activity): LiveData<LogoutResultClient<Boolean>?> {
        var logout = MutableLiveData<LogoutResultClient<Boolean>?>()
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Question ?")
        dialog.setMessage("Do you want Sign Out?")
        dialog.setPositiveButton(
            "Yes"
        ) { _, _ ->
            googleSignInClient =
                GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN)
            googleSignInClient.signOut().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirebaseAuth.getInstance().signOut()
                    logout.value = LogoutResultClient.Success(true)
                } else {
                    logout.value = LogoutResultClient.Error("Error Logout",false)
                }
            }
        }
        dialog.setNegativeButton("No") { _, _ ->logout?.value = LogoutResultClient.Error("Cancel Logout",false)
        }
        dialog.show()
        return logout
    }

    fun checkLoginWithGoogle(context: Context): Boolean =
        GoogleSignIn.getLastSignedInAccount(context) != null
}
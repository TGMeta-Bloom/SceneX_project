package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.repository.UserRepository
import com.example.scenex.utils.SessionManager
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Senior Technical Implementation: LoginActivity.
 * Requirement: Google/FB users are PERMANENT providers. No account deletion.
 * Flow: Social Auth -> Firestore Check -> Registration Pre-fill (if new).
 */
class LoginActivity : AppCompatActivity() {

    private val repository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SceneX_Auth"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnGoogleLogin = findViewById<Button>(R.id.btnGoogleLogin)
        val btnFacebookLogin = findViewById<Button>(R.id.btnFacebookLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvSignupLink = findViewById<TextView>(R.id.tvSignupLink)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!, progressBar)
                } catch (e: ApiException) {
                    progressBar.visibility = View.GONE
                }
            } else {
                progressBar.visibility = View.GONE
            }
        }

        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) { handleFacebookAccessToken(result.accessToken, progressBar) }
            override fun onCancel() { progressBar.visibility = View.GONE }
            override fun onError(error: FacebookException) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@LoginActivity, "Facebook Error", Toast.LENGTH_SHORT).show()
            }
        })

        btnGoogleLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        btnFacebookLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Credentials required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result -> performSecurityHandshake(result.user?.uid ?: "", progressBar) }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "Auth Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvSignupLink.setOnClickListener { startActivity(Intent(this, RoleSelectActivity::class.java)) }
    }

    private fun firebaseAuthWithGoogle(idToken: String, progressBar: ProgressBar) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result -> performSecurityHandshake(result.user?.uid ?: "", progressBar) }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Google Auth Error", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken, progressBar: ProgressBar) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result -> performSecurityHandshake(result.user?.uid ?: "", progressBar) }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Facebook Error", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performSecurityHandshake(userId: String, progressBar: ProgressBar) {
        repository.getUserRoutingData(userId) { role, status, vStatus, error ->
            progressBar.visibility = View.GONE
            findViewById<Button>(R.id.btnLogin).isEnabled = true

            if (error == null && status != null && role != null) {
                // Existing Returning User
                SessionManager.establishSession(this, userId, role, status)
                routeUser(role, status, vStatus)
            } else if (error == null) {
                //  CONVENIENCE PRE-FILL: Keep authenticated Social user
                val user = auth.currentUser
                Log.d(TAG, "New Social User detected. Redirecting to Role Selection with pre-fill.")
                val intent = Intent(this, RoleSelectActivity::class.java).apply {
                    putExtra("PREFILL_NAME", user?.displayName)
                    putExtra("PREFILL_EMAIL", user?.email)
                    putExtra("IS_SOCIAL_AUTH", true) // Signal to hide password fields
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Handshake Error", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
        }
    }

    private fun routeUser(role: String?, status: String?, vStatus: String?) {
        val normalizedRole = role?.uppercase()?.trim()
        val normalizedStatus = status?.lowercase()?.trim()
        val normalizedVStatus = vStatus?.lowercase()?.trim()

        val isApproved = (normalizedStatus == "verified" || normalizedStatus == "active" ||
                normalizedStatus == "available" || normalizedStatus == "unavailable" || normalizedStatus == "approved") &&
                (normalizedVStatus == "verified" || normalizedVStatus == "active")

        val intent = when {
            normalizedRole == "TALENT" || normalizedRole == "RECRUITER" -> {
                when {
                    isApproved -> Intent(this, MainActivity::class.java)
                    normalizedStatus == "pending_review" || normalizedVStatus == "pending" -> Intent(this, WaitingRoomActivity::class.java)
                    else -> Intent(this, SignupActivity::class.java).apply { putExtra("USER_ROLE", normalizedRole) }
                }
            }
            else -> Intent(this, RoleSelectActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}

package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "GoogleAuthHelper"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

sealed interface GoogleAuthResult {
    data class Success(val user: FirebaseUser) : GoogleAuthResult
    data object Cancelled : GoogleAuthResult
    data object NoSavedCredentials : GoogleAuthResult
    data class Error(val message: String) : GoogleAuthResult
}

class GoogleAuthHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    private fun webClientId(): String = context.getString(R.string.default_web_client_id)

    suspend fun attemptAutoSignIn(activityContext: Context): GoogleAuthResult {
        if (Firebase.auth.currentUser != null) {
            return GoogleAuthResult.Success(Firebase.auth.currentUser!!)
        }
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId())
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            exchangeGoogleCredential(result.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            GoogleAuthResult.NoSavedCredentials
        } catch (_: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: Exception) {
            Log.w(TAG, "Auto sign-in skipped: ${e.message}")
            GoogleAuthResult.NoSavedCredentials
        }
    }

    suspend fun signInWithGoogleInteractive(activityContext: Context): GoogleAuthResult {
        return try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId())
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            exchangeGoogleCredential(result.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (_: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available in Credential Manager", e)
            GoogleAuthResult.Error(
                "No Google account found on this device. Please add a Google account in device Settings and try again."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            GoogleAuthResult.Error(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
        }
    }

    private suspend fun exchangeGoogleCredential(credential: androidx.credentials.Credential): GoogleAuthResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return GoogleAuthResult.Error("Firebase sign-in returned no user.")
            return GoogleAuthResult.Success(user)
        }
        return GoogleAuthResult.Error("Unexpected credential type received from Google Sign-In.")
    }

    suspend fun signOut() {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "clearCredentialState warning: ${e.message}")
            }
            Firebase.auth.signOut()
        }
    }
}

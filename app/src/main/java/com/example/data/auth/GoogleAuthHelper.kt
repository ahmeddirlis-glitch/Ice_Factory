package com.example.data.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.util.RuntimeDiagnosticLogger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest

private const val TAG = "GoogleAuthHelper"
private const val FIREBASE_SIGN_IN_TIMEOUT_MS = 25_000L

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

sealed interface GoogleAuthResult {
    data class Success(val user: FirebaseUser) : GoogleAuthResult
    data class Cancelled(val reason: String? = null) : GoogleAuthResult
    data object NoSavedCredentials : GoogleAuthResult
    data class Error(val message: String) : GoogleAuthResult
}

internal fun classifyGoogleAuthErrorMessage(
    rawMessage: String?,
    isOnline: Boolean,
    appSha1: String? = null
): String {
    if (!isOnline) {
        return "Internet connection required for Google Sign-In or Registration.\nانٹرنیٹ کنکشن درکار ہے، براہ کرم نیٹ آن کر کے دوبارہ کوشش کریں۔"
    }
    val msg = rawMessage.orEmpty().trim()
    val lower = msg.lowercase()
    return when {
        lower.contains("developer_error") ||
            lower.contains("28444") ||
            lower.contains("status code: 10") ||
            Regex("""\b10:\s""").containsMatchIn(lower) ||
            lower.contains("cannot find a matching credential") ||
            lower.contains("reauth") && lower.contains("sha") -> {
            val shaNote = if (!appSha1.isNullOrBlank()) "\nAPK SHA-1: $appSha1" else ""
            "Google Sign-In app signature mismatch (OAuth/SHA-1 error). Make sure this APK's SHA-1 is added in Firebase Console & google-services.json.$shaNote"
        }
        lower.contains("network") || lower.contains("timeout") || lower.contains("unable to resolve host") -> {
            "Network error while contacting Google / Firebase. Check your internet connection and tap Continue with Google again."
        }
        lower.contains("play services") || lower.contains("provider configuration") || lower.contains("credentials_api") -> {
            "Google Play Services Credential Manager is unavailable or outdated on this device. Please update Google Play Services and try again."
        }
        msg.isNotBlank() -> msg
        else -> "Google Sign-In failed. Please try again."
    }
}

class GoogleAuthHelper(private val context: Context) {

    private fun webClientId(): String {
        val id = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull().orEmpty().trim()
        if (id.isBlank()) {
            throw IllegalStateException("Google Sign-In configuration missing (default_web_client_id).")
        }
        return id
    }

    @Suppress("DEPRECATION")
    private fun getAppSigningSha1(): String? = runCatching {
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }
        val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.signingInfo?.apkContentsSigners?.firstOrNull()
                ?: pkgInfo.signingInfo?.signingCertificateHistory?.firstOrNull()
        } else {
            pkgInfo.signatures?.firstOrNull()
        } ?: return@runCatching null

        val digest = MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
        digest.joinToString(":") { "%02X".format(it) }
    }.getOrNull()

    suspend fun attemptAutoSignIn(activityContext: Context): GoogleAuthResult {
        if (Firebase.auth.currentUser != null) {
            return GoogleAuthResult.Success(Firebase.auth.currentUser!!)
        }
        return try {
            val credManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId())
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credManager.getCredential(
                request = request,
                context = activityContext
            )
            exchangeGoogleCredential(result.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            GoogleAuthResult.NoSavedCredentials
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Auto sign-in cancelled: ${e.message}")
            GoogleAuthResult.Cancelled(e.errorMessage?.toString() ?: e.message)
        } catch (e: Exception) {
            Log.w(TAG, "Auto sign-in skipped: ${e.message}")
            GoogleAuthResult.NoSavedCredentials
        }
    }

    suspend fun signInWithGoogleInteractive(
        activityContext: Context,
        isOnline: Boolean = true
    ): GoogleAuthResult {
        val clientId = try {
            webClientId()
        } catch (e: Exception) {
            Log.e(TAG, "Missing default_web_client_id", e)
            RuntimeDiagnosticLogger.recordError("GoogleAuth", e.message ?: "Missing default_web_client_id")
            return GoogleAuthResult.Error(e.message ?: "Google Sign-In configuration missing.")
        }

        val credManager = CredentialManager.create(activityContext)
        val appSha1 = getAppSigningSha1()

        // 1. Primary Interactive Sign-In using GetSignInWithGoogleOption (1 option per request)
        val primaryOutcome = try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(clientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credManager.getCredential(
                request = request,
                context = activityContext
            )
            return exchangeGoogleCredential(result.credential, isOnline = isOnline, appSha1 = appSha1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            val detail = e.errorMessage?.toString() ?: e.message
            Log.w(TAG, "Google Sign-In cancelled or dismissed: type=${e.type}, msg=$detail", e)
            RuntimeDiagnosticLogger.recordWarning("GoogleAuth", "Cancelled: ${detail ?: "dismissed"}")
            return GoogleAuthResult.Cancelled(reason = detail)
        } catch (e: NoCredentialException) {
            e
        } catch (e: GetCredentialProviderConfigurationException) {
            e
        } catch (e: GetCredentialUnsupportedException) {
            e
        } catch (e: GetCredentialInterruptedException) {
            e
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Primary Google Sign-In failed: type=${e.type}, msg=${e.errorMessage ?: e.message}", e)
            e
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed unexpectedly", e)
            val friendly = classifyGoogleAuthErrorMessage(e.localizedMessage ?: e.message, isOnline, appSha1)
            RuntimeDiagnosticLogger.recordError("GoogleAuth", friendly)
            return GoogleAuthResult.Error(friendly)
        }

        // 2. Fallback Interactive Request using GetGoogleIdOption in a SEPARATE single-option request
        // (protects against OEM Play Services issues with GetSignInWithGoogleOption)
        return try {
            Log.w(TAG, "Retrying Google Sign-In with GetGoogleIdOption fallback after: ${primaryOutcome.type} / ${primaryOutcome.message}")
            val fallbackOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackOption)
                .build()

            val fallbackResult = credManager.getCredential(
                request = fallbackRequest,
                context = activityContext
            )
            exchangeGoogleCredential(fallbackResult.credential, isOnline = isOnline, appSha1 = appSha1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            val detail = e.errorMessage?.toString() ?: e.message
            Log.w(TAG, "Fallback Google Sign-In cancelled: type=${e.type}, msg=$detail", e)
            RuntimeDiagnosticLogger.recordWarning("GoogleAuth", "Fallback cancelled: ${detail ?: "dismissed"}")
            GoogleAuthResult.Cancelled(reason = detail)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available in Credential Manager", e)
            val msg = if (!isOnline) {
                classifyGoogleAuthErrorMessage(e.message, isOnline = false, appSha1 = appSha1)
            } else {
                "No Google account is signed in or available on this phone. Please add a Google account in Settings -> Accounts and try again."
            }
            RuntimeDiagnosticLogger.recordError("GoogleAuth", msg)
            GoogleAuthResult.NoSavedCredentials
        } catch (e: GetCredentialException) {
            val raw = e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: e.message ?: primaryOutcome.message
            val friendly = classifyGoogleAuthErrorMessage(raw, isOnline, appSha1)
            Log.e(TAG, "Fallback Google Sign-In failed: type=${e.type}, msg=$raw", e)
            RuntimeDiagnosticLogger.recordError("GoogleAuth", friendly)
            GoogleAuthResult.Error(friendly)
        } catch (e: Exception) {
            val raw = e.localizedMessage ?: e.message ?: primaryOutcome.message
            val friendly = classifyGoogleAuthErrorMessage(raw, isOnline, appSha1)
            Log.e(TAG, "Fallback Google Sign-In failed", e)
            RuntimeDiagnosticLogger.recordError("GoogleAuth", friendly)
            GoogleAuthResult.Error(friendly)
        }
    }

    private suspend fun exchangeGoogleCredential(
        credential: Credential,
        isOnline: Boolean = true,
        appSha1: String? = null
    ): GoogleAuthResult = withContext(NonCancellable) {
        try {
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = withTimeoutOrNull(FIREBASE_SIGN_IN_TIMEOUT_MS) {
                    Firebase.auth.signInWithCredential(firebaseCredential).await()
                } ?: run {
                    val timeoutMsg = "Firebase authentication timed out. Please check your internet connection and tap Continue with Google again."
                    RuntimeDiagnosticLogger.recordError("GoogleAuth", timeoutMsg)
                    return@withContext GoogleAuthResult.Error(timeoutMsg)
                }
                val user = authResult.user
                    ?: Firebase.auth.currentUser
                    ?: return@withContext GoogleAuthResult.Error("Firebase sign-in returned no user.")
                return@withContext GoogleAuthResult.Success(user)
            }
            val unexpectedMsg = "Unexpected credential type received from Google Sign-In (${credential.type})."
            RuntimeDiagnosticLogger.recordError("GoogleAuth", unexpectedMsg)
            GoogleAuthResult.Error(unexpectedMsg)
        } catch (e: FirebaseNetworkException) {
            val msg = classifyGoogleAuthErrorMessage(e.message ?: "Network error", isOnline = false, appSha1 = appSha1)
            RuntimeDiagnosticLogger.recordError("GoogleAuth", msg)
            GoogleAuthResult.Error(msg)
        } catch (e: FirebaseAuthException) {
            val msg = classifyGoogleAuthErrorMessage("${e.errorCode}: ${e.localizedMessage ?: e.message}", isOnline, appSha1)
            Log.e(TAG, "FirebaseAuthException during Google exchange: ${e.errorCode}", e)
            RuntimeDiagnosticLogger.recordError("GoogleAuth", msg)
            GoogleAuthResult.Error(msg)
        } catch (e: Exception) {
            val msg = classifyGoogleAuthErrorMessage(e.localizedMessage ?: e.message, isOnline, appSha1)
            Log.e(TAG, "Failed exchanging Google ID token with Firebase", e)
            RuntimeDiagnosticLogger.recordError("GoogleAuth", msg)
            GoogleAuthResult.Error(msg)
        }
    }

    suspend fun signOut() {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "clearCredentialState warning: ${e.message}")
            }
            Firebase.auth.signOut()
        }
    }
}

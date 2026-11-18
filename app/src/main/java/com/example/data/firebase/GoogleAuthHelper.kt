package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class AuthDiagnosticInfo(
    val packageName: String,
    val runtimeSha1Colon: String?,
    val expectedSha1Colon: String,
    val isSha1Matched: Boolean,
    val hasWebClientId: Boolean,
)

object GoogleAuthHelper {

    private const val TAG = "Auth"
    const val EXPECTED_DEBUG_SHA1_COLON =
        "F7:F8:E1:70:B9:E3:2F:91:80:38:74:0A:E1:44:0F:4B:9D:51:8D:A1"
    private const val INSTANT_CANCEL_THRESHOLD_MS = 1200L

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun getAuthDiagnosticInfo(context: Context): AuthDiagnosticInfo {
        val runtimeSha1 = getSigningCertSha1Colon(context)
        val webClientId = runCatching {
            context.getString(R.string.default_web_client_id).trim()
        }.getOrDefault("")
        return AuthDiagnosticInfo(
            packageName = context.packageName,
            runtimeSha1Colon = runtimeSha1,
            expectedSha1Colon = EXPECTED_DEBUG_SHA1_COLON,
            isSha1Matched = runtimeSha1 != null &&
                runtimeSha1.equals(EXPECTED_DEBUG_SHA1_COLON, ignoreCase = true),
            hasWebClientId = webClientId.isNotBlank(),
        )
    }

    suspend fun signInWithGoogle(
        activityContext: Context,
        autoSelectSilentOnly: Boolean = false,
    ): Result<FirebaseUser> {
        val activity = activityContext.findActivity()
            ?: return Result.failure(
                IllegalStateException(
                    "Real Reason: ActivityContextMissing — Google Sign-In ko active screen (Activity) chahiye."
                )
            )

        val webClientId = runCatching { activity.getString(R.string.default_web_client_id).trim() }
            .getOrDefault("")
        if (webClientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Real Reason: Missing default_web_client_id in app/google-services.json. " +
                        "Fix: Re-push valid app/google-services.json or update GOOGLE_SERVICES_JSON in GitHub Secrets."
                )
            )
        }

        val credentialManager = CredentialManager.create(activity)

        if (autoSelectSilentOnly) {
            val request = buildGoogleIdRequest(
                webClientId = webClientId,
                filterByAuthorizedAccounts = true,
                autoSelectEnabled = true,
            )
            return runCredentialRequest(activity, credentialManager, request)
                .mapCatching { credential -> completeFirebaseAuth(credential) }
        }

        val startMs = System.currentTimeMillis()
        val primaryRequest = buildSignInWithGoogleRequest(webClientId)
        val primaryResult = runCredentialRequest(activity, credentialManager, primaryRequest)

        primaryResult.onSuccess { credential ->
            return runCatching { completeFirebaseAuth(credential) }.fold(
                onSuccess = { Result.success(it) },
                onFailure = { err ->
                    if (err is CancellationException) throw err
                    Result.failure(buildDiagnosticException(activity, err))
                }
            )
        }

        val primaryError = primaryResult.exceptionOrNull()
        if (primaryError is CancellationException) throw primaryError

        val elapsedMs = System.currentTimeMillis() - startMs
        val shouldTryAccountPickerFallback = when (primaryError) {
            is NoCredentialException -> true
            is GetCredentialCancellationException -> elapsedMs < INSTANT_CANCEL_THRESHOLD_MS
            is GetCredentialException -> true
            else -> false
        }

        if (shouldTryAccountPickerFallback) {
            Log.w(
                TAG,
                "Primary Google Sign-In failed in ${elapsedMs}ms (${primaryError?.javaClass?.simpleName}: ${primaryError?.message}). Retrying with GetGoogleIdOption chooser."
            )
            runCatching {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
            val fallbackRequest = buildGoogleIdRequest(
                webClientId = webClientId,
                filterByAuthorizedAccounts = false,
                autoSelectEnabled = false,
            )
            val fallbackResult = runCredentialRequest(activity, credentialManager, fallbackRequest)
            fallbackResult.onSuccess { credential ->
                return runCatching { completeFirebaseAuth(credential) }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { err ->
                        if (err is CancellationException) throw err
                        Result.failure(buildDiagnosticException(activity, err))
                    }
                )
            }
            val fallbackError = fallbackResult.exceptionOrNull()
            if (fallbackError is CancellationException) throw fallbackError
            return Result.failure(buildDiagnosticException(activity, fallbackError ?: primaryError, elapsedMs))
        }

        return Result.failure(buildDiagnosticException(activity, primaryError, elapsedMs))
    }

    private fun buildSignInWithGoogleRequest(webClientId: String): GetCredentialRequest {
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
    }

    private fun buildGoogleIdRequest(
        webClientId: String,
        filterByAuthorizedAccounts: Boolean,
        autoSelectEnabled: Boolean,
    ): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(autoSelectEnabled)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    private suspend fun runCredentialRequest(
        activity: Activity,
        credentialManager: CredentialManager,
        request: GetCredentialRequest,
    ): Result<Credential> {
        var hadFlagSecure = false
        return try {
            withContext(Dispatchers.Main.immediate) {
                val flags = activity.window?.attributes?.flags ?: 0
                hadFlagSecure = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                if (hadFlagSecure) {
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            val response = credentialManager.getCredential(
                request = request,
                context = activity,
            )
            Result.success(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential found on device: ${e.message}", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In credential error (${e.type}): ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            if (hadFlagSecure) {
                withContext(Dispatchers.Main.immediate) {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }

    private suspend fun completeFirebaseAuth(credential: Credential): FirebaseUser {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null,
            )
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            return authResult.user ?: error("Google sign-in completed without user record")
        }
        error("Unexpected credential type received: ${credential.type}")
    }

    private fun buildDiagnosticException(
        context: Context,
        error: Throwable?,
        elapsedMs: Long = -1L,
    ): Throwable {
        val diag = getAuthDiagnosticInfo(context)
        val shaSummary = diag.runtimeSha1Colon ?: "Unknown"
        val rawMsg = error?.localizedMessage ?: error?.message ?: "Unknown error"

        if (!diag.isSha1Matched && diag.runtimeSha1Colon != null) {
            return IllegalStateException(
                "Real Reason: APK Signing SHA-1 Mismatch!\n" +
                    "• Built APK SHA-1: $shaSummary\n" +
                    "• Registered Firebase SHA-1: ${diag.expectedSha1Colon}\n" +
                    "• Package: ${diag.packageName}\n" +
                    "Fix: Push debug.keystore.base64 & app/google-services.json to GitHub and reinstall the new APK.",
                error
            )
        }

        val detail = when (error) {
            is NoCredentialException ->
                "Real Reason: NoCredentialException ($rawMsg)\n" +
                    "• Play Services ko device par active Google/Gmail account nahi mila.\n" +
                    "• Package: ${diag.packageName} | SHA-1: Matched ✅\n" +
                    "Fix: Phone Settings -> Google / Play Store me apna Gmail sign in karein."

            is GetCredentialCancellationException -> {
                val timingNote = if (elapsedMs in 0 until INSTANT_CANCEL_THRESHOLD_MS) {
                    "Popup ${elapsedMs}ms me Play Services ne auto-close kiya (aksar mismatched GOOGLE_SERVICES_JSON secret ya device Google Credential Provider off hone par hota hai)."
                } else {
                    "Sign-in sheet user ya system dwara dismiss hui."
                }
                "Real Reason: GetCredentialCancellationException ($rawMsg)\n" +
                    "• $timingNote\n" +
                    "• Package: ${diag.packageName} | SHA-1: Matched ✅\n" +
                    "Check: 1) Play Store me Gmail logged in hai, 2) Settings -> Passwords & accounts -> Google Credential Manager ON hai, 3) GitHub Secret GOOGLE_SERVICES_JSON purana to nahi hai."
            }

            is FirebaseAuthException ->
                "Real Reason: FirebaseAuthException [${error.errorCode}] — $rawMsg\n" +
                    "• Package: ${diag.packageName} | SHA-1: $shaSummary"

            is GetCredentialException ->
                "Real Reason: ${error.type} — $rawMsg\n" +
                    "• Package: ${diag.packageName} | SHA-1: $shaSummary\n" +
                    "Check: Google Play Services update karein aur internet connection verify karein."

            else ->
                "Real Reason: ${error?.javaClass?.simpleName ?: "AuthError"} — $rawMsg\n" +
                    "• Package: ${diag.packageName} | SHA-1: $shaSummary"
        }

        return IllegalStateException(detail, error)
    }

    fun getSigningCertSha1Colon(context: Context): String? {
        return runCatching {
            val pm = context.packageManager
            val packageName = context.packageName
            val rawBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                val cert = signingInfo?.apkContentsSigners?.firstOrNull()
                    ?: signingInfo?.signingCertificateHistory?.firstOrNull()
                cert?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }
            val bytes = rawBytes ?: return null
            val digest = MessageDigest.getInstance("SHA-1").digest(bytes)
            digest.joinToString(":") { b -> "%02X".format(b) }
        }.getOrNull()
    }

    suspend fun signOut(context: Context) {
        runCatching {
            Firebase.auth.signOut()
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

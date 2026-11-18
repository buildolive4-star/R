package com.example.data.firebase

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {

    suspend fun signInWithGoogle(
        activityContext: Context,
        autoSelectSilentOnly: Boolean = false,
    ): Result<FirebaseUser> {
        return try {
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(activityContext)

            val request = if (autoSelectSilentOnly) {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()
                GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
            } else {
                val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()
                GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
            }

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext,
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleIdTokenCredential.idToken,
                    null,
                )
                val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: error("Google sign-in completed without user record")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(context: Context) {
        runCatching {
            Firebase.auth.signOut()
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

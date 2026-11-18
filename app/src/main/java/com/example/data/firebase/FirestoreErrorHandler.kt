package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?,
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providersArray = JSONArray().apply {
        currentUser?.providerData?.forEach { info ->
            put(JSONObject().apply {
                put("providerId", info.providerId)
                put("email", info.email ?: JSONObject.NULL)
            })
        }
    }

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid ?: JSONObject.NULL)
        put("email", currentUser?.email ?: JSONObject.NULL)
        put("emailVerified", currentUser?.isEmailVerified ?: false)
        put("providerInfo", providersArray)
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

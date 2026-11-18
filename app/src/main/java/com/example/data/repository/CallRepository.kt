package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.IceCandidateRecord
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CallRepository(
    private val db: FirebaseFirestore,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(): String =
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google first.")

    suspend fun createCallSession(
        callId: String,
        callerName: String,
        callerRole: String, // "client" or "admin"
        calleeUid: String,
        calleeName: String,
        chatId: String,
        profileId: String,
        profileName: String,
        offerSdp: String,
    ): Result<CallSession> {
        val path = "$COLLECTION_CALLS/$callId"
        return try {
            val uid = requireSignedInUid()
            val cleanRole = if (callerRole == "admin") "admin" else "client"
            val payload = hashMapOf<String, Any>(
                "callerUid" to uid,
                "callerName" to callerName.trim().ifBlank { "JustUs User" }.take(80),
                "callerRole" to cleanRole,
                "calleeUid" to calleeUid.trim().take(128),
                "calleeName" to calleeName.trim().ifBlank { "JustUs" }.take(80),
                "chatId" to chatId.trim().take(128),
                "profileId" to profileId.trim().take(128),
                "profileName" to profileName.trim().take(80),
                "status" to CallStatus.RINGING,
                "offerSdp" to offerSdp.take(19500),
                "answerSdp" to "",
                "answeredByUid" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.document(path).set(payload).await()
            val saved = db.document(path).get().await().toObject(
                CallSession::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CallSession(
                id = callId,
                callerUid = uid,
                callerName = callerName,
                callerRole = cleanRole,
                calleeUid = calleeUid,
                calleeName = calleeName,
                chatId = chatId,
                profileId = profileId,
                profileName = profileName,
                status = CallStatus.RINGING,
                offerSdp = offerSdp,
            )
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptCallSession(
        callId: String,
        answerSdp: String,
    ): Result<Unit> {
        val path = "$COLLECTION_CALLS/$callId"
        return try {
            val uid = requireSignedInUid()
            db.document(path).update(
                mapOf(
                    "status" to CallStatus.ACCEPTED,
                    "answerSdp" to answerSdp.take(19500),
                    "answeredByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCallStatus(
        callId: String,
        status: String,
    ): Result<Unit> {
        val path = "$COLLECTION_CALLS/$callId"
        return try {
            requireSignedInUid()
            db.document(path).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addIceCandidate(
        callId: String,
        isOfferSide: Boolean,
        sdpMid: String,
        sdpMLineIndex: Int,
        candidate: String,
    ): Result<Unit> {
        val sub = if (isOfferSide) SUB_OFFER_CANDIDATES else SUB_ANSWER_CANDIDATES
        val docRef = db.collection(COLLECTION_CALLS).document(callId).collection(sub).document()
        return try {
            val uid = requireSignedInUid()
            docRef.set(
                mapOf(
                    "senderUid" to uid,
                    "sdpMid" to sdpMid.take(64),
                    "sdpMLineIndex" to sdpMLineIndex.coerceAtLeast(0),
                    "candidate" to candidate.take(2000),
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCallSession(callId: String): Flow<CallSession?> {
        if (callId.isBlank()) return flowOf(null)
        val docRef = db.collection(COLLECTION_CALLS).document(callId)
        return docRef.snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.GET, docRef.path)
                }
                emit(null)
            }
    }

    fun observeIceCandidates(callId: String, fromOfferSide: Boolean): Flow<List<IceCandidateRecord>> {
        if (callId.isBlank()) return flowOf(emptyList())
        val sub = if (fromOfferSide) SUB_OFFER_CANDIDATES else SUB_ANSWER_CANDIDATES
        val colRef = db.collection(COLLECTION_CALLS).document(callId).collection(sub)
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    IceCandidateRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
    }

    fun observeIncomingCallsForUser(uid: String): Flow<List<CallSession>> {
        if (uid.isBlank()) return flowOf(emptyList())
        return db.collection(COLLECTION_CALLS)
            .whereEqualTo("calleeUid", uid)
            .whereEqualTo("status", CallStatus.RINGING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
    }

    fun observeIncomingCallsForAdmin(): Flow<List<CallSession>> {
        return db.collection(COLLECTION_CALLS)
            .whereEqualTo("calleeUid", CallStatus.ADMIN_CALLEE_UID)
            .whereEqualTo("status", CallStatus.RINGING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
    }

    companion object {
        const val COLLECTION_CALLS = "call_sessions"
        const val SUB_OFFER_CANDIDATES = "offer_candidates"
        const val SUB_ANSWER_CANDIDATES = "answer_candidates"
    }
}

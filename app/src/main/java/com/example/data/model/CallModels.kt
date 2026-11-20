package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object CallStatus {
    const val RINGING = "ringing"
    const val ACCEPTED = "accepted"
    const val ENDED = "ended"
    const val DECLINED = "declined"
    const val MISSED = "missed"

    const val ADMIN_CALLEE_UID = "__ADMIN__"
}

object CallType {
    const val AUDIO = "audio"
    const val VIDEO = "video"
}

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerUid: String = "",
    val callerName: String = "",
    val callerRole: String = "client", // "client", "staff", or "admin"
    val calleeUid: String = "",
    val calleeName: String = "",
    val chatId: String = "",
    val profileId: String = "",
    val profileName: String = "",
    val callType: String = CallType.AUDIO,
    val ratePerMinInr: Int = 0,
    val status: String = CallStatus.RINGING,
    val offerSdp: String = "",
    val answerSdp: String = "",
    val answeredByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val isVideoCall: Boolean
        get() = callType.equals(CallType.VIDEO, ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class IceCandidateRecord(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val candidate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

sealed interface ActiveCallUi {
    data object Idle : ActiveCallUi
    data class OutgoingRinging(val session: CallSession) : ActiveCallUi
    data class IncomingRinging(val session: CallSession) : ActiveCallUi
    data class Connecting(val session: CallSession, val isCaller: Boolean) : ActiveCallUi
    data class Connected(
        val session: CallSession,
        val connectedAtEpochMs: Long,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false,
        val isCameraOff: Boolean = false,
        val isFrontCamera: Boolean = true,
    ) : ActiveCallUi
}

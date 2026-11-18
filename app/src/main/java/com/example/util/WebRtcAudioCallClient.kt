package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Clean WebRTC PeerConnection + AudioManager wrapper powered by io.getstream:stream-webrtc-android.
 */
class WebRtcAudioCallClient(
    context: Context,
    private val onLocalIceCandidate: (IceCandidate) -> Unit,
    private val onConnectionStateChanged: (PeerConnection.PeerConnectionState) -> Unit,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var audioFocusRequest: AudioFocusRequest? = null

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null

    private fun ensureInitialized() {
        if (factory != null && peerConnection != null) return

        synchronized(Companion) {
            if (!isFactoryInitialized) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                isFactoryInitialized = true
            }
        }

        val adm = JavaAudioDeviceModule.builder(appContext)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
        audioDeviceModule = adm

        val createdFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)
            .createPeerConnectionFactory()
        factory = createdFactory

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val pc = createdFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) = Unit
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit
                override fun onIceCandidate(candidate: IceCandidate?) {
                    if (candidate != null) {
                        onLocalIceCandidate(candidate)
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
                override fun onAddStream(stream: MediaStream?) = Unit
                override fun onRemoveStream(stream: MediaStream?) = Unit
                override fun onDataChannel(dc: DataChannel?) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) = Unit
                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    if (newState != null) {
                        onConnectionStateChanged(newState)
                    }
                }
            }
        ) ?: error("Unable to create WebRTC PeerConnection")
        peerConnection = pc

        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        }
        val source = createdFactory.createAudioSource(audioConstraints)
        audioSource = source
        val track = createdFactory.createAudioTrack("justus_audio_track", source).apply {
            setEnabled(true)
        }
        localAudioTrack = track
        pc.addTrack(track, listOf("justus_audio_stream"))

        configureVoiceAudioMode(speakerOn = false)
    }

    suspend fun createOfferSdp(): String {
        ensureInitialized()
        val pc = peerConnection ?: error("PeerConnection unavailable")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        val offer = createOfferAsync(pc, constraints)
        setLocalDescriptionAsync(pc, offer)
        return offer.description
    }

    suspend fun acceptOfferAndCreateAnswerSdp(remoteOfferSdp: String): String {
        ensureInitialized()
        val pc = peerConnection ?: error("PeerConnection unavailable")
        val remoteOffer = SessionDescription(SessionDescription.Type.OFFER, remoteOfferSdp)
        setRemoteDescriptionAsync(pc, remoteOffer)

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        val answer = createAnswerAsync(pc, constraints)
        setLocalDescriptionAsync(pc, answer)
        return answer.description
    }

    suspend fun applyRemoteAnswerSdp(remoteAnswerSdp: String) {
        val pc = peerConnection ?: return
        val remoteAnswer = SessionDescription(SessionDescription.Type.ANSWER, remoteAnswerSdp)
        setRemoteDescriptionAsync(pc, remoteAnswer)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidateSdp: String) {
        val pc = peerConnection ?: return
        runCatching {
            pc.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidateSdp))
        }.onFailure { err ->
            Log.w(TAG, "Failed adding ICE candidate", err)
        }
    }

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
        audioManager.isMicrophoneMute = muted
    }

    fun setSpeakerphone(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
    }

    private fun configureVoiceAudioMode(speakerOn: Boolean) {
        runCatching {
            previousAudioMode = audioManager.mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = speakerOn
            audioManager.isMicrophoneMute = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioFocusRequest = focus
                audioManager.requestAudioFocus(focus)
            }
        }
    }

    fun release() {
        runCatching {
            audioManager.isMicrophoneMute = false
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = previousAudioMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            }
        }
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { audioDeviceModule?.release() }
        runCatching { factory?.dispose() }
        localAudioTrack = null
        audioSource = null
        peerConnection = null
        audioDeviceModule = null
        factory = null
    }

    private suspend fun createOfferAsync(pc: PeerConnection, constraints: MediaConstraints): SessionDescription {
        val deferred = CompletableDeferred<SessionDescription>()
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp != null) deferred.complete(sdp)
                else deferred.completeExceptionally(IllegalStateException("Null offer SDP"))
            }

            override fun onCreateFailure(err: String?) {
                deferred.completeExceptionally(IllegalStateException(err ?: "Offer creation failed"))
            }

            override fun onSetSuccess() = Unit
            override fun onSetFailure(err: String?) = Unit
        }, constraints)
        return deferred.await()
    }

    private suspend fun createAnswerAsync(pc: PeerConnection, constraints: MediaConstraints): SessionDescription {
        val deferred = CompletableDeferred<SessionDescription>()
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp != null) deferred.complete(sdp)
                else deferred.completeExceptionally(IllegalStateException("Null answer SDP"))
            }

            override fun onCreateFailure(err: String?) {
                deferred.completeExceptionally(IllegalStateException(err ?: "Answer creation failed"))
            }

            override fun onSetSuccess() = Unit
            override fun onSetFailure(err: String?) = Unit
        }, constraints)
        return deferred.await()
    }

    private suspend fun setLocalDescriptionAsync(pc: PeerConnection, sdp: SessionDescription) {
        val deferred = CompletableDeferred<Unit>()
        pc.setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) = Unit
            override fun onCreateFailure(p0: String?) = Unit
            override fun onSetSuccess() {
                deferred.complete(Unit)
            }

            override fun onSetFailure(err: String?) {
                deferred.completeExceptionally(IllegalStateException(err ?: "setLocalDescription failed"))
            }
        }, sdp)
        deferred.await()
    }

    private suspend fun setRemoteDescriptionAsync(pc: PeerConnection, sdp: SessionDescription) {
        val deferred = CompletableDeferred<Unit>()
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) = Unit
            override fun onCreateFailure(p0: String?) = Unit
            override fun onSetSuccess() {
                deferred.complete(Unit)
            }

            override fun onSetFailure(err: String?) {
                deferred.completeExceptionally(IllegalStateException(err ?: "setRemoteDescription failed"))
            }
        }, sdp)
        deferred.await()
    }

    companion object {
        private const val TAG = "WebRtcAudioCall"
        @Volatile
        private var isFactoryInitialized = false
    }
}

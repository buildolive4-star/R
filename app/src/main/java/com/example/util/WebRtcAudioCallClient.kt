package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Clean WebRTC PeerConnection + Audio/Video Camera wrapper powered by io.getstream:stream-webrtc-android.
 * Supports both crystal-clear 1-on-1 Voice Calls and private HD Video Calls.
 */
class WebRtcAudioCallClient(
    context: Context,
    val isVideoCall: Boolean = false,
    private val onLocalIceCandidate: (IceCandidate) -> Unit,
    private val onConnectionStateChanged: (PeerConnection.PeerConnectionState) -> Unit,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var audioFocusRequest: AudioFocusRequest? = null

    private var eglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null

    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null

    private val _localVideoTrackFlow = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrackFlow: StateFlow<VideoTrack?> = _localVideoTrackFlow.asStateFlow()

    private val _remoteVideoTrackFlow = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrackFlow: StateFlow<VideoTrack?> = _remoteVideoTrackFlow.asStateFlow()

    val eglBaseContext: EglBase.Context?
        get() = eglBase?.eglBaseContext

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

        val factoryBuilder = PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)

        if (isVideoCall) {
            val createdEgl = EglBase.create()
            eglBase = createdEgl
            val encoderFactory = DefaultVideoEncoderFactory(createdEgl.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(createdEgl.eglBaseContext)
            factoryBuilder
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
        }

        val createdFactory = factoryBuilder.createPeerConnectionFactory()
        factory = createdFactory

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
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
                override fun onAddStream(stream: MediaStream?) {
                    val track = stream?.videoTracks?.firstOrNull()
                    if (track != null) {
                        track.setEnabled(true)
                        _remoteVideoTrackFlow.value = track
                    }
                }

                override fun onRemoveStream(stream: MediaStream?) = Unit
                override fun onDataChannel(dc: DataChannel?) = Unit
                override fun onRenegotiationNeeded() = Unit

                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        track.setEnabled(true)
                        _remoteVideoTrackFlow.value = track
                    }
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    if (track is VideoTrack) {
                        track.setEnabled(true)
                        _remoteVideoTrackFlow.value = track
                    }
                }

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
        pc.addTrack(track, listOf("justus_media_stream"))

        if (isVideoCall) {
            setupLocalCameraTrack(createdFactory, pc)
        }

        configureVoiceAudioMode(speakerOn = isVideoCall)
    }

    private fun setupLocalCameraTrack(createdFactory: PeerConnectionFactory, pc: PeerConnection) {
        val eglCtx = eglBase?.eglBaseContext ?: return
        runCatching {
            val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(appContext)) {
                Camera2Enumerator(appContext)
            } else {
                Camera1Enumerator(true)
            }
            val deviceNames = enumerator.deviceNames
            val chosenName = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: deviceNames.firstOrNull()
                ?: return

            val capturer = enumerator.createCapturer(chosenName, null) as? CameraVideoCapturer ?: return
            videoCapturer = capturer

            val helper = SurfaceTextureHelper.create("DateMateCaptureThread", eglCtx)
            surfaceTextureHelper = helper

            val vSource = createdFactory.createVideoSource(capturer.isScreencast)
            videoSource = vSource

            capturer.initialize(helper, appContext, vSource.capturerObserver)
            capturer.startCapture(720, 1280, 24)

            val vTrack = createdFactory.createVideoTrack("justus_video_track", vSource).apply {
                setEnabled(true)
            }
            localVideoTrack = vTrack
            _localVideoTrackFlow.value = vTrack
            pc.addTrack(vTrack, listOf("justus_media_stream"))
        }.onFailure { err ->
            Log.w(TAG, "Unable to start local camera capture", err)
        }
    }

    suspend fun createOfferSdp(): String {
        ensureInitialized()
        val pc = peerConnection ?: error("PeerConnection unavailable")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
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
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
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

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera(onSwitched: (isFrontCamera: Boolean) -> Unit = {}) {
        val capturer = videoCapturer ?: return
        runCatching {
            capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    onSwitched(isFrontCamera)
                }

                override fun onCameraSwitchError(errorDescription: String?) {
                    Log.w(TAG, "Camera switch failed: $errorDescription")
                }
            })
        }
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
        _localVideoTrackFlow.value = null
        _remoteVideoTrackFlow.value = null
        runCatching {
            audioManager.isMicrophoneMute = false
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = previousAudioMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            }
        }
        runCatching { videoCapturer?.stopCapture() }
        runCatching { videoCapturer?.dispose() }
        runCatching { localVideoTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        runCatching { surfaceTextureHelper?.dispose() }
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { audioDeviceModule?.release() }
        runCatching { factory?.dispose() }
        runCatching { eglBase?.release() }
        videoCapturer = null
        localVideoTrack = null
        videoSource = null
        surfaceTextureHelper = null
        localAudioTrack = null
        audioSource = null
        peerConnection = null
        audioDeviceModule = null
        factory = null
        eglBase = null
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
        private const val TAG = "WebRtcCallClient"
        @Volatile
        private var isFactoryInitialized = false
    }
}

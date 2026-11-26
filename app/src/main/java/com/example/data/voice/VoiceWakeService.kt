package com.example.data.voice

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.VoiceAiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "VoiceWakeService"
private const val CHANNEL_ID_STATUS = "brohi_wake_service_channel"
private const val CHANNEL_ID_WAKE_ALERT = "brohi_wake_alert_channel"
private const val NOTIFICATION_ID_STATUS = 4001
private const val NOTIFICATION_ID_WAKE = 4002

class VoiceWakeService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var partialWakeLock: PowerManager.WakeLock? = null
    private var isStopped = false
    private var isListeningCycleActive = false
    private var activeLocaleTag = "ur-PK"
    private var userDisplayName = ""

    private val restartRunnable = Runnable {
        if (!isStopped && !VoiceWakeCoordinator.foregroundMicBusy.value) {
            startWakeListeningCycle()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquirePartialWakeLock()
        serviceScope.launch {
            VoiceWakeCoordinator.foregroundMicBusy.collectLatest { busy ->
                if (busy) {
                    pauseRecognizer()
                    VoiceWakeCoordinator.setWakeServiceRunning(true, "Assistant Active")
                } else if (!isStopped) {
                    VoiceWakeCoordinator.setWakeServiceRunning(true, "Listening for 'Hey Ice'")
                    scheduleRestart(450L)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_WAKE) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        activeLocaleTag = intent?.getStringExtra(EXTRA_LOCALE_TAG)?.takeIf { it.isNotBlank() } ?: activeLocaleTag
        userDisplayName = intent?.getStringExtra(EXTRA_USER_NAME)?.takeIf { it.isNotBlank() } ?: userDisplayName

        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasMic || !SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Cannot start VoiceWakeService: mic permission=$hasMic")
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_STATUS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID_STATUS, notification)
        }

        VoiceWakeCoordinator.setWakeServiceRunning(true, "Listening for 'Hey Ice'")
        if (!VoiceWakeCoordinator.foregroundMicBusy.value) {
            scheduleRestart(250L)
        }
        return START_STICKY
    }

    private fun acquirePartialWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BrohiIce::HeyIcePartialLock")
                .apply {
                    setReferenceCounted(false)
                    acquire(6 * 60 * 60 * 1000L) // Refreshable safety cap
                }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to acquire partial wake lock: ${e.message}")
        }
    }

    private fun wakeScreenAndLaunchAssistant(trailingCommand: String) {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val screenLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "BrohiIce::HeyIceScreenWake"
            )
            screenLock.acquire(8_000L)
        } catch (e: Exception) {
            Log.w(TAG, "Screen wake lock failed: ${e.message}")
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_WAKE_TRIGGERED
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(EXTRA_WAKE_TRIGGERED, true)
            putExtra(EXTRA_WAKE_COMMAND, trailingCommand)
            putExtra(EXTRA_USER_NAME, userDisplayName)
        }

        // Pause background recognizer immediately so MainActivity foreground mic has clean audio
        VoiceWakeCoordinator.setForegroundBusy(true)
        pauseRecognizer()

        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = km?.isKeyguardLocked == true

        // Post full-screen high-priority notification so Android 10-15 allows waking over lock screen reliably
        val fullScreenPending = PendingIntent.getActivity(
            this,
            902,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val greetingTitle = if (userDisplayName.isNotBlank()) "Hey Ice • Listening for $userDisplayName" else "Hey Ice • Voice Assistant"
        val wakeNotification = NotificationCompat.Builder(this, CHANNEL_ID_WAKE_ALERT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(greetingTitle)
            .setContentText(if (trailingCommand.isNotBlank()) trailingCommand else "Tap or speak your factory command")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setTimeoutAfter(8_000L)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .build()
        nm.notify(NOTIFICATION_ID_WAKE, wakeNotification)

        if (!isLocked) {
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Direct startActivity blocked by OS, relying on FullScreenIntent: ${e.message}")
            }
        } else {
            try {
                startActivity(launchIntent)
            } catch (_: Exception) {
            }
        }
    }

    private fun startWakeListeningCycle() {
        if (isStopped || VoiceWakeCoordinator.foregroundMicBusy.value || isListeningCycleActive) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelfCleanly()
            return
        }

        pauseRecognizer()
        isListeningCycleActive = true
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    isListeningCycleActive = false
                    val backoff = when (error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        SpeechRecognizer.ERROR_AUDIO -> 1_400L
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 2_200L
                        else -> 350L
                    }
                    scheduleRestart(backoff)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    checkCandidatesForWakeWord(matches)
                }

                override fun onResults(results: Bundle?) {
                    isListeningCycleActive = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val triggered = checkCandidatesForWakeWord(matches)
                    if (!triggered) {
                        scheduleRestart(300L)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val recIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLocaleTag.ifBlank { "ur-PK" })
                putExtra(
                    RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                    arrayListOf("ur-PK", "hi-IN", "en-IN", "en-US")
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1_500L)
            }
            recognizer.startListening(recIntent)
        } catch (e: Exception) {
            isListeningCycleActive = false
            Log.w(TAG, "Wake recognizer cycle error: ${e.message}")
            scheduleRestart(1_800L)
        }
    }

    private fun checkCandidatesForWakeWord(candidates: List<String>): Boolean {
        for (raw in candidates) {
            val match = VoiceAiRepository.matchWakePhrase(raw)
            if (match.matched) {
                Log.i(TAG, "Wake phrase matched in '$raw' -> trailing='${match.trailingCommand}'")
                isListeningCycleActive = false
                wakeScreenAndLaunchAssistant(match.trailingCommand)
                return true
            }
        }
        return false
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacks(restartRunnable)
        if (!isStopped && !VoiceWakeCoordinator.foregroundMicBusy.value) {
            mainHandler.postDelayed(restartRunnable, delayMs)
        }
    }

    private fun pauseRecognizer() {
        mainHandler.removeCallbacks(restartRunnable)
        isListeningCycleActive = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    private fun stopSelfCleanly() {
        isStopped = true
        pauseRecognizer()
        VoiceWakeCoordinator.setWakeServiceRunning(false, "Off")
        try {
            if (partialWakeLock?.isHeld == true) partialWakeLock?.release()
        } catch (_: Exception) {
        }
        partialWakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isStopped = true
        pauseRecognizer()
        VoiceWakeCoordinator.setWakeServiceRunning(false, "Off")
        try {
            if (partialWakeLock?.isHeld == true) partialWakeLock?.release()
        } catch (_: Exception) {
        }
        partialWakeLock = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildOngoingNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val openPending = PendingIntent.getActivity(
            this, 901, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, VoiceWakeService::class.java).apply {
            action = ACTION_STOP_WAKE
        }
        val stopPending = PendingIntent.getService(
            this, 903, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Brohi Ice Voice • 'Hey Ice' Active")
            .setContentText("Say 'Hey Ice' (اے آئس / हे आइस) anytime to talk")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPending)
            .addAction(0, "Turn Off", stopPending)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_STATUS,
                    "Hey Ice Always-On Listener",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps 'Hey Ice' hands-free wake word active"
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_WAKE_ALERT,
                    "Hey Ice Wake Screen Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Wakes the voice assistant screen when you say 'Hey Ice'"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }

    companion object {
        const val ACTION_WAKE_TRIGGERED = "com.example.ACTION_HEY_ICE_WAKE"
        const val ACTION_STOP_WAKE = "com.example.ACTION_STOP_WAKE_SERVICE"
        const val EXTRA_WAKE_TRIGGERED = "extra_hey_ice_triggered"
        const val EXTRA_WAKE_COMMAND = "extra_hey_ice_command"
        const val EXTRA_LOCALE_TAG = "extra_wake_locale"
        const val EXTRA_USER_NAME = "extra_wake_user_name"

        fun startOrUpdate(context: Context, userName: String, localeTag: String) {
            val intent = Intent(context, VoiceWakeService::class.java).apply {
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_LOCALE_TAG, localeTag)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceWakeService::class.java))
        }
    }
}

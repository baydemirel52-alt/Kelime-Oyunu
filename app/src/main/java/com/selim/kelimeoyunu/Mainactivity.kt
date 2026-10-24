package com.selim.kelimeoyunu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private lateinit var adMobManager: AdMobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadWordLists(applicationContext)
        adMobManager = AdMobManager(this)
        adMobManager.requestConsentAndInitAds()
        setContent {
            MaterialTheme {
                WordGameApp(adMobManager = adMobManager, onExitApp = { finish() })
            }
        }
    }
}

// ==========================================
// 0. GOOGLE ADMOB & UMP YONETICISI (GERCEK + TEST KORUMALI KIMLIKLER)
// ==========================================
class AdMobManager(private val activity: Activity) {
    companion object {
        // Senin Gercek AdMob Reklam Birimi Kimliklerin (Release APK/AAB'de otomatik devreye girer)
        const val REAL_INTERSTITIAL_ID = "ca-app-pub-2295789870341419/9733700921"
        const val REAL_REWARDED_ID = "ca-app-pub-2295789870341419/7299109278"
        // Not: AdMob'da Banner birimi olusturursan ID'sini asagidaki tirnak icine yazabilirsin (bos kalirsa menude banner gizlenir)
        const val REAL_BANNER_ID = ""

        // Google Resmi Test Reklam Birimi Kimlikleri (Debug derlemelerde ban riskini onlemek icin otomatik kullanilir)
        const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

        const val CONTACT_EMAIL = "baydemirel52@gmail.com"

        fun isDebugBuild(context: Context): Boolean {
            return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }

        fun getInterstitialUnitId(context: Context): String {
            return if (isDebugBuild(context)) TEST_INTERSTITIAL_ID else REAL_INTERSTITIAL_ID
        }

        fun getRewardedUnitId(context: Context): String {
            return if (isDebugBuild(context)) TEST_REWARDED_ID else REAL_REWARDED_ID
        }

        fun getBannerUnitId(context: Context): String {
            return if (isDebugBuild(context)) TEST_BANNER_ID else REAL_BANNER_ID
        }
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialTimeMs: Long = 0L
    var isRewardedLoaded by mutableStateOf(false)
        private set

    // Tam ekran reklam acikken zamana karsi yaris sayaci durur
    var isAdShowing by mutableStateOf(false)
        private set

    // AB / Ingiltere / Isvicre oyunculari icin Ayarlar'da "Reklam Tercihleri" butonu gerekir
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    // MobileAds.initialize en fazla bir kez cagrilsin
    private val adsInitStarted = AtomicBoolean(false)

    fun requestConsentAndInitAds() {
        val params = ConsentRequestParameters.Builder().build()
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    privacyOptionsRequired = consentInfo.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    if (consentInfo.canRequestAds()) {
                        initMobileAds()
                    }
                }
            },
            {
                // Onay bilgisi alinamadiysa yalnizca daha once izin kaydi varsa reklam baslat
                if (consentInfo.canRequestAds()) {
                    initMobileAds()
                }
            }
        )
        // Daha once onay verilmisse formu beklemeden baslat (initMobileAds tek sefer calisir)
        if (consentInfo.canRequestAds()) {
            initMobileAds()
        }
    }

    private fun initMobileAds() {
        if (!adsInitStarted.compareAndSet(false, true)) return
        MobileAds.initialize(activity) {
            loadInterstitial()
            loadRewarded()
        }
    }

    // Ayarlar > Reklam Tercihleri (onay secimini degistirme)
    fun showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            if (UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) {
                initMobileAds()
            }
        }
    }

    fun loadInterstitial() {
        // Onay alinmadan reklam istegi gonderme
        if (!adsInitStarted.get()) return
        InterstitialAd.load(
            activity,
            getInterstitialUnitId(activity),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun loadRewarded() {
        if (!adsInitStarted.get()) return
        RewardedAd.load(
            activity,
            getRewardedUnitId(activity),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoaded = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoaded = false
                }
            }
        )
    }

    // Ilk 5 bolumde cikmaz, sonrasinda en az 3 dakika (180.000 ms) arayla cikar
    fun maybeShowInterstitial(completedLevel: Int, onFinished: () -> Unit) {
        val now = System.currentTimeMillis()
        val minIntervalMs = 3 * 60 * 1000L
        val ad = interstitialAd
        if (completedLevel > 5 && ad != null && (now - lastInterstitialTimeMs >= minIntervalMs)) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isAdShowing = false
                    interstitialAd = null
                    lastInterstitialTimeMs = System.currentTimeMillis()
                    loadInterstitial()
                    onFinished()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isAdShowing = false
                    interstitialAd = null
                    loadInterstitial()
                    onFinished()
                }
            }
            isAdShowing = true
            ad.show(activity)
        } else {
            if (ad == null) loadInterstitial()
            onFinished()
        }
    }

    fun showRewardedAd(onRewardEarned: () -> Unit, onNotReady: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded()
            onNotReady()
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isAdShowing = false
                rewardedAd = null
                isRewardedLoaded = false
                loadRewarded()
                if (earned) onRewardEarned()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isAdShowing = false
                rewardedAd = null
                isRewardedLoaded = false
                loadRewarded()
                onNotReady()
            }
        }
        isAdShowing = true
        ad.show(activity) {
            earned = true
        }
    }

    // Gizlilik politikasindaki iletisim e-postasi (mail uygulamasini acar)
    fun openContactEmail() {
        try {
            val uri = Uri.parse("mailto:" + CONTACT_EMAIL + "?subject=" + Uri.encode("Kelime Efsanesi"))
            activity.startActivity(Intent(Intent.ACTION_SENDTO, uri))
        } catch (_: Exception) {}
    }
}

// Gunluk cark icin yerel saate gore gun numarasi (gece yarisi sifirlanir)
fun currentDayNumber(): Int {
    val now = System.currentTimeMillis()
    return ((now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L).toInt()
}

@Composable
fun MenuBannerAdView() {
    val context = LocalContext.current
    val unitId = remember { AdMobManager.getBannerUnitId(context) }
    if (unitId.isBlank()) return

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = unitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// ==========================================
// 1. SES MOTORU (GERCEK MP3/OGG + YORMAYAN ZEN AMBIYANS)
// ==========================================
class ProceduralSoundEngine(private val appContext: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var musicJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var assetMusicChecked = false
    private var hasAssetMusic = false

    var sfxEnabled = true
    var musicEnabled = true
    var isAppInForeground = true

    private val harpScale = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 587.33, 659.25, 783.99)

    fun playClick() {
        if (!sfxEnabled) return
        scope.launch {
            playHarmonicPluck(480.0, 35, 0.26f)
            playHarmonicPluck(960.0, 45, 0.20f)
        }
    }

    fun playSwipeNote(index: Int) {
        if (!sfxEnabled) return
        val base = harpScale[index.coerceIn(0, harpScale.lastIndex)]
        scope.launch { playHarmonicPluck(base, 110, 0.34f) }
    }

    fun playWordSuccess() {
        if (!sfxEnabled) return
        scope.launch {
            playHarmonicPluck(523.25, 75, 0.34f)
            playHarmonicPluck(659.25, 75, 0.34f)
            playHarmonicPluck(783.99, 85, 0.36f)
            playHarmonicPluck(1046.50, 170, 0.40f)
        }
    }

    fun playBonusWord() {
        if (!sfxEnabled) return
        scope.launch {
            playHarmonicPluck(587.33, 65, 0.34f)
            playHarmonicPluck(880.00, 90, 0.36f)
            playHarmonicPluck(1174.66, 150, 0.38f)
        }
    }

    fun playHammerHit() {
        if (!sfxEnabled) return
        scope.launch {
            playPercussiveImpact(110.0, 95, 0.46f)
            playHarmonicPluck(880.0, 120, 0.34f)
        }
    }

    fun playWheelTick() {
        if (!sfxEnabled) return
        scope.launch { playHarmonicPluck(700.0, 28, 0.22f) }
    }

    fun playLightningStrike() {
        if (!sfxEnabled) return
        scope.launch {
            playPercussiveImpact(95.0, 110, 0.48f)
            playHarmonicPluck(1318.5, 80, 0.38f)
            playHarmonicPluck(1567.98, 160, 0.40f)
        }
    }

    fun playError() {
        if (!sfxEnabled) return
        scope.launch {
            playHarmonicPluck(185.0, 80, 0.30f)
            playHarmonicPluck(155.0, 130, 0.30f)
        }
    }

    fun playLevelWin() {
        if (!sfxEnabled) return
        scope.launch {
            val fanfare = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 987.77, 1046.50)
            for (i in fanfare.indices) {
                playHarmonicPluck(fanfare[i], if (i == fanfare.lastIndex) 240 else 95, 0.40f)
            }
        }
    }

    fun updateBackgroundMusic() {
        musicJob?.cancel()
        if (!musicEnabled || !isAppInForeground) {
            try {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
            } catch (_: Exception) {}
            return
        }

        if (!assetMusicChecked) {
            assetMusicChecked = true
            val candidates = listOf("bg_music.mp3", "bg_music.ogg", "music.mp3")
            for (fileName in candidates) {
                try {
                    val afd = appContext.assets.openFd(fileName)
                    val mp = MediaPlayer()
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    mp.isLooping = true
                    mp.setVolume(0.28f, 0.28f)
                    mp.prepare()
                    mediaPlayer = mp
                    hasAssetMusic = true
                    break
                } catch (_: Exception) {}
            }
        }

        if (hasAssetMusic && mediaPlayer != null) {
            try {
                if (mediaPlayer?.isPlaying == false) mediaPlayer?.start()
            } catch (_: Exception) {}
            return
        }

        musicJob = scope.launch {
            val zenChords = arrayOf(
                doubleArrayOf(130.81, 196.00, 246.94),
                doubleArrayOf(110.00, 164.81, 220.00),
                doubleArrayOf(87.31, 130.81, 174.61),
                doubleArrayOf(98.00, 146.83, 196.00),
                doubleArrayOf(116.54, 174.61, 220.00),
                doubleArrayOf(110.00, 164.81, 246.94),
                doubleArrayOf(87.31, 146.83, 220.00),
                doubleArrayOf(98.00, 164.81, 246.94)
            )
            var step = 0
            while (isActive && musicEnabled && isAppInForeground) {
                val chord = zenChords[step % zenChords.size]
                playAmbientPadChord(chord[0], chord[1], chord[2], 2400, 0.032f)
                delay(2200)
                step++
            }
        }
    }

    private suspend fun playAmbientPadChord(f1: Double, f2: Double, f3: Double, durationMs: Int, volume: Float) = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val env = sin(PI * progress)
                val wave = (sin(2.0 * PI * f1 * t) + 0.7 * sin(2.0 * PI * f2 * t) + 0.5 * sin(2.0 * PI * f3 * t)) / 2.2
                samples[i] = (wave * Short.MAX_VALUE * volume * env).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            writeAndPlayTrack(samples, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private suspend fun playHarmonicPluck(freqHz: Double, durationMs: Int, volume: Float) = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = exp(-4.5 * i.toDouble() / numSamples) * (if (i < 60) i / 60.0 else 1.0)
                val wave = (sin(2.0 * PI * freqHz * t) + 0.35 * sin(2.0 * PI * (freqHz * 1.498) * t) + 0.25 * sin(2.0 * PI * (freqHz * 2.0) * t)) / 1.6
                samples[i] = (wave * Short.MAX_VALUE * volume * env).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            writeAndPlayTrack(samples, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private suspend fun playPercussiveImpact(baseFreq: Double, durationMs: Int, volume: Float) = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)
            val rng = Random(42)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = exp(-8.0 * i.toDouble() / numSamples)
                val wave = (sin(2.0 * PI * baseFreq * t) + (rng.nextDouble() * 2.0 - 1.0) * 0.4) * 0.7
                samples[i] = (wave * Short.MAX_VALUE * volume * env).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            writeAndPlayTrack(samples, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private suspend fun writeAndPlayTrack(samples: ShortArray, sampleRate: Int, durationMs: Int) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        delay(durationMs.toLong() + 15)
        track.stop()
        track.release()
    }
}

// ==========================================
// 2. DUNYA TEMALARI, MUZE ESERLERI & SOZLUK ANLAMLARI
// ==========================================
enum class Lang(val code: String, val flag: String, val label: String) {
    TR("tr", "🇹🇷", "TR"),
    EN("en", "🇬🇧", "EN")
}

data class WorldTheme(
    val zoneIndex: Int,
    val regionNameTr: String,
    val regionNameEn: String,
    val monumentNameTr: String,
    val bossTitleTr: String,
    val bossTitleEn: String,
    val bgGradient: List<Color>,
    val accentColor: Color,
    val openTileGradient: List<Color>
)

fun getWorldThemeForLevel(level: Int): WorldTheme = when (((level - 1) / 10) % 10) {
    0 -> WorldTheme(0, "Göbeklitepe Vadisi", "Göbeklitepe Valley", "Göbeklitepe T-Dikilitaşı", "🗿 Göbeklitepe Muhafızı", "🗿 Guardian of Göbeklitepe", listOf(Color(0xFF1C140D), Color(0xFF3B2314), Color(0xFF24130B)), Color(0xFFF59E0B), listOf(Color(0xFFD97706), Color(0xFFB45309)))
    1 -> WorldTheme(1, "Ayasofya Meydanı", "Hagia Sophia Square", "Ayasofya Büyük Kubbesi", "⚔️ Ayasofya Muhafızı", "⚔️ Guardian of Hagia Sophia", listOf(Color(0xFF1F0916), Color(0xFF3F1028), Color(0xFF230A34)), Color(0xFFF43F5E), listOf(Color(0xFFE11D48), Color(0xFF9F1239)))
    2 -> WorldTheme(2, "Efes Antik Kenti", "Ancient Ephesus", "Celsus Mermer Cephesi", "🏛️ Celsus Muhafızı", "🏛️ Guardian of Ephesus", listOf(Color(0xFF061A2E), Color(0xFF0C365A), Color(0xFF08223A)), Color(0xFF38BDF8), listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
    3 -> WorldTheme(3, "Galata & Boğaz Gecesi", "Galata Midnight", "Galata Kulesi Külahı", "🦅 Galata Muhafızı", "🦅 Guardian of Galata", listOf(Color(0xFF0B1120), Color(0xFF1E1B4B), Color(0xFF311042)), Color(0xFFA855F7), listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)))
    4 -> WorldTheme(4, "Giza Piramitleri", "Pyramids of Giza", "Keops Altın Piramidi", "👑 Firavun Muhafızı", "👑 Pharaoh Guardian", listOf(Color(0xFF221A09), Color(0xFF42300E), Color(0xFF1C1306)), Color(0xFFFBBF24), listOf(Color(0xFFCA8A04), Color(0xFF854D0E)))
    5 -> WorldTheme(5, "Roma Kolezyumu", "Roman Colosseum", "Kolezyum Kemerleri", "🛡️ Gladyatör Muhafızı", "🛡️ Gladiator Guardian", listOf(Color(0xFF230C0C), Color(0xFF451A1A), Color(0xFF1A0B12)), Color(0xFFFB923C), listOf(Color(0xFFEA580C), Color(0xFF9A3412)))
    6 -> WorldTheme(6, "Sümela Zümrüt Ormanı", "Sumela Emerald Forest", "Sümela Kaya Mabedi", "🌲 Karadağ Muhafızı", "🌲 Guardian of Sumela", listOf(Color(0xFF051F18), Color(0xFF0B3B2D), Color(0xFF072227)), Color(0xFF34D399), listOf(Color(0xFF10B981), Color(0xFF047857)))
    7 -> WorldTheme(7, "Tac Mahal Sarayı", "Taj Mahal Palace", "Tac Mahal Mermer Sarayı", "💎 Babür Muhafızı", "💎 Taj Mahal Guardian", listOf(Color(0xFF161130), Color(0xFF2D1B4E), Color(0xFF3B1239)), Color(0xFFF472B6), listOf(Color(0xFFDB2777), Color(0xFF9D174D)))
    8 -> WorldTheme(8, "Nemrut Dağı Zirvesi", "Mount Nemrut Peak", "Nemrut Kral Heykelleri", "☀️ Kommagene Muhafızı", "☀️ Guardian of Nemrut", listOf(Color(0xFF181124), Color(0xFF331D4A), Color(0xFF421C24)), Color(0xFFFBBF24), listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
    else -> WorldTheme(9, "Machu Picchu Bulutları", "Clouds of Machu Picchu", "İnka Güneş Tapınağı", "🏔️ İnka Muhafızı", "🏔️ Incan Guardian", listOf(Color(0xFF071E22), Color(0xFF113F46), Color(0xFF092327)), Color(0xFF2DD4BF), listOf(Color(0xFF0D9488), Color(0xFF115E59)))
}

val PRAISE_WORDS_TR = listOf("🔥 HARİKASIN!", "⚡ MUHTEŞEMSİN!", "👑 ZEKA KÜPÜ!", "🚀 DURDURULAMAZSIN!", "💎 EFSANE ÇÖZÜM!", "🌟 SÜPERSİN!", "🎯 TAM İSABET!", "💥 ŞOV YAPIYORSUN!")
val PRAISE_WORDS_EN = listOf("🔥 AWESOME!", "⚡ MAGNIFICENT!", "👑 GENIUS MOVE!", "🚀 UNSTOPPABLE!", "💎 LEGENDARY!", "🌟 SUPERB!", "🎯 BULLSEYE!")
val LEVEL_WIN_PRAISE_TR = listOf("🏆 MUHTEŞEMSİN! BÖLÜMÜ YIKTIN GEÇTİN!", "🔥 HARİKASIN! ZEKA ŞOV YAPTIN!", "👑 EFSANESİN! KİMSE SENİ TUTAMAZ!", "⚡ OLAĞANÜSTÜ! BEYİN FIRTINASI ESTİRDİN!", "💎 ŞAMPİYON GİBİ ÇÖZDÜN, HELAL OLSUN!")
val LEVEL_WIN_PRAISE_EN = listOf("🏆 MAGNIFICENT! YOU CRUSHED THIS LEVEL!", "🔥 AWESOME! PURE GENIUS DISPLAY!", "👑 LEGENDARY! NOTHING CAN STOP YOU!")

val WORD_MEANINGS_TR = mapOf(
    "BAL" to "Arıların çiçek özlerinden yaptığı tatlı, şifalı besin.",
    "KALE" to "Düşman saldırılarına karşı kalın surlarla çevrili tarihi savunma yapısı.",
    "MASA" to "Ayakları üzerine oturtulmuş düz yüzeyli mobilya.",
    "KAYA" to "Yerkabuğunu oluşturan büyük ve sert taş kütlesi.",
    "SIRA" to "Arka arkaya veya yan yana dizilmiş şeylerin bütünü; düzen.",
    "KAPI" to "Bir yere girip çıkarken geçilen açılır kapanır bölme.",
    "YURT" to "Bir halkın üzerinde yaşadığı, kültürünü oluşturduğu toprak; vatan.",
    "RENK" to "Işığın göz retinasına ulaşmasıyla oluşan görsel algı.",
    "DOST" to "Güvenilen, sevilen yakın arkadaş, gönüldaş.",
    "ARKA" to "Bir şeyin temel tutulan yüzünün tam tersi olan taraf; geri.",
    "KALEM" to "Yazma veya çizme işlerinde kullanılan araç.",
    "KİTAP" to "Ciltlenmiş basılı veya yazılı sayfalar bütünü, eser.",
    "TAKİP" to "Bir kimsenin veya olayın izini sürme, ardı sıra gitme.",
    "PATİK" to "Özellikle bebekler veya ev içi kullanım için örülen kısa ayak giysisi.",
    "KATİ" to "Kesin, şüphesiz, tereddüt bırakmayan.",
    "PAK" to "Çok temiz, arınmış, lekesiz.",
    "DENİZ" to "Yeryüzünün çukur bölümlerini dolduran büyük tuzlu su kütlesi.",
    "TARİH" to "Geçmişteki olayları yer ve zaman göstererek inceleyen bilim dalı.",
    "SEVGİ" to "İnsanı bir şeye veya bir kimseye yakın ilgi göstermeye yönelten duygu.",
    "BAHAR" to "Kış ile yaz arasındaki doğanın canlandığı ılıman mevsim.",
    "GÜNEŞ" to "Dünyamızı ısıtan ve aydınlatan sistemimizin merkez yıldızı.",
    "ORMAN" to "Ağaçlarla ve doğal bitki örtüsüyle kaplı geniş yeşil alan."
)

fun getWordMeaning(word: String, lang: Lang): String {
    if (lang == Lang.EN) return "English vocabulary word (${word.length} letters) — Score value: ${word.length * 15} pts."
    return WORD_MEANINGS_TR[word]
        ?: "Türkçe sözlükte yer alan ${word.length} harfli anlamlı kelime. (Harf Değeri: ${word.length * 15} Puan)"
}

data class HistoricalFact(val levelMilestone: Int, val titleTr: String, val infoTr: String, val titleEn: String, val infoEn: String)

val HISTORICAL_FACTS = listOf(
    HistoricalFact(10, "Göbeklitepe (Şanlıurfa, Türkiye)", "M.Ö. 9600 civarında inşa edilen Göbeklitepe, insanlık tarihinin bilinen en eski anıtsal tapınak yerleşkesidir. Mısır Piramitlerinden yaklaşık 7.000 yıl daha eskidir.", "Göbeklitepe (Şanlıurfa, Turkey)", "Built around 9600 BCE, Göbeklitepe is the world's oldest known monumental temple complex, predating the Pyramids by 7,000 years."),
    HistoricalFact(20, "Ayasofya (İstanbul, Türkiye)", "537 yılında Bizans İmparatoru I. Justinianus tarafından inşa ettirilmiştir. Devasa kubbesiyle dünya mimarlık tarihinin başyapıtlarından biridir ve 1453'te camiye çevrilmiştir.", "Hagia Sophia (Istanbul, Turkey)", "Completed in 537 CE under Emperor Justinian I, Hagia Sophia is famous for its massive dome and was converted into a mosque in 1453."),
    HistoricalFact(30, "Efes & Celsus Kütüphanesi (İzmir)", "M.S. 117 yılında tamamlanan Celsus Kütüphanesi, antik dünyanın en büyük üçüncü kütüphanesiydi ve içerisinde yaklaşık 12.000 el yazması rulo saklanmaktaydı.", "Ephesus & Library of Celsus (Izmir)", "Completed in 117 CE, the Library of Celsus stored around 12,000 scrolls and stood as the third-largest library of the ancient world."),
    HistoricalFact(40, "Galata Kulesi (İstanbul, Türkiye)", "1348 yılında Cenevizliler tarafından gözetleme amacıyla inşa edilmiştir. Hezârfen Ahmed Çelebi'nin 1632'de tahta kanatlarla buradan Üsküdar'a uçtuğu rivayet edilir.", "Galata Tower (Istanbul, Turkey)", "Built by the Genoese in 1348, this medieval stone tower became legendary for Hezarfen Ahmed Çelebi's 17th-century flight across the Bosphorus."),
    HistoricalFact(50, "Keops Piramidi (Giza, Mısır)", "M.Ö. 2560 civarında Firavun Khufu adına yapılan Büyük Piramit, yaklaşık 2.3 milyon taş bloktan oluşur ve 3.800 yıl boyunca dünyanın en yüksek yapısı kalmıştır.", "Great Pyramid of Giza (Egypt)", "Constructed around 2560 BCE using 2.3 million stone blocks, it remained the tallest human-made structure on Earth for over 3,800 years."),
    HistoricalFact(60, "Kolezyum (Roma, İtalya)", "M.S. 80 yılında İmparator Titus döneminde tamamlanan Kolezyum, 50.000'den fazla seyirci kapasitesine ve gelişmiş yeraltı asansör sistemlerine sahipti.", "The Colosseum (Rome, Italy)", "Completed in 80 CE under Emperor Titus, the Colosseum held over 50,000 spectators and featured a complex underground network of elevators."),
    HistoricalFact(70, "Sümela Manastırı (Trabzon, Türkiye)", "Karadağ'ın sarp kayalıklarına denizden 1.200 metre yükseklikte, M.S. 386 yılında inşa edilmeye başlanmıştır. Kayalara oyulmuş mimarisiyle eşsizdir.", "Sumela Monastery (Trabzon, Turkey)", "Founded in 386 CE on a steep cliff 1,200 meters above sea level in Trabzon, famous for its dramatic rock-cut architecture."),
    HistoricalFact(80, "Tac Mahal (Agra, Hindistan)", "1632-1653 yılları arasında Babür İmparatoru Şah Cihan tarafından eşi Mümtaz Mahal anısına beyaz mermerden yaptırılmıştır.", "Taj Mahal (Agra, India)", "Commissioned in 1632 by Emperor Shah Jahan in memory of his wife Mumtaz Mahal, universally admired as a masterpiece of symmetry."),
    HistoricalFact(90, "Nemrut Dağı Tümülüsü (Adıyaman)", "M.Ö. 62 yılında Kommagene Kralı I. Antiochos tarafından 2.150 metre zirveye yaptırılmıştır. Dev taş heykeller güneşin doğuşunu selamlar.", "Mount Nemrut (Adıyaman, Turkey)", "Built in 62 BCE by King Antiochus I at a 2,150-meter peak, featuring colossal statues guarding a royal tomb sanctuary."),
    HistoricalFact(100, "Machu Picchu (Cusco, Peru)", "1450 yıllarında İnka İmparatoru Pachacuti tarafından And Dağları'nın zirvesine harç kullanılmadan kenetlenen taşlarla inşa edilmiştir.", "Machu Picchu (Cusco, Peru)", "Built around 1450 CE high in the Andes using dry-stone walls, Machu Picchu escaped conquest and remained intact for centuries.")
)

data class LeagueInfo(val index: Int, val nameTr: String, val nameEn: String, val icon: String, val color: Color)

fun getLeagueForLevel(level: Int): LeagueInfo = when ((level - 1) / 10) {
    0 -> LeagueInfo(0, "Bronz Ligi", "Bronze League", "🥉", Color(0xFFCD7F32))
    1 -> LeagueInfo(1, "Gümüş Ligi", "Silver League", "🥈", Color(0xFFE2E8F0))
    2 -> LeagueInfo(2, "Altın Ligi", "Gold League", "🥇", Color(0xFFFFD700))
    3 -> LeagueInfo(3, "Platin Ligi", "Platinum League", "💎", Color(0xFF00E5FF))
    4 -> LeagueInfo(4, "Elmas Ligi", "Diamond League", "👑", Color(0xFFB388FF))
    5 -> LeagueInfo(5, "Usta Ligi", "Master League", "🔥", Color(0xFFFF5252))
    6 -> LeagueInfo(6, "Büyük Usta Ligi", "Grandmaster League", "⚡", Color(0xFFFFAB00))
    7 -> LeagueInfo(7, "Şampiyon Ligi", "Champion League", "🦁", Color(0xFF34D399))
    8 -> LeagueInfo(8, "Destansı Lig", "Mythic League", "🐉", Color(0xFFF472B6))
    else -> LeagueInfo(9, "Efsane İmparator", "Legend Emperor", "🌟", Color(0xFF69F0AE))
}

// ==========================================
// 3. HER 10 SEVIYEDE HARF SAYISI ARTAN SOZLUK & ASSETS YUKLEYICI
// ==========================================
val TR_WORDS_TIER_1 = listOf("BAL", "KALE", "MASA", "KAYA", "SIRA", "KAPI", "YURT", "RENK", "DOST", "ARKA")
val TR_WORDS_TIER_2 = listOf("KALEM", "KİTAP", "DENİZ", "TARİH", "SEVGİ", "BAHAR", "GÜNEŞ", "ORMAN", "ASLAN", "SARAY")
val TR_WORDS_TIER_3 = listOf("KARTAL", "MİMARİ", "EFSANE", "DESTAN", "KORSAN", "KALKAN", "MEYDAN", "CÜZDAN", "FİNCAN", "MERCAN")
val TR_WORDS_TIER_4 = listOf("BAŞKENT", "MANZARA", "FABRİKA", "TEHLİKE", "BİLMECE", "ÇEKMECE", "KARINCA", "PALAMUT", "OKYANUS", "GELENEK")
val TR_WORDS_TIER_5 = listOf("KAHRAMAN", "TEKERLEK", "KARANFİL", "ŞAMPİYON", "GÜVERCİN", "PORTAKAL", "KARDELEN", "YOLCULUK", "MUTLULUK", "ÖZGÜRLÜK")

val TR_WORDS_ADVANCED = """
MASAL KAYIK YATAK TABAK KABAK SOKAK LİMAN ZAMAN SAMAN KEMİK DEMİR ŞEHİR NEHİR ZEHİR FİKİR ŞÜKÜR BAKIR ÇAKIR SABIR SATIR
KATIR ÇADIR KADIN YARIN KARIN BURUN TORUN SORUN BOYUN KOYUN SABUN YOSUN KAVUN DAVUL BAVUL KABUL KURAL KURAK DURAK
ÇIRAK MERAK SICAK TABAN TAVAN KAVAN KAZAN SAZAN YAZAR PAZAR HAZAR KARAR ZARAR DAMAR ÇINAR PINAR KENAR FENER KEMER SEMER
BİBER HABER ŞEKER DEĞER KADER KEDER SEFER ZAFER NEFES KAFES HEVES ADRES TERAS MAKAS TAKAS SAVAŞ YAVAŞ TIRAŞ KUMAŞ YAMAÇ
ÇEKİÇ BARIŞ YARIŞ KARIŞ ALKIŞ BAKIŞ YAKIŞ ÇIKIŞ NAKIŞ SATIŞ YATIŞ KAÇIŞ DİKİŞ GİRİŞ VERİŞ GELİŞ BİLİŞ GÖRÜŞ DÖNÜŞ
YORGAN HAYVAN ZİNDAN DESTEK GÖMLEK ÇELENK BÖBREK ŞİMŞEK LEYLEK SÖZLÜK GÖZLÜK TOPRAK YAPRAK BAYRAK KAYNAK BARDAK PARMAK SALKIM YARDIM KAVRUK
SAVRUK KUVVET DEVLET SERVET ŞEFKAT DİKKAT SOHBET GURBET HASRET KISMET HİZMET HİKMET ZAHMET RAHMET MERKEZ HERKES KELEBEK SEÇENEK YETENEK TENCERE
PENCERE DEMİRCİ BALIKÇI EKMEKÇİ SİMİTÇİ EĞİTİMCİ ÖĞRETMEN MÜHENDİS MİMARLIK CUMHURİYET
""".trimIndent().split(Regex("\\s+")).map { it.uppercase(Locale("tr", "TR")) }.distinct()

val TR_ALL_TIER_WORDS = TR_WORDS_TIER_1 + TR_WORDS_TIER_2 + TR_WORDS_TIER_3 + TR_WORDS_TIER_4 + TR_WORDS_TIER_5 + TR_WORDS_ADVANCED

val TR_CURATED_DICTIONARY: Set<String> = (TR_ALL_TIER_WORDS + """
AB AC AÇ AD AF AG AH AK AL AM AN AR AS AŞ AT AV AY AZ BA BE BU DA DE EK EL EM EN ER ES EŞ ET EV EY
İÇ İL İM İN İP İS İŞ İT İZ KA KE Kİ LA LE MA ME Mİ NE OD OF OH OK OL OM ON OT OY ÖÇ ÖD ÖK ÖL ÖN ÖZ
PA PE Pİ RE SU ŞU TA TE Tİ UÇ UK UL UN UR US UT UY ÜÇ ÜN ÜS ÜZ YA YE YO Zİ
ABA ABİ ACI AÇI ADA ADI AĞA AĞI AHA AHİ AİT AKA AKI ALA ALİ AMA ANA ANI ANİ ARA ARI ARİ ARK ARP ART ARZ
ASA ASİ AŞI AŞK ATA ATI AVI AYA AYI AZA AZI BAÇ BAD BAĞ BAK BAL BAN BAR BAS BAŞ BAT BAY BAZ BEK BEL BEN
BER BES BEŞ BET BEY BEZ BİN BİR BİT BİZ BOA BOK BOL BOM BOR BOT BOY BOZ BÖL BÜK BÜZ CAM CAN CAR CAZ CEM
CEP CER CET CİL CİN CİP CİS ÇAB ÇAĞ ÇAK ÇAL ÇAM ÇAN ÇAP ÇAR ÇAT ÇAY ÇEK ÇİL ÇİM ÇİN ÇİP ÇİT ÇİZ ÇOK ÇÖL
ÇÖZ DAĞ DAL DAM DAR DAV DAY DAZ DEK DEM DEN DER DES DEV DİK DİL DİN DİP DİR DİS DİŞ DİZ DOĞ DOK DOL DON
DOR DOY DOZ DÖK DÖL DÖN DÖŞ DUA DUL DUN DUR DUŞ DUT DUY DÜŞ DÜZ EBE ECE EDA EDE EDİ EGE EĞE EKİ ELA ELİ
EME EMİ ENİ EPE ERK ERİ EŞİ ETİ EVİ EZA EZE FAN FAR FAS FAŞ FAY FAZ FEK FEN FER FES FET FİL FİN FİŞ FİT
FON FOR FOY FÖN FUL FUT GAF GAH GAM GAR GAZ GEÇ GEM GEN GER GEZ GIK GİT GİY GİZ GOL GÖÇ GÖK GÖL GÖN GÖR
GÖZ GÜR GÜÇ GÜL GÜM GÜN GÜZ HAB HAC HAÇ HAD HAF HAK HAL HAM HAN HAP HAR HAS HAT HAV HAY HAZ HEM HEP HER
HIR HİS HIZ HİÇ HOL HOR HOŞ HÖY HÜZ IRA IRK İDE İKİ İLA İLE İLİ İMA İMİ İNİ İPİ İRİ İSA İSİ İŞİ İTİ İYİ
İZİ KAÇ KAD KAF KAH KAL KAM KAN KAP KAR KAS KAŞ KAT KAV KAY KAZ KEK KEL KEM KEN KER KES KEŞ KET KEZ KIK
KIL KIN KIP KIR KIS KIŞ KIT KIY KIZ KİL KİM KİN KİP KİR KİT KOD KOK KOL KOM KON KOP KOR KOŞ KOT KOY KOZ
KÖK KÖL KÖR KÖS KÖY KÖZ KUL KUM KUN KUP KUR KUS KUŞ KUT KUY KUZ KÜL KÜP KÜR KÜS KÜT LAF LAK LAM LAN LAP
LAR LAS LAT LAV LAZ LEF LEH LEŞ LİF LİG LİM LİR LÖK LÜK MAÇ MAİ MAK MAL MAN MAS MAT MAY MAZ MEÇ MEY MİL
MİM MİN MİR MİS MİT MOR MUŞ MUT MUZ MÜL NAM NAN NAR NAS NAZ NEM NET NEY NİL NİM NİŞ NOD NOM NOT NUR ODA
OJE OKA OKU OLA OLU OMA ONA ONİ ORA ORG ORT OYA OYU ÖBE ÖCÜ ÖDE ÖĞE ÖKE ÖLÜ ÖNÜ ÖPÜ ÖRÜ ÖTE ÖTÜ ÖZÜ PAK
PAL PAM PAN PAR PAS PAT PAY PAZ PEÇ PEK PES PET PİL PİM PİR PİS PİT POZ PÖF PUL PUN PUS PUT PÜR RAB RAM
RAP RAY RAZ RED REN RET REY RİA RİN RİT ROD ROL ROM ROT ROZ RUH RUJ RUM RUN RUS RUT SAÇ SAF SAĞ SAH SAK
SAL SAM SAN SAP SAR SAS SAT SAV SAY SAZ SEÇ SEK SEL SEM SEN SER SES SET SEV SEY SEZ SIĞ SIK SIN SIR SIS
SIT SIZ SİL SİM SİN SİR SİS SİT SİZ SOF SOK SOL SOM SON SOY SÖZ SUÇ SUL SUN SUR SUS SÜS SÜT ŞAH ŞAK ŞAL
ŞAM ŞAN ŞAP ŞAR ŞAT ŞAZ ŞEF ŞEK ŞEM ŞEN ŞER ŞEŞ ŞEY ŞIK Şİİ ŞİP ŞOK ŞOR ŞOV ŞUA ŞUH TAB TAÇ TAK TAL TAM
TAN TAP TAR TAS TAŞ TAT TAV TAY TAZ TEF TEK TEL TEM TEN TER TES TEZ TIK TIN TIP TIR TIS TİP TİR TİZ TOK
TON TOP TOR TOS TOY TOZ TÖS TUĞ TUL TUN TUR TUŞ TUT TUZ TÜL TÜM TÜR TÜY UCA UCU UÇU UFA ULA ULU UMA UMU
UNA UNU URA URU USA USU UTA UTU UYA UYU UZA UZU ÜÇÜ ÜLÜ ÜMİ ÜNÜ ÜRÜ ÜSÜ ÜTÜ ÜVE ÜZÜ VAH VAK VAL VAN VAR
VAS VAY VAZ VEZ VİN VİR VİZ VUR YAB YAĞ YAK YAL YAM YAN YAP YAR YAS YAŞ YAT YAV YAY YAZ YEK YEL YEM YEN
YER YES YET YEZ YIĞ YIK YIL YIR YİV YOĞ YOK YOL YOM YON YOR YOS YOZ YÖN YUF YUH YUM YUN YUR YUT YUV YÜK
YÜN YÜZ ZAĞ ZAM ZAN ZAR ZAT ZIT ZİL ZİN ZİR ZOR ZÜL
KALE ELMA ALEM KEMAL EMLAK KAPI KATİ TAKİP PATİK DİZE EDİN ZİNDE AHİR TAHİR İHTAR EVGİ SEVİ HARA BARA
ONAR NORM ORAN ROMAN ALAN SALA SANA ASAL SANAL YARA SAYA SARA ARSA AYAR YASA KART KARAT TARLA KARA TAKA
ARKA AKAR MİMAR İMAM RAMİ SENE ENSE SEFA FENA NEFES ESNAF SEDA DANS SENA TANE SONRA KORA KORS KANAL
ALKAN KALA LAKA KLAN MADEN DEMA NAME CÜDA İNAN ACEM MERA MACERA KENT BANK BAŞAK BANKET ARMA MANA ARAZ
ZARA AZAR NARA ARAMA MARAZ ZAMAN AZMAN KİRA İBRA FAKİR ARİF ETKİ ETİK ETEK ELEK EKİL İLKE HİLE ELİT
İLME EMEL ELEM BİLE CEMİL EMEK EKME ÇEKE KEÇE ANCAK KARIN AKIN ARIK KANI ANCA MALT LAPA PALA TAPA MALA
ALAT APTAL KOYUN YOSUN KONU KOYU SOKU OYUN ONAY AKSU ENGEL GENEL EKEN GELE GENE KENE LEKE KARMA MARKA
KAMA HARMAN MAKARA ANKARA KARAMAN TEKER ERKEK EREK KERE TERK İNKAR FİRAR FİLAN KRAL PİYANO GÜVEN VERGİ
EVRİN İNCE AKORT ROTA ORTA OTLAK PARLAK DERE DENE ELDE EDER KENAR EKRAN ENDER ELDEN KARNE KOLLU OLUK
UYKU KULU KUTLU MUTLU KUMLU UMUT GÖRÜ ÖZÜR ÖLÜK ÖZGÜR GÖZLÜK
""".trimIndent().split(Regex("\\s+")).map { it.uppercase(Locale("tr", "TR")) }).toSet()

val EN_BASE_WORDS = listOf(
    "CAT", "STAR", "TIME", "WORD", "GOLD", "DARK", "FIRE", "WIND", "KING", "LION",
    "STONE", "WATER", "HEART", "SMART", "CROWN", "PLANT", "LIGHT", "NIGHT", "BRAVE", "DREAM",
    "PLANET", "CASTLE", "LEGEND", "MASTER", "GARDEN", "BRIDGE", "SILVER", "FOREST", "DRAGON", "WONDER",
    "CAPTAIN", "KINGDOM", "DIAMOND", "THUNDER", "JOURNEY", "FREEDOM", "CRYSTAL", "FORTUNE", "MYSTERY", "VICTORY"
)

val EN_CURATED_DICTIONARY: Set<String> = (EN_BASE_WORDS + """
AT AS AN AM IT IN IS IF ON OR TO DO GO NO SO UP US ME WE BE HE BY MY OF
CAT ACT ART RAT TAR SAT STAR RATS ARTS TARS TIME ITEM EMIT MITE TIE MET
WORD ROW ROD DOW GOLD GOD DOG LOG OLD DARK ARK RAD FIRE IRE FIR WIND WIN
DIN KIN KING GIN LION OIL NIL STONE NOTES TONES ONSET NOTE TONE SENT NEST
NOSE ONE SON NET SET TEN WATER RATE TEAR WEAR WARE WAR EAR EAT TEA RAW
HEART EARTH HATER HEAR HEAT HATE HAT SMART MARTS MARS ARMS MATS ARM MAR
MAT RAM CROWN CORN CROW WORN COW NOW OWN WON PLANET PLANT PLATE PANEL
PLANE LEANT PETAL PLAN LATE TALE PALE LEAP PEAL NEAT TAPE CASTLE SCALE
SLATE STALE TALES LEAST STEAL CAST CATS EAST SEAT SALE SEAL LEGEND LEDGE
GENE NEED EDGE LEND GLEE END DEN LEG GEL MASTER STREAM TAMERS TEAM MEAT
MATE TAME STEAM SEAM REST GARDEN DANGER RANGED GRAND GRADE DEAR DARE READ
NEAR GEAR RAGE RING SILVER LIVERS RIVER LIVE EVIL VEIL VILE FOREST FORTS
FROST STORE FORTE FORT ROSE SORE TORE DRAGON GROAN ORGAN ROAD GONE DONE
NODE WONDER DOWNER WORD DOWN DREW CAPTAIN PAINT ACTIN ANTIC PAIN CANT
KINGDOM DOING DINGO MIND KIND DONG DIAMOND DOMAIN MONAD MAIN MIND THUNDER
HUNTED TURNED UNDER TREND HUNT TURN HURT JOURNEY ENJOY JURY FREED DOME
""".trimIndent().split(Regex("\\s+"))).toSet()

var TR_DICTIONARY: Set<String> = TR_CURATED_DICTIONARY
var TR_COMMON: Set<String> = TR_CURATED_DICTIONARY
var EN_DICTIONARY: Set<String> = EN_CURATED_DICTIONARY
var EN_COMMON: Set<String> = EN_CURATED_DICTIONARY

const val MAX_REWARDED_BONUS_WORDS = 8

fun readWordFile(context: Context, fileName: String, minLen: Int, locale: Locale): Set<String> {
    return try {
        context.assets.open(fileName).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim().uppercase(locale) }.filter { it.length >= minLen }.toSet()
        }
    } catch (_: Exception) {
        emptySet()
    }
}

fun loadWordLists(context: Context) {
    val trLocale = Locale("tr", "TR")
    val tr = readWordFile(context, "tr_words.txt", 2, trLocale)
    if (tr.size > 5000) {
        TR_DICTIONARY = tr + TR_ALL_TIER_WORDS
        TR_COMMON = readWordFile(context, "tr_common.txt", 3, trLocale) + TR_CURATED_DICTIONARY
    }
    val en = readWordFile(context, "en_words.txt", 2, Locale.US)
    if (en.size > 5000) {
        EN_DICTIONARY = en + EN_BASE_WORDS
        EN_COMMON = readWordFile(context, "en_common.txt", 3, Locale.US) + EN_CURATED_DICTIONARY
    }
}

fun canFormWordFromWheel(wheelLetters: String, candidate: String): Boolean {
    if (candidate.length < 2 || candidate.length > wheelLetters.length) return false
    val used = BooleanArray(wheelLetters.length)
    for (ch in candidate) {
        var found = false
        for (i in wheelLetters.indices) {
            if (!used[i] && wheelLetters[i] == ch) {
                used[i] = true
                found = true
                break
            }
        }
        if (!found) return false
    }
    return true
}

// ==========================================
// 4. BULMACA, KELEBEK, SIS & BOSS URETICISI
// ==========================================
data class PlacedWord(val word: String, val row: Int, val col: Int, val isHorizontal: Boolean)

data class LevelPuzzle(
    val level: Int,
    val wheelLetters: List<Char>,
    val placedWords: List<PlacedWord>,
    val bonusWords: Set<String>,
    val gridRows: Int,
    val gridCols: Int,
    val isTimeAttack: Boolean,
    val timeLimitSec: Int,
    val isBossLevel: Boolean,
    val initialSealedWheelIndex: Int?,
    val butterflyCells: Set<Pair<Int, Int>>,
    val goldCells: Set<Pair<Int, Int>>,
    val fogCells: Set<Pair<Int, Int>>
)

fun generateLevelPuzzle(level: Int, lang: Lang): LevelPuzzle {
    val dict = if (lang == Lang.TR) TR_DICTIONARY else EN_DICTIONARY
    val mainWord = if (lang == Lang.TR) {
        when (level) {
            in 1..10 -> TR_WORDS_TIER_1[level - 1]
            in 11..20 -> TR_WORDS_TIER_2[level - 11]
            in 21..30 -> TR_WORDS_TIER_3[level - 21]
            in 31..40 -> TR_WORDS_TIER_4[level - 31]
            in 41..50 -> TR_WORDS_TIER_5[level - 41]
            else -> TR_WORDS_ADVANCED[(level - 51) % TR_WORDS_ADVANCED.size]
        }
    } else EN_BASE_WORDS[(level - 1) % EN_BASE_WORDS.size]

    val allValidWords = dict.filter { canFormWordFromWheel(mainWord, it) }
        .distinct()
        .sortedWith(compareByDescending<String> { it.length }.thenBy { it })

    val rng = Random(level * 1337 + lang.ordinal * 97)
    val targetBoardCount = when {
        level == 1 -> 2
        level <= 10 -> 4
        level <= 30 -> 5
        else -> 6
    }

    val commonWords = if (lang == Lang.TR) TR_COMMON else EN_COMMON
    val subCandidatesAll = allValidWords.filter { it != mainWord && it.length >= 3 }
    val subCandidates = subCandidatesAll.filter { it in commonWords }.shuffled(rng).sortedByDescending { it.length } +
        subCandidatesAll.filter { it !in commonWords }.shuffled(rng).sortedByDescending { it.length }

    val twoLetterCandidates = allValidWords.filter { it != mainWord && it.length == 2 }.shuffled(rng)

    val selectedForBoard = mutableListOf(mainWord)
    for (w in subCandidates) if (selectedForBoard.size < targetBoardCount) selectedForBoard.add(w)
    for (w in twoLetterCandidates) if (selectedForBoard.size < targetBoardCount) selectedForBoard.add(w)
    if (selectedForBoard.size < 2 && mainWord.length >= 3) selectedForBoard.add(mainWord.substring(0, 3))

    val boardSize = 20
    val grid = Array(boardSize) { CharArray(boardSize) { ' ' } }
    val placed = mutableListOf<PlacedWord>()

    val startRow = boardSize / 2
    val startCol = (boardSize - mainWord.length) / 2
    for (i in mainWord.indices) grid[startRow][startCol + i] = mainWord[i]
    placed.add(PlacedWord(mainWord, startRow, startCol, true))

    var fallbackRow = startRow + 2
    for (w in selectedForBoard.drop(1)) {
        var isPlaced = false
        outer@ for (pw in placed) {
            val tryH = !pw.isHorizontal
            for (i in w.indices) {
                for (j in pw.word.indices) {
                    if (w[i] == pw.word[j]) {
                        val r = if (tryH) pw.row + j else pw.row - i
                        val c = if (tryH) pw.col - i else pw.col + j
                        if (canPlaceWord(grid, w, r, c, tryH)) {
                            for (k in w.indices) grid[if (tryH) r else r + k][if (tryH) c + k else c] = w[k]
                            placed.add(PlacedWord(w, r, c, tryH))
                            isPlaced = true
                            break@outer
                        }
                    }
                }
            }
        }
        if (!isPlaced) {
            while (fallbackRow < boardSize - 1 && !canPlaceWord(grid, w, fallbackRow, 2, true)) fallbackRow++
            if (fallbackRow < boardSize - 1) {
                for (k in w.indices) grid[fallbackRow][2 + k] = w[k]
                placed.add(PlacedWord(w, fallbackRow, 2, true))
                fallbackRow += 2
            }
        }
    }

    var minR = boardSize
    var maxR = 0
    var minC = boardSize
    var maxC = 0
    for (pw in placed) {
        for (k in pw.word.indices) {
            val r = if (pw.isHorizontal) pw.row else pw.row + k
            val c = if (pw.isHorizontal) pw.col + k else pw.col
            if (r < minR) minR = r
            if (r > maxR) maxR = r
            if (c < minC) minC = c
            if (c > maxC) maxC = c
        }
    }

    val normalizedPlaced = placed.map { it.copy(row = it.row - minR, col = it.col - minC) }
    val boardWordSet = normalizedPlaced.map { it.word }.toSet()
    val bonusSet = allValidWords.filter { it !in boardWordSet }.toSet()

    var wheelChars = mainWord.toList().shuffled(rng)
    if (wheelChars.joinToString("") == mainWord && mainWord.length > 2) wheelChars = wheelChars.reversed()

    val allCoords = mutableListOf<Pair<Int, Int>>()
    for (pw in normalizedPlaced) {
        for (k in pw.word.indices) {
            val pos = (if (pw.isHorizontal) pw.row else pw.row + k) to (if (pw.isHorizontal) pw.col + k else pw.col)
            if (pos !in allCoords) allCoords.add(pos)
        }
    }
    val shuffledCoords = allCoords.shuffled(rng)
    val butterflySet = if (level >= 2 && shuffledCoords.isNotEmpty()) setOf(shuffledCoords[0]) else emptySet()
    val goldSet = if (level >= 2 && shuffledCoords.size >= 3) setOf(shuffledCoords[1], shuffledCoords[2]) else emptySet()
    val fogSet = if (level >= 3 && shuffledCoords.size >= 5) setOf(shuffledCoords[3], shuffledCoords[4]) else emptySet()

    val isBossLevel = (level % 5 == 0)
    var sealedIdx: Int? = null
    if (isBossLevel) {
        for (candidateIdx in wheelChars.indices) {
            val remStr = wheelChars.filterIndexed { i, _ -> i != candidateIdx }.joinToString("")
            if (normalizedPlaced.any { pw -> canFormWordFromWheel(remStr, pw.word) }) {
                sealedIdx = candidateIdx
                break
            }
        }
    }

    val isTimeAttack = (level % 7 == 0) && !isBossLevel
    val timeLimit = if (isTimeAttack) (45 + normalizedPlaced.size * 8) else 0

    return LevelPuzzle(
        level,
        wheelChars,
        normalizedPlaced,
        bonusSet,
        (maxR - minR + 1).coerceAtLeast(1),
        (maxC - minC + 1).coerceAtLeast(1),
        isTimeAttack,
        timeLimit,
        isBossLevel,
        sealedIdx,
        butterflySet,
        goldSet,
        fogSet
    )
}

private fun canPlaceWord(grid: Array<CharArray>, word: String, r: Int, c: Int, isHoriz: Boolean): Boolean {
    val size = grid.size
    if (r < 1 || c < 1) return false
    if (isHoriz && c + word.length >= size - 1) return false
    if (!isHoriz && r + word.length >= size - 1) return false
    if (isHoriz && (grid[r][c - 1] != ' ' || grid[r][c + word.length] != ' ')) return false
    if (!isHoriz && (grid[r - 1][c] != ' ' || grid[r + word.length][c] != ' ')) return false

    var newCellCount = 0
    for (i in word.indices) {
        val nr = if (isHoriz) r else r + i
        val nc = if (isHoriz) c + i else c
        val cur = grid[nr][nc]
        if (cur != ' ' && cur != word[i]) return false
        if (cur != ' ') {
            val prevSame = if (isHoriz) grid[nr][nc - 1] else grid[nr - 1][nc]
            val nextSame = if (isHoriz) grid[nr][nc + 1] else grid[nr + 1][nc]
            if (i > 0 && prevSame != ' ') return false
            if (i < word.lastIndex && nextSame != ' ') return false
        } else {
            newCellCount++
            if (isHoriz && (grid[nr - 1][nc] != ' ' || grid[nr + 1][nc] != ' ')) return false
            if (!isHoriz && (grid[nr][nc - 1] != ' ' || grid[nr][nc + 1] != ' ')) return false
        }
    }
    return newCellCount > 0
}

// ==========================================
// 5. ANA UYGULAMA YONETICISI
// ==========================================
enum class Screen { MENU, GAME }

@Composable
fun WordGameApp(adMobManager: AdMobManager, onExitApp: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kelime_efsanesi_prefs", Context.MODE_PRIVATE) }
    val soundEngine = remember { ProceduralSoundEngine(context.applicationContext) }

    val appVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.2"
        } catch (_: Exception) {
            "2.2"
        }
    }

    val defaultLangCode = remember { if (Locale.getDefault().language.lowercase().startsWith("tr")) "tr" else "en" }
    var lang by remember { mutableStateOf(if ((prefs.getString("lang", defaultLangCode) ?: defaultLangCode) == "tr") Lang.TR else Lang.EN) }
    var currentLevel by remember { mutableIntStateOf(prefs.getInt("level", 1).coerceIn(1, 300)) }
    var gold by remember { mutableIntStateOf(prefs.getInt("gold", 200)) }
    var bonusJarCount by remember { mutableIntStateOf(prefs.getInt("bonus_jar", 0).coerceIn(0, 4)) }
    var freeHammers by remember { mutableIntStateOf(prefs.getInt("free_hammers", 1)) }
    var musicEnabled by remember { mutableStateOf(prefs.getBoolean("music", true)) }
    var sfxEnabled by remember { mutableStateOf(prefs.getBoolean("sfx", true)) }
    var highGraphics by remember { mutableStateOf(prefs.getBoolean("graphics", true)) }

    // Gunluk Cark Siniri (Gunde 1 Ucretsiz + En Fazla 2 Reklamli = Toplam 3 Hak)
    val todayDayNumber = remember { currentDayNumber() }
    var lastSpinDay by remember { mutableIntStateOf(prefs.getInt("spin_day", -1)) }
    var spinsUsedToday by remember {
        val savedDay = prefs.getInt("spin_day", -1)
        val savedCount = prefs.getInt("spin_count_today", 0)
        mutableIntStateOf(if (savedDay == todayDayNumber) savedCount else 0)
    }

    var screen by remember { mutableStateOf(Screen.MENU) }
    var showSettings by remember { mutableStateOf(false) }
    var showMuseumDialog by remember { mutableStateOf(false) }
    var showSpinWheelDialog by remember { mutableStateOf(false) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    fun saveState() {
        prefs.edit()
            .putString("lang", lang.code)
            .putInt("level", currentLevel)
            .putInt("gold", gold)
            .putInt("bonus_jar", bonusJarCount)
            .putInt("free_hammers", freeHammers)
            .putBoolean("music", musicEnabled)
            .putBoolean("sfx", sfxEnabled)
            .putBoolean("graphics", highGraphics)
            .putInt("spin_day", lastSpinDay)
            .putInt("spin_count_today", spinsUsedToday)
            .apply()
    }

    LaunchedEffect(musicEnabled, sfxEnabled) {
        soundEngine.musicEnabled = musicEnabled
        soundEngine.sfxEnabled = sfxEnabled
        soundEngine.updateBackgroundMusic()
        saveState()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    soundEngine.isAppInForeground = true
                    soundEngine.updateBackgroundMusic()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    soundEngine.isAppInForeground = false
                    soundEngine.updateBackgroundMusic()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val worldTheme = getWorldThemeForLevel(currentLevel)

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = worldTheme.bgGradient))) {
        PuzzleWordBackground(highGraphics = highGraphics, accentColor = worldTheme.accentColor)

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            when (screen) {
                Screen.MENU -> MainMenuScreen(
                    lang = lang,
                    level = currentLevel,
                    gold = gold,
                    spinsRemainingToday = (3 - spinsUsedToday).coerceAtLeast(0),
                    versionName = appVersionName,
                    worldTheme = worldTheme,
                    onToggleLang = {
                        soundEngine.playClick()
                        lang = if (lang == Lang.TR) Lang.EN else Lang.TR
                        saveState()
                    },
                    onStartGame = { soundEngine.playClick(); screen = Screen.GAME },
                    onOpenSpinWheel = { soundEngine.playClick(); showSpinWheelDialog = true },
                    onOpenMuseum = { soundEngine.playClick(); showMuseumDialog = true },
                    onOpenSettings = { soundEngine.playClick(); showSettings = true },
                    onExit = { soundEngine.playClick(); onExitApp() }
                )

                Screen.GAME -> {
                    BackHandler { soundEngine.playClick(); screen = Screen.MENU }
                    GameplayScreen(
                        lang = lang,
                        level = currentLevel,
                        gold = gold,
                        bonusJarCount = bonusJarCount,
                        freeHammers = freeHammers,
                        worldTheme = worldTheme,
                        soundEngine = soundEngine,
                        adMobManager = adMobManager,
                        onUpdateEconomy = { newGold, newJar, newHammers ->
                            gold = newGold
                            bonusJarCount = newJar
                            freeHammers = newHammers
                            saveState()
                        },
                        onLevelCompleted = { earnedGold ->
                            val finishedLvl = currentLevel
                            gold += earnedGold
                            currentLevel = if (currentLevel < 300) currentLevel + 1 else 1
                            saveState()
                            // Ilk 5 bolumde cikmaz, sonrasinda en az 3 dk arayla gecis reklami gosterir
                            adMobManager.maybeShowInterstitial(finishedLvl) {}
                        },
                        onOpenSpinWheel = { soundEngine.playClick(); showSpinWheelDialog = true },
                        onOpenMuseum = { soundEngine.playClick(); showMuseumDialog = true },
                        onBackToMenu = { soundEngine.playClick(); screen = Screen.MENU },
                        onOpenSettings = { soundEngine.playClick(); showSettings = true }
                    )
                }
            }

            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
            ) {
                LaunchedEffect(bannerMessage) {
                    if (bannerMessage != null) {
                        delay(2500)
                        bannerMessage = null
                    }
                }
                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
                    Text(
                        text = bannerMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }

        if (showSpinWheelDialog) {
            val today = currentDayNumber()
            if (lastSpinDay != today) {
                spinsUsedToday = 0
            }
            SpinWheelDialog(
                lang = lang,
                spinsUsedToday = spinsUsedToday,
                soundEngine = soundEngine,
                adMobManager = adMobManager,
                onPrizeWon = { goldDelta, hammerDelta ->
                    gold = (gold + goldDelta).coerceAtLeast(0)
                    freeHammers += hammerDelta
                    lastSpinDay = today
                    spinsUsedToday += 1
                    saveState()
                },
                onDismiss = { soundEngine.playClick(); showSpinWheelDialog = false }
            )
        }

        if (showMuseumDialog) {
            MuseumBuilderDialog(
                lang = lang,
                currentLevel = currentLevel,
                worldTheme = worldTheme,
                onDismiss = { soundEngine.playClick(); showMuseumDialog = false }
            )
        }

        if (showSettings) {
            SettingsDialog(
                lang = lang,
                musicEnabled = musicEnabled,
                sfxEnabled = sfxEnabled,
                highGraphics = highGraphics,
                onToggleMusic = { soundEngine.playClick(); musicEnabled = !musicEnabled },
                onToggleSfx = { sfxEnabled = !sfxEnabled; soundEngine.sfxEnabled = sfxEnabled; soundEngine.playClick() },
                onToggleGraphics = { soundEngine.playClick(); highGraphics = !highGraphics; saveState() },
                onContactEmail = {
                    soundEngine.playClick()
                    adMobManager.openContactEmail()
                },
                privacyOptionsRequired = adMobManager.privacyOptionsRequired,
                onOpenPrivacyOptions = {
                    soundEngine.playClick()
                    adMobManager.showPrivacyOptions()
                },
                onResetProgress = {
                    soundEngine.playClick()
                    currentLevel = 1
                    gold = 200
                    bonusJarCount = 0
                    freeHammers = 1
                    spinsUsedToday = 0
                    saveState()
                    showSettings = false
                },
                onDismiss = { soundEngine.playClick(); showSettings = false }
            )
        }
    }
}

// ==========================================
// 6. ARKA PLAN, SIMSEK VE KONFETI EFEKTLERI
// ==========================================
@Composable
fun PuzzleWordBackground(highGraphics: Boolean, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val floatOffset by if (highGraphics) {
        infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "f")
    } else remember { mutableFloatStateOf(0.3f) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val tileSize = size.width / 7f
        for (r in 0..(size.height / tileSize).toInt()) {
            for (c in 0..7) {
                if ((r + c) % 3 == 0) {
                    val shift = if (highGraphics) sin((r + c + floatOffset * 6f).toDouble()).toFloat() * 6f else 0f
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.055f),
                        topLeft = Offset(c * tileSize + 6f, r * tileSize + shift),
                        size = Size(tileSize - 12f, tileSize - 12f),
                        cornerRadius = CornerRadius(14f, 14f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun LightningFlashOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF38BDF8).copy(alpha = 0.24f))
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.52f)
            lineTo(w * 0.55f, h * 0.48f)
            lineTo(w * 0.46f, h * 0.26f)
        }
        drawPath(path = path, color = Color(0xFFFDE047), style = Stroke(width = 12f, cap = StrokeCap.Round))
    }
}

@Composable
fun ConfettiEffectOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "c")
    val colors = remember { listOf(Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFFA78BFA), Color(0xFFFB7185)) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until 45) {
            val seed = i * 37
            val xBase = ((seed * 73) % 100) / 100f * size.width
            val speed = 0.6f + ((seed % 5) * 0.15f)
            val yPos = ((progress * speed + (seed % 10) * 0.1f) % 1f) * size.height
            val drift = sin((progress * 6f + i).toDouble()).toFloat() * 24f
            drawCircle(color = colors[i % colors.size], radius = (8 + (i % 5) * 2).toFloat(), center = Offset(xBase + drift, yPos))
        }
    }
}

// ==========================================
// 7. ANA MENU EKRANI (ALT BANNER REKLAM DESTEKLI)
// ==========================================
@Composable
fun MainMenuScreen(
    lang: Lang,
    level: Int,
    gold: Int,
    spinsRemainingToday: Int,
    versionName: String,
    worldTheme: WorldTheme,
    onToggleLang: () -> Unit,
    onStartGame: () -> Unit,
    onOpenSpinWheel: () -> Unit,
    onOpenMuseum: () -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    val league = getLeagueForLevel(level)
    val buildStep = ((level - 1) % 10) + 1
    val buildPercent = buildStep * 10

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onToggleLang, shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.14f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(lang.flag, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(lang.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E293B).copy(alpha = 0.85f), border = BorderStroke(1.dp, Color(0xFFFBBF24))) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("$gold", color = Color(0xFFFBBF24), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val logoLetters = if (lang == Lang.TR) listOf('K', 'E', 'L', 'İ', 'M', 'E') else listOf('W', 'O', 'R', 'D', 'S')
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                logoLetters.forEachIndexed { idx, ch ->
                    Box(
                        modifier = Modifier.size(44.dp).shadow(6.dp, RoundedCornerShape(10.dp))
                            .background(Brush.verticalGradient(if (idx % 2 == 0) listOf(Color(0xFFF59E0B), Color(0xFFD97706)) else worldTheme.openTileGradient), RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(ch.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (lang == Lang.TR) "🗺️ BÖLGE: ${worldTheme.regionNameTr.uppercase()}" else "🗺️ ZONE: ${worldTheme.regionNameEn.uppercase()}",
                color = worldTheme.accentColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = onOpenMuseum,
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, league.color.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(league.icon, fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (lang == Lang.TR) league.nameTr else league.nameEn, color = league.color, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(
                                text = if (lang == Lang.TR) "🏗️ ${worldTheme.monumentNameTr} İnşası: %$buildPercent" else "🏗️ Monument Build: $buildPercent%",
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth(0.88f), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MenuButton(if (lang == Lang.TR) "▶  BAŞLA (SEVİYE $level)" else "▶  PLAY (LEVEL $level)", listOf(Color(0xFF10B981), Color(0xFF059669)), 56.dp, 18, onStartGame)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    MenuButton(
                        text = if (lang == Lang.TR) "🎡 Çark ($spinsRemainingToday/3)" else "🎡 Spin ($spinsRemainingToday/3)",
                        gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                        height = 48.dp,
                        fontSize = 13,
                        onClick = onOpenSpinWheel
                    )
                }
                Box(Modifier.weight(1f)) {
                    MenuButton(
                        text = if (lang == Lang.TR) "🏗️ Müzemi İnşa Et" else "🏗️ My Museum",
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                        height = 48.dp,
                        fontSize = 13,
                        onClick = onOpenMuseum
                    )
                }
            }
            MenuButton(if (lang == Lang.TR) "⚙️  AYARLAR" else "⚙️  SETTINGS", listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), 48.dp, 15, onOpenSettings)
            MenuButton(if (lang == Lang.TR) "🚪  ÇIKIŞ YAP" else "🚪  EXIT GAME", listOf(Color(0xFFEF4444), Color(0xFFB91C1C)), 46.dp, 14, onExit)
        }

        // Sadece Ana Menude Banner Reklam (Oyun icinde carkin yaninda yanlislikla tiklanmamasi icin)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MenuBannerAdView()
            Spacer(Modifier.height(4.dp))
            Text("Galaxy Store & Play Edition • v$versionName", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
        }
    }
}

@Composable
fun MenuButton(text: String, gradient: List<Color>, height: Dp, fontSize: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(height).shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(gradient))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = fontSize.sp) }
}

// ==========================================
// 8. OYUN ICI EKRANI (ODULLU REKLAM DESTEKLI)
// ==========================================
@Composable
fun GameplayScreen(
    lang: Lang,
    level: Int,
    gold: Int,
    bonusJarCount: Int,
    freeHammers: Int,
    worldTheme: WorldTheme,
    soundEngine: ProceduralSoundEngine,
    adMobManager: AdMobManager,
    onUpdateEconomy: (Int, Int, Int) -> Unit,
    onLevelCompleted: (Int) -> Unit,
    onOpenSpinWheel: () -> Unit,
    onOpenMuseum: () -> Unit,
    onBackToMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val puzzle = remember(level, lang) { generateLevelPuzzle(level, lang) }
    var wheelLetters by remember(puzzle) { mutableStateOf(puzzle.wheelLetters) }
    var solvedWords by remember(puzzle) { mutableStateOf(setOf<String>()) }
    var revealedCells by remember(puzzle) { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var foundBonusWords by remember(puzzle) { mutableStateOf(setOf<String>()) }
    var comboCount by remember(puzzle) { mutableIntStateOf(0) }

    var activeButterflies by remember(puzzle) { mutableStateOf(puzzle.butterflyCells) }
    var collectedGoldCells by remember(puzzle) { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var clearedFogCells by remember(puzzle) { mutableStateOf(setOf<Pair<Int, Int>>()) }

    var feverProgress by remember(puzzle) { mutableFloatStateOf(0.15f) }
    val isFeverMode = comboCount >= 3 && feverProgress > 0.2f
    var showLightningFlash by remember { mutableStateOf(false) }

    var isHammerMode by remember(puzzle) { mutableStateOf(false) }
    var isMemoryMode by remember(puzzle) { mutableStateOf(false) }
    var memoryHideLetters by remember(puzzle) { mutableStateOf(false) }
    var selectedDictionaryWord by remember { mutableStateOf<String?>(null) }

    var showBonusJarDialog by remember { mutableStateOf(false) }
    var sealedWheelIndex by remember(puzzle) { mutableStateOf(puzzle.initialSealedWheelIndex) }

    var selectedIndices by remember(puzzle) { mutableStateOf(listOf<Int>()) }
    var currentTouchPos by remember(puzzle) { mutableStateOf<Offset?>(null) }

    var praisePopup by remember { mutableStateOf<String?>(null) }
    var winPraiseTitle by remember(puzzle) { mutableStateOf("") }
    var winRewardDoubled by remember(puzzle) { mutableStateOf(false) }

    var remainingSeconds by remember(puzzle) { mutableIntStateOf(puzzle.timeLimitSec) }
    var isTimeOut by remember(puzzle) { mutableStateOf(false) }
    var showWinDialog by remember(puzzle) { mutableStateOf(false) }
    var unlockedFact by remember(puzzle) { mutableStateOf<HistoricalFact?>(null) }
    var promotedLeague by remember(puzzle) { mutableStateOf<LeagueInfo?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isMemoryMode) {
        if (isMemoryMode) {
            memoryHideLetters = false
            feedbackBanner = if (lang == Lang.TR) "🧠 HAFIZA MODU: Harflerin yerini ezberle (3sn)... 2x Altın!" else "🧠 MEMORY MODE: Memorize letters (3s)... 2x Gold!"
            delay(3200)
            if (isMemoryMode) memoryHideLetters = true
        } else {
            memoryHideLetters = false
        }
    }

    LaunchedEffect(puzzle, showWinDialog) {
        while (!showWinDialog) {
            delay(120)
            if (feverProgress > 0.08f) {
                feverProgress = (feverProgress - 0.011f).coerceAtLeast(0.08f)
                if (feverProgress <= 0.09f) comboCount = 0
            }
        }
    }

    val cellMap = remember(puzzle, solvedWords, revealedCells) {
        val map = mutableMapOf<Pair<Int, Int>, Pair<Char, Boolean>>()
        for (pw in puzzle.placedWords) {
            val isWordSolved = pw.word in solvedWords
            for (k in pw.word.indices) {
                val pos = (if (pw.isHorizontal) pw.row else pw.row + k) to (if (pw.isHorizontal) pw.col + k else pw.col)
                val prevOpen = map[pos]?.second ?: false
                map[pos] = pw.word[k] to (prevOpen || isWordSolved || (pos in revealedCells))
            }
        }
        map
    }

    LaunchedEffect(cellMap) {
        var extraGold = 0
        var msg: String? = null
        for ((pos, data) in cellMap) {
            if (data.second) {
                if (pos in activeButterflies) {
                    activeButterflies = activeButterflies - pos
                    extraGold += 25
                    msg = if (lang == Lang.TR) "🦋 KELEBEK YAKALANDI! +25 🪙" else "🦋 BUTTERFLY CAUGHT! +25 🪙"
                }
                if (pos in puzzle.goldCells && pos !in collectedGoldCells) {
                    collectedGoldCells = collectedGoldCells + pos
                    extraGold += 15
                    if (msg == null) msg = if (lang == Lang.TR) "🪙 ALTIN KUTU AÇILDI! +15 🪙" else "🪙 GOLD TILE! +15 🪙"
                }
                if (pos in puzzle.fogCells && pos !in clearedFogCells) {
                    clearedFogCells = clearedFogCells + pos
                    extraGold += 20
                    if (msg == null) msg = if (lang == Lang.TR) "🌫️ GİZEMLİ SİS DAĞILDI! +20 🪙" else "🌫️ MYSTERY FOG CLEARED! +20 🪙"
                }
            }
        }
        if (extraGold > 0) {
            onUpdateEconomy(gold + extraGold, bonusJarCount, freeHammers)
            if (msg != null) praisePopup = msg
        }
    }

    val allBoardCellsOpen = remember(cellMap) { cellMap.isNotEmpty() && cellMap.values.all { it.second } }

    LaunchedEffect(allBoardCellsOpen) {
        if (allBoardCellsOpen && !showWinDialog) {
            delay(350)
            soundEngine.playLevelWin()
            winPraiseTitle = if (lang == Lang.TR) LEVEL_WIN_PRAISE_TR.random() else LEVEL_WIN_PRAISE_EN.random()
            if (level % 10 == 0) {
                unlockedFact = HISTORICAL_FACTS.find { it.levelMilestone == level }
                    ?: HISTORICAL_FACTS[(level / 10 - 1) % HISTORICAL_FACTS.size].copy(levelMilestone = level)
                promotedLeague = getLeagueForLevel(level + 1)
            }
            showWinDialog = true
        }
    }

    LaunchedEffect(puzzle, solvedWords.size, isTimeOut, showWinDialog) {
        if (puzzle.isTimeAttack && !showWinDialog && !isTimeOut) {
            while (remainingSeconds > 0 && !allBoardCellsOpen) {
                delay(1000)
                // Tam ekran reklam acikken sure akmaz
                if (!adMobManager.isAdShowing) remainingSeconds--
            }
            if (remainingSeconds <= 0 && !allBoardCellsOpen) {
                isTimeOut = true
                soundEngine.playError()
            }
        }
    }

    LaunchedEffect(showLightningFlash) {
        if (showLightningFlash) {
            delay(650)
            showLightningFlash = false
        }
    }

    fun breakBossSealIfNeeded() {
        if (sealedWheelIndex != null) {
            sealedWheelIndex = null
            soundEngine.playLightningStrike()
            praisePopup = if (lang == Lang.TR) "💥 MUHAFIZ MÜHRÜ KIRILDI!" else "💥 BOSS SEAL SHATTERED!"
        }
    }

    fun submitSwipedWord() {
        if (selectedIndices.isEmpty()) return
        val candidate = selectedIndices.map { wheelLetters[it] }.joinToString("")
        selectedIndices = emptyList()
        currentTouchPos = null

        val matchingPlaced = puzzle.placedWords.filter { it.word == candidate }
        when {
            matchingPlaced.isNotEmpty() && candidate !in solvedWords -> {
                soundEngine.playWordSuccess()
                solvedWords = solvedWords + candidate
                comboCount++
                feverProgress = (feverProgress + 0.42f).coerceAtMost(1f)

                val memoryBonus = if (isMemoryMode) 20 else 0
                val feverBonus = if (comboCount >= 3) 10 else 0
                if (memoryBonus + feverBonus > 0) {
                    onUpdateEconomy(gold + memoryBonus + feverBonus, bonusJarCount, freeHammers)
                }

                if (sealedWheelIndex != null) breakBossSealIfNeeded()
                else if (isMemoryMode) praisePopup = if (lang == Lang.TR) "🧠 HAFIZA ŞOV! +20 🪙" else "🧠 MEMORY GENIUS! +20 🪙"
                else praisePopup = if (lang == Lang.TR) PRAISE_WORDS_TR.random() else PRAISE_WORDS_EN.random()

                if (comboCount >= 4 && comboCount % 4 == 0) {
                    val closed = cellMap.filter { !it.value.second }.keys.toList()
                    if (closed.isNotEmpty()) {
                        soundEngine.playLightningStrike()
                        showLightningFlash = true
                        revealedCells = revealedCells + closed.random()
                        feedbackBanner = if (lang == Lang.TR) "⚡ ALEV ŞİMŞEĞİ 1 KUTU PATLATTI!" else "⚡ LIGHTNING OPENED A TILE!"
                    }
                }
            }
            candidate in solvedWords -> {
                soundEngine.playError()
                feedbackBanner = if (lang == Lang.TR) "Bu kelimeyi zaten buldun!" else "Word already found!"
            }
            candidate in puzzle.bonusWords && candidate !in foundBonusWords -> {
                foundBonusWords = foundBonusWords + candidate
                feverProgress = (feverProgress + 0.25f).coerceAtMost(1f)
                if (foundBonusWords.size > MAX_REWARDED_BONUS_WORDS) {
                    soundEngine.playBonusWord()
                    feedbackBanner = if (lang == Lang.TR) "✨ Geçerli kelime ($candidate) — bu bölümün bonus ödülü doldu"
                    else "✨ Valid word ($candidate) — bonus rewards maxed for this level"
                } else {
                    val nextJar = bonusJarCount + 1
                    if (nextJar >= 5) {
                        soundEngine.playLevelWin()
                        onUpdateEconomy(gold + 100, 0, freeHammers)
                        praisePopup = if (lang == Lang.TR) "🏺 ANTİK KÜP PATLADI! +100 🪙" else "🏺 BONUS JAR FULL! +100 🪙"
                    } else {
                        soundEngine.playBonusWord()
                        onUpdateEconomy(gold + 10, nextJar, freeHammers)
                        feedbackBanner = if (lang == Lang.TR) "🏺 Bonus ($candidate)! Küp: $nextJar/5 (+10 🪙)" else "🏺 Bonus ($candidate)! Jar: $nextJar/5 (+10 🪙)"
                    }
                }
            }
            candidate.length >= 2 -> {
                soundEngine.playError()
                comboCount = 0
                feverProgress = 0.08f
                if (activeButterflies.isNotEmpty()) {
                    activeButterflies = emptySet()
                    feedbackBanner = if (lang == Lang.TR) "🦋 Yanlış hamle! Kelebek uçup kaçtı!" else "🦋 Wrong move! Butterfly flew away!"
                }
            }
        }
    }

    val league = getLeagueForLevel(level)
    val buildPercent = (((level - 1) % 10) + 1) * 10

    Box(modifier = Modifier.fillMaxSize().then(if (isFeverMode) Modifier.border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp)) else Modifier)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        IconButton(onClick = onBackToMenu, modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.12f), CircleShape)) {
                            Text("⬅", fontSize = 16.sp, color = Color.White)
                        }
                        Surface(
                            onClick = onOpenSpinWheel,
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24))
                        ) {
                            Text("🎡", fontSize = 15.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                        }
                        Surface(
                            onClick = onOpenMuseum,
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFC4B5FD))
                        ) {
                            Text("🏗️%$buildPercent", color = Color(0xFFE9D5FF), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(league.icon, fontSize = 14.sp)
                            Spacer(Modifier.width(3.dp))
                            Text(if (lang == Lang.TR) "SEVİYE $level" else "LEVEL $level", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                        Text(if (lang == Lang.TR) worldTheme.regionNameTr else worldTheme.regionNameEn, color = worldTheme.accentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), border = BorderStroke(1.dp, Color(0xFFFBBF24))) {
                            Text("🪙$gold", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                        }
                        // Odullu Reklam ile +50 Altin Destek Butonu
                        Surface(
                            onClick = {
                                soundEngine.playClick()
                                adMobManager.showRewardedAd(
                                    onRewardEarned = {
                                        soundEngine.playLevelWin()
                                        onUpdateEconomy(gold + 50, bonusJarCount, freeHammers)
                                        praisePopup = if (lang == Lang.TR) "🎬 REKLAM ÖDÜLÜ: +50 🪙!" else "🎬 AD REWARD: +50 🪙!"
                                    },
                                    onNotReady = {
                                        feedbackBanner = if (lang == Lang.TR) "⏳ Reklam yükleniyor, birkaç saniye sonra tekrar dene!" else "⏳ Ad loading, try again in a moment!"
                                    }
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF065F46),
                            border = BorderStroke(1.dp, Color(0xFF34D399))
                        ) {
                            Text("🎬+50", color = Color(0xFFA7F3D0), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                        }
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.12f), CircleShape)) {
                            Text("⚙️", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.96f)
                        .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFeverMode) "🔥 ALEV 2x!" else "🔥 Kombo x$comboCount",
                        color = if (isFeverMode) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.14f))) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(feverProgress.coerceIn(0.08f, 1f)).background(
                                Brush.horizontalGradient(if (isFeverMode) listOf(Color(0xFFF59E0B), Color(0xFFEF4444)) else listOf(Color(0xFF38BDF8), Color(0xFF10B981)))
                            )
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("⚡", fontSize = 13.sp)
                }

                if (puzzle.isBossLevel) {
                    Spacer(Modifier.height(4.dp))
                    Surface(color = Color(0xFF7F1D1D).copy(alpha = 0.9f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFF87171)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (lang == Lang.TR) worldTheme.bossTitleTr else worldTheme.bossTitleEn, color = Color(0xFFFECACA), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Text(
                                text = if (sealedWheelIndex != null) (if (lang == Lang.TR) "🪨 1 Kelime Bul -> Mührü Kır!" else "🪨 Find 1 Word -> Unseal!") else (if (lang == Lang.TR) "💥 Mühür Kırıldı!" else "💥 Seal Broken!"),
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (puzzle.isTimeAttack) {
                    Text(
                        text = if (lang == Lang.TR) "⏱️ ZAMANA KARŞI YARIŞ: ${remainingSeconds}sn" else "⏱️ TIME ATTACK: ${remainingSeconds}s",
                        color = if (remainingSeconds <= 10) Color(0xFFEF4444) else Color(0xFFFBBF24),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            AnimatedVisibility(visible = isHammerMode) {
                Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = if (lang == Lang.TR) "🔨 ÇEKİÇ HAZIR: Yukarıda kırmak istediğin kapalı kutuya dokun!" else "🔨 HAMMER READY: Tap any closed tile to smash it!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
                val cellW = (maxWidth / (puzzle.gridCols + 1).coerceAtLeast(4)).coerceAtMost(45.dp)
                val cellH = (maxHeight / (puzzle.gridRows + 1).coerceAtLeast(4)).coerceAtMost(45.dp)
                val cellSize = minOf(cellW, cellH).coerceAtLeast(22.dp)

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until puzzle.gridRows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (c in 0 until puzzle.gridCols) {
                                val pos = r to c
                                val cellData = cellMap[pos]
                                if (cellData != null) {
                                    val (ch, isOpen) = cellData
                                    val hasButterfly = pos in activeButterflies && !isOpen
                                    val hasGold = pos in puzzle.goldCells && pos !in collectedGoldCells && !isOpen
                                    val hasFog = pos in puzzle.fogCells && pos !in clearedFogCells && !isOpen

                                    val popScale by animateFloatAsState(
                                        targetValue = if (isOpen) 1f else if (isHammerMode) 0.96f else 0.92f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                        label = "pop"
                                    )
                                    Box(
                                        modifier = Modifier.size(cellSize).scale(popScale).shadow(if (isOpen) 6.dp else 2.dp, RoundedCornerShape(9.dp))
                                            .background(
                                                when {
                                                    isOpen -> Brush.verticalGradient(worldTheme.openTileGradient)
                                                    hasFog -> Brush.verticalGradient(listOf(Color(0xFF4C1D95), Color(0xFF2E1065)))
                                                    else -> Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                                },
                                                RoundedCornerShape(9.dp)
                                            )
                                            .border(
                                                width = if (isHammerMode && !isOpen) 2.dp else 1.5.dp,
                                                color = when {
                                                    isOpen -> Color.White.copy(alpha = 0.7f)
                                                    isHammerMode -> Color(0xFFFBBF24)
                                                    hasButterfly -> Color(0xFF38BDF8)
                                                    hasGold -> Color(0xFFFBBF24)
                                                    hasFog -> Color(0xFFC084FC)
                                                    else -> Color.White.copy(alpha = 0.25f)
                                                },
                                                shape = RoundedCornerShape(9.dp)
                                            )
                                            .clickable {
                                                if (isHammerMode && !isOpen) {
                                                    soundEngine.playHammerHit()
                                                    if (freeHammers > 0) {
                                                        onUpdateEconomy(gold, bonusJarCount, freeHammers - 1)
                                                    } else {
                                                        onUpdateEconomy(gold - 80, bonusJarCount, freeHammers)
                                                    }
                                                    revealedCells = revealedCells + pos
                                                    isHammerMode = false
                                                    breakBossSealIfNeeded()
                                                } else if (isOpen) {
                                                    val ownerWord = puzzle.placedWords.find { pw ->
                                                        if (pw.isHorizontal) pw.row == r && c in pw.col until (pw.col + pw.word.length)
                                                        else pw.col == c && r in pw.row until (pw.row + pw.word.length)
                                                    }?.word
                                                    if (ownerWord != null) {
                                                        soundEngine.playClick()
                                                        selectedDictionaryWord = ownerWord
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isOpen -> Text(ch.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = (cellSize.value * 0.50f).sp)
                                            isHammerMode -> Text("🎯", fontSize = (cellSize.value * 0.38f).sp)
                                            hasButterfly -> Text("🦋", fontSize = (cellSize.value * 0.42f).sp)
                                            hasGold -> Text("🪙", fontSize = (cellSize.value * 0.38f).sp)
                                            hasFog -> Text("🌫️", fontSize = (cellSize.value * 0.40f).sp)
                                        }
                                    }
                                } else Spacer(Modifier.size(cellSize))
                            }
                        }
                    }
                }
            }

            val currentPreview = selectedIndices.map { wheelLetters[it] }.joinToString("")
            Box(modifier = Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                if (currentPreview.isNotEmpty()) {
                    Surface(color = worldTheme.accentColor, shape = RoundedCornerShape(18.dp), shadowElevation = 6.dp) {
                        Text(currentPreview, color = Color(0xFF0F172A), fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp))
                    }
                } else if (feedbackBanner != null) {
                    LaunchedEffect(feedbackBanner) { delay(1800); feedbackBanner = null }
                    Text(feedbackBanner ?: "", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                } else {
                    Text(
                        text = if (lang == Lang.TR) "💡 Açılan kelimelerin anlamına bakmak için kutulara dokun!" else "💡 Tap opened tiles to see word meanings!",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(onClick = { soundEngine.playClick(); showBonusJarDialog = true }, shape = RoundedCornerShape(14.dp), color = Color(0xFF78350F).copy(alpha = 0.45f), border = BorderStroke(1.dp, Color(0xFFF59E0B))) {
                        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🏺", fontSize = 15.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("$bonusJarCount/5", color = Color(0xFFFDE047), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    }
                    Surface(
                        onClick = {
                            soundEngine.playClick()
                            val currentSealedChar = sealedWheelIndex?.let { wheelLetters[it] }
                            wheelLetters = wheelLetters.shuffled()
                            if (currentSealedChar != null) sealedWheelIndex = wheelLetters.indexOf(currentSealedChar).takeIf { it >= 0 }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) { Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) { Text("🔀", fontSize = 15.sp) } }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Ipucu: Altin yetmezse Odullu Reklam ile bedava Ipucu verir!
                    Surface(
                        onClick = {
                            val unrevealed = cellMap.filter { !it.value.second }.keys.toList()
                            if (unrevealed.isEmpty()) return@Surface
                            if (gold >= 50) {
                                soundEngine.playClick()
                                onUpdateEconomy(gold - 50, bonusJarCount, freeHammers)
                                revealedCells = revealedCells + unrevealed.random()
                                breakBossSealIfNeeded()
                            } else {
                                soundEngine.playClick()
                                adMobManager.showRewardedAd(
                                    onRewardEarned = {
                                        soundEngine.playWordSuccess()
                                        revealedCells = revealedCells + unrevealed.random()
                                        breakBossSealIfNeeded()
                                    },
                                    onNotReady = {
                                        soundEngine.playError()
                                        feedbackBanner = if (lang == Lang.TR) "İpucu için 50 🪙 gerekir (Reklam henüz hazır değil)!" else "Need 50 🪙!"
                                    }
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(if (gold >= 50) "50" else "🎬", color = Color(0xFFFBBF24), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }

                    // Cekic: Altin yetmezse Odullu Reklam ile +1 Bedava Cekic verir!
                    Surface(
                        onClick = {
                            if (isHammerMode) {
                                soundEngine.playClick()
                                isHammerMode = false
                            } else if (freeHammers > 0 || gold >= 80) {
                                soundEngine.playClick()
                                isHammerMode = true
                            } else {
                                soundEngine.playClick()
                                adMobManager.showRewardedAd(
                                    onRewardEarned = {
                                        soundEngine.playLevelWin()
                                        onUpdateEconomy(gold, bonusJarCount, freeHammers + 1)
                                        isHammerMode = true
                                    },
                                    onNotReady = {
                                        soundEngine.playError()
                                        feedbackBanner = if (lang == Lang.TR) "Çekiç için 80 🪙 gerekir (Reklam henüz hazır değil)!" else "Need 80 🪙!"
                                    }
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isHammerMode) Color(0xFFF59E0B) else Color(0xFFEA580C).copy(alpha = 0.28f),
                        border = BorderStroke(1.5.dp, if (isHammerMode) Color.White else Color(0xFFFB923C))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔨", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (freeHammers > 0) "x$freeHammers" else if (gold >= 80) "80" else "🎬",
                                color = if (isHammerMode) Color.Black else Color(0xFFFDBA74),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            soundEngine.playClick()
                            isMemoryMode = !isMemoryMode
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMemoryMode) Color(0xFF10B981) else Color(0xFF8B5CF6).copy(alpha = 0.28f),
                        border = BorderStroke(1.5.dp, if (isMemoryMode) Color.White else Color(0xFFC4B5FD))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🧠", fontSize = 14.sp)
                            Spacer(Modifier.width(3.dp))
                            Text("2x", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            WordSwipeWheel(
                letters = wheelLetters,
                selectedIndices = selectedIndices,
                sealedIndex = sealedWheelIndex,
                hideLettersForMemory = memoryHideLetters,
                currentTouchPos = currentTouchPos,
                comboCount = comboCount,
                accentColor = worldTheme.accentColor,
                onSelectIndex = { idx ->
                    if (idx == sealedWheelIndex) {
                        soundEngine.playError()
                        feedbackBanner = if (lang == Lang.TR) "🗿 Bu harf mühürlü! Önce diğer harflerle 1 kelime bul!" else "🗿 Sealed! Find 1 word with other letters first!"
                    } else if (idx !in selectedIndices) {
                        selectedIndices = selectedIndices + idx
                        soundEngine.playSwipeNote(selectedIndices.size - 1)
                    }
                },
                onUpdateTouch = { pos -> currentTouchPos = pos },
                onDragEnd = { submitSwipedWord() }
            )
        }

        if (showLightningFlash) LightningFlashOverlay()

        AnimatedVisibility(
            visible = praisePopup != null,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp)
        ) {
            LaunchedEffect(praisePopup) {
                if (praisePopup != null) {
                    delay(1150)
                    praisePopup = null
                }
            }
            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF0F172A).copy(alpha = 0.94f), border = BorderStroke(2.dp, Color(0xFFFBBF24)), shadowElevation = 12.dp) {
                Text(praisePopup ?: "", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 19.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
            }
        }
    }

    selectedDictionaryWord?.let { word ->
        Dialog(onDismissRequest = { selectedDictionaryWord = null }) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF1E293B), border = BorderStroke(2.dp, Color(0xFF38BDF8))) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖 $word", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(getWordMeaning(word, lang), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { selectedDictionaryWord = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) {
                        Text(if (lang == Lang.TR) "Anladım" else "Got It", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showBonusJarDialog) {
        Dialog(onDismissRequest = { showBonusJarDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1E293B), border = BorderStroke(2.dp, Color(0xFFF59E0B))) {
                Column(modifier = Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏺", fontSize = 42.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(if (lang == Lang.TR) "ANTİK KELİME KÜPÜ ($bonusJarCount / 5)" else "ANCIENT BONUS JAR ($bonusJarCount / 5)", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (lang == Lang.TR) "Tabloda olmayan geçerli kelimeler buldukça küp dolar. Her 5 bonus kelimede küp kırılır ve +100 🪙 verir!"
                        else "Every 5 bonus words breaks the jar for +100 🪙!",
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF0F172A), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (lang == Lang.TR) "Bu Bölümde Bulduğun Bonus Kelimeler:" else "Bonus Words Found:", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (foundBonusWords.isEmpty()) (if (lang == Lang.TR) "Henüz yok — çarktan farklı kelimeler dene!" else "None yet!") else foundBonusWords.joinToString(", "),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showBonusJarDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))) {
                        Text(if (lang == Lang.TR) "Tamam" else "Close", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // SURE DOLUNCA ODULLU REKLAM VEYA ALTIN ILE +30 SANIYE
    if (isTimeOut) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF1E293B), border = BorderStroke(2.dp, Color(0xFFEF4444))) {
                Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏰", fontSize = 44.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(if (lang == Lang.TR) "SÜRE DOLDU!" else "TIME'S UP!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(Modifier.height(14.dp))
                    MenuButton(
                        text = if (lang == Lang.TR) "🎬 Reklam İzle (+30 Sn Ekle)" else "🎬 Watch Ad (+30s)",
                        gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                        height = 48.dp,
                        fontSize = 14
                    ) {
                        soundEngine.playClick()
                        adMobManager.showRewardedAd(
                            onRewardEarned = {
                                remainingSeconds = 30
                                isTimeOut = false
                            },
                            onNotReady = {
                                soundEngine.playError()
                                feedbackBanner = if (lang == Lang.TR) "⏳ Reklam henüz hazır değil, birkaç saniye sonra tekrar dene!" else "⏳ Ad not ready yet, try again in a moment!"
                            }
                        )
                    }
                    if (gold >= 50) {
                        Spacer(Modifier.height(8.dp))
                        MenuButton(
                            text = if (lang == Lang.TR) "🪙 50 Altın Ver (+30 Sn Ekle)" else "🪙 Spend 50 Gold (+30s)",
                            gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                            height = 44.dp,
                            fontSize = 13
                        ) {
                            soundEngine.playClick()
                            onUpdateEconomy(gold - 50, bonusJarCount, freeHammers)
                            remainingSeconds = 30
                            isTimeOut = false
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { isTimeOut = false; onBackToMenu() }) {
                        Text(if (lang == Lang.TR) "Ana Menüye Dön" else "Back to Menu", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    // BOLUM SONU EKRANI (ODULU 2'YE KATLAMA REKLAM BUTONU ILE)
    if (showWinDialog) {
        val monumentCompleteBonus = if (level % 10 == 0) 150 else 0
        val baseTotal = 30 + (if (puzzle.isTimeAttack) remainingSeconds * 2 else 0) + (if (puzzle.isBossLevel) 80 else 0) + monumentCompleteBonus
        val finalEarned = if (winRewardDoubled) baseTotal * 2 else baseTotal

        Dialog(onDismissRequest = {}) {
            Box(contentAlignment = Alignment.Center) {
                ConfettiEffectOverlay()
                Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFF0F172A), border = BorderStroke(2.5.dp, Color(0xFFFBBF24)), shadowElevation = 16.dp) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (puzzle.isBossLevel) "🗿👑🗿" else "⭐⭐⭐", fontSize = 28.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (puzzle.isBossLevel) (if (lang == Lang.TR) "🎉 MUHAFIZ YENİLDİ! (SV.$level)" else "🎉 GUARDIAN DEFEATED! (LV.$level)")
                            else (if (lang == Lang.TR) "🎉 SEVİYE $level GEÇİLDİ!" else "🎉 LEVEL $level CLEARED!"),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(winPraiseTitle, color = Color(0xFFFBBF24), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF064E3B), border = BorderStroke(1.dp, Color(0xFF34D399))) {
                            Text(
                                text = if (lang == Lang.TR) "Toplam Ödül: +$finalEarned 🪙 • Müze: %$buildPercent" else "Reward: +$finalEarned 🪙 • Museum: $buildPercent%",
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        unlockedFact?.let { fact ->
                            Spacer(Modifier.height(10.dp))
                            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), border = BorderStroke(1.dp, Color(0xFF38BDF8))) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(if (lang == Lang.TR) "🏛️ ESER TAMAMLANDI (+150 🪙): ${fact.titleTr}" else "🏛️ MONUMENT COMPLETED: ${fact.titleEn}", color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (lang == Lang.TR) fact.infoTr else fact.infoEn, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 14.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (!winRewardDoubled) {
                            MenuButton(
                                text = if (lang == Lang.TR) "🎬 REKLAM İZLE -> ÖDÜLÜ 2'YE KATLA (+${baseTotal * 2} 🪙)" else "🎬 WATCH AD -> 2x REWARD (+${baseTotal * 2} 🪙)",
                                gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                                height = 46.dp,
                                fontSize = 13
                            ) {
                                soundEngine.playClick()
                                adMobManager.showRewardedAd(
                                    onRewardEarned = {
                                        soundEngine.playLevelWin()
                                        winRewardDoubled = true
                                    },
                                    onNotReady = {
                                        feedbackBanner = if (lang == Lang.TR) "⏳ Reklam henüz hazır değil!" else "⏳ Ad not ready yet!"
                                    }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        MenuButton(if (lang == Lang.TR) "SEVİYE ${level + 1}'E GEÇ ➔" else "CONTINUE TO LEVEL ${level + 1} ➔", listOf(Color(0xFF10B981), Color(0xFF059669)), 48.dp, 15) {
                            soundEngine.playClick()
                            showWinDialog = false
                            onLevelCompleted(finalEarned)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. SINIRLANDIRILMIS GUNLUK SANS CARKI (1 UCRETSIZ + 2 REKLAMLI = MAX 3)
// ==========================================
@Composable
fun SpinWheelDialog(
    lang: Lang,
    spinsUsedToday: Int,
    soundEngine: ProceduralSoundEngine,
    adMobManager: AdMobManager,
    onPrizeWon: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val prizesTr = listOf("+50 🪙", "+100 🪙", "🔨 Çekiç", "+150 🪙", "+75 🪙", "💎 250 🪙")
    val sliceColors = listOf(Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFFEF4444))
    var targetAngle by remember { mutableFloatStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val maxDailySpins = 3
    val remainingSpins = (maxDailySpins - spinsUsedToday).coerceAtLeast(0)
    val isFirstFreeSpin = (spinsUsedToday == 0)

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
        label = "spin"
    )

    fun startSpinAnimation() {
        isSpinning = true
        resultText = null
        val winIdx = Random.nextInt(prizesTr.size)
        val extraSpins = 5 * 360f
        val sliceAngle = 360f / prizesTr.size
        targetAngle += extraSpins + (360f - winIdx * sliceAngle)

        scope.launch {
            repeat(10) { delay(240); soundEngine.playWheelTick() }
            delay(450)
            soundEngine.playLevelWin()
            when (winIdx) {
                0 -> { onPrizeWon(50, 0); resultText = "🎉 +50 🪙 KAZANDIN!" }
                1 -> { onPrizeWon(100, 0); resultText = "🎉 +100 🪙 KAZANDIN!" }
                2 -> { onPrizeWon(0, 1); resultText = "🎉 +1 BEDAVA 🔨 ÇEKİÇ KAZANDIN!" }
                3 -> { onPrizeWon(150, 0); resultText = "🎉 +150 🪙 KAZANDIN!" }
                4 -> { onPrizeWon(75, 0); resultText = "🎉 +75 🪙 KAZANDIN!" }
                else -> { onPrizeWon(250, 0); resultText = "💎 JACKPOT! +250 🪙 KAZANDIN!" }
            }
            isSpinning = false
        }
    }

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFF0F172A), border = BorderStroke(2.dp, Color(0xFFFBBF24))) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (lang == Lang.TR) "🎡 GÜNLÜK ŞANS ÇARKI" else "🎡 DAILY SPIN WHEEL", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    text = if (lang == Lang.TR) "Bugün Kalan Hakkın: $remainingSpins / $maxDailySpins" else "Daily Spins Left: $remainingSpins / $maxDailySpins",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text("🔻", fontSize = 22.sp)

                Box(modifier = Modifier.size(205.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        rotate(animatedAngle) {
                            val sweep = 360f / prizesTr.size
                            for (i in prizesTr.indices) {
                                drawArc(
                                    color = sliceColors[i],
                                    startAngle = i * sweep - 90f - sweep / 2f,
                                    sweepAngle = sweep,
                                    useCenter = true
                                )
                            }
                            drawCircle(color = Color.White, radius = size.width / 2f, style = Stroke(width = 6f))
                        }
                        drawCircle(color = Color(0xFF0F172A), radius = 26f)
                        drawCircle(color = Color(0xFFFBBF24), radius = 26f, style = Stroke(width = 4f))
                    }
                    Text("🎁", fontSize = 18.sp)
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = resultText ?: (if (lang == Lang.TR) "Ödüller: 50🪙 • 100🪙 • 150🪙 • 250🪙 • Bedava 🔨" else "Prizes: Up to 250🪙 & Free Hammer!"),
                    color = if (resultText != null) Color(0xFF34D399) else Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                MenuButton(
                    text = when {
                        isSpinning -> if (lang == Lang.TR) "ÇARK DÖNÜYOR..." else "SPINNING..."
                        remainingSpins <= 0 -> if (lang == Lang.TR) "⏳ GÜNLÜK HAKKIN DOLDU (Yarın Gel)" else "⏳ DAILY LIMIT REACHED"
                        isFirstFreeSpin -> if (lang == Lang.TR) "ÜCRETSİZ ÇEVİR! (1/1) 🎰" else "FREE SPIN! (1/1) 🎰"
                        else -> if (lang == Lang.TR) "🎬 REKLAM İZLE & ÇEVİR ($remainingSpins Hak)" else "🎬 WATCH AD TO SPIN ($remainingSpins Left)"
                    },
                    gradient = if (remainingSpins > 0) listOf(Color(0xFF10B981), Color(0xFF059669)) else listOf(Color(0xFF475569), Color(0xFF334155)),
                    height = 48.dp,
                    fontSize = 14
                ) {
                    if (isSpinning || remainingSpins <= 0) {
                        if (remainingSpins <= 0) soundEngine.playError()
                        return@MenuButton
                    }
                    soundEngine.playClick()
                    if (isFirstFreeSpin) {
                        startSpinAnimation()
                    } else {
                        adMobManager.showRewardedAd(
                            onRewardEarned = { startSpinAnimation() },
                            onNotReady = {
                                resultText = if (lang == Lang.TR) "⏳ Reklam yükleniyor, birkaç saniye sonra tekrar bas!" else "⏳ Ad loading, try again in a moment!"
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { if (!isSpinning) onDismiss() }) {
                    Text(if (lang == Lang.TR) "Kapat" else "Close", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun MuseumBuilderDialog(lang: Lang, currentLevel: Int, worldTheme: WorldTheme, onDismiss: () -> Unit) {
    val stepInZone = ((currentLevel - 1) % 10) + 1
    val percent = stepInZone * 10

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF0F172A), border = BorderStroke(2.dp, Color(0xFF8B5CF6)), modifier = Modifier.fillMaxHeight(0.84f)) {
            Column(modifier = Modifier.padding(18.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (lang == Lang.TR) "🏗️ MÜZEMİ İNŞA ET" else "🏗️ BUILD MY MUSEUM", color = Color(0xFFC4B5FD), fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    text = if (lang == Lang.TR) "Aktif İnşaat: ${worldTheme.monumentNameTr} (%$percent)" else "Current Build: $percent%",
                    color = Color(0xFFFDE047),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))

                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E293B), border = BorderStroke(1.dp, worldTheme.accentColor), modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        val w = size.width
                        val h = size.height
                        if (stepInZone >= 1) drawRoundRect(Color(0xFF94A3B8), Offset(w * 0.1f, h * 0.82f), Size(w * 0.8f, h * 0.14f), CornerRadius(8f, 8f))
                        if (stepInZone >= 2) drawRoundRect(Color(0xFFCBD5E1), Offset(w * 0.16f, h * 0.72f), Size(w * 0.68f, h * 0.10f), CornerRadius(6f, 6f))
                        if (stepInZone >= 4) drawRect(worldTheme.accentColor, Offset(w * 0.22f, h * 0.35f), Size(w * 0.08f, h * 0.37f))
                        if (stepInZone >= 5) drawRect(worldTheme.accentColor, Offset(w * 0.46f, h * 0.35f), Size(w * 0.08f, h * 0.37f))
                        if (stepInZone >= 6) drawRect(worldTheme.accentColor, Offset(w * 0.70f, h * 0.35f), Size(w * 0.08f, h * 0.37f))
                        if (stepInZone >= 7) drawRoundRect(Color(0xFFE2E8F0), Offset(w * 0.18f, h * 0.25f), Size(w * 0.64f, h * 0.10f), CornerRadius(8f, 8f))
                        if (stepInZone >= 9) drawArc(Color(0xFFFBBF24), 180f, 180f, true, Offset(w * 0.28f, h * 0.05f), Size(w * 0.44f, h * 0.40f))
                    }
                }

                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HISTORICAL_FACTS) { fact ->
                        val isUnlocked = currentLevel > fact.levelMilestone
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isUnlocked) Color(0xFF1E293B) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, if (isUnlocked) Color(0xFF34D399) else Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Text(
                                    text = if (isUnlocked) "✅ ${fact.titleTr} (TAMAMLANDI)" else "🏗️ Seviye ${fact.levelMilestone}'da Tamamlanır: ${fact.titleTr}",
                                    color = if (isUnlocked) Color(0xFF34D399) else Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                if (isUnlocked) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (lang == Lang.TR) fact.infoTr else fact.infoEn, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))) {
                    Text(if (lang == Lang.TR) "Tamam" else "Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 10. NEON PUSULA HARF CARKI
// ==========================================
@Composable
fun WordSwipeWheel(
    letters: List<Char>,
    selectedIndices: List<Int>,
    sealedIndex: Int?,
    hideLettersForMemory: Boolean,
    currentTouchPos: Offset?,
    comboCount: Int,
    accentColor: Color,
    onSelectIndex: (Int) -> Unit,
    onUpdateTouch: (Offset?) -> Unit,
    onDragEnd: () -> Unit
) {
    val wheelDiameter = 242.dp
    val density = LocalDensity.current
    val wheelPx = with(density) { wheelDiameter.toPx() }
    val center = Offset(wheelPx / 2f, wheelPx / 2f)
    val orbitRadius = wheelPx * 0.35f
    val nodeRadius = (wheelPx * (if (letters.size <= 5) 0.125f else if (letters.size <= 7) 0.11f else 0.098f))

    val letterCenters = remember(letters, wheelPx) {
        letters.indices.map { i ->
            val angle = -PI / 2 + (2 * PI * i / letters.size)
            Offset(center.x + (orbitRadius * cos(angle)).toFloat(), center.y + (orbitRadius * sin(angle)).toFloat())
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "w")
    val ringRotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "r")
    val corePulse by infiniteTransition.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")

    Box(
        modifier = Modifier.size(wheelDiameter).pointerInput(letters, sealedIndex) {
            detectDragGestures(
                onDragStart = { startOffset ->
                    onUpdateTouch(startOffset)
                    letterCenters.forEachIndexed { idx, c -> if ((startOffset - c).getDistance() <= nodeRadius * 1.32f) onSelectIndex(idx) }
                },
                onDrag = { change, _ ->
                    val pos = change.position
                    onUpdateTouch(pos)
                    letterCenters.forEachIndexed { idx, c -> if ((pos - c).getDistance() <= nodeRadius * 1.25f) onSelectIndex(idx) }
                },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragEnd() }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF090D16)), center, wheelPx / 2f), wheelPx / 2f, center)
            drawCircle(Brush.sweepGradient(listOf(accentColor, Color(0xFFF59E0B), Color(0xFFA855F7), accentColor), center), wheelPx / 2f - 3f, center, style = Stroke(width = 5f))

            rotate(degrees = ringRotation, pivot = center) {
                drawCircle(accentColor.copy(alpha = 0.35f), orbitRadius, center, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)))
            }

            letterCenters.forEach { lc -> drawLine(Color.White.copy(alpha = 0.07f), center, lc, strokeWidth = 2.5f) }

            val activeGlow = if (selectedIndices.isNotEmpty()) Color(0xFFF59E0B) else accentColor
            drawCircle(activeGlow.copy(alpha = 0.18f), 28f * corePulse, center)
            drawCircle(activeGlow.copy(alpha = 0.55f), 12f, center)

            if (selectedIndices.isNotEmpty()) {
                val path = Path().apply {
                    val firstPt = letterCenters[selectedIndices.first()]
                    moveTo(firstPt.x, firstPt.y)
                    for (i in 1 until selectedIndices.size) lineTo(letterCenters[selectedIndices[i]].x, letterCenters[selectedIndices[i]].y)
                    currentTouchPos?.let { lineTo(it.x, it.y) }
                }
                drawPath(path, Color(0xFFF59E0B).copy(alpha = 0.35f), style = Stroke(width = 24f, cap = StrokeCap.Round))
                drawPath(path, Color(0xFFFDE047), style = Stroke(width = 11f, cap = StrokeCap.Round))
            }

            letterCenters.forEachIndexed { idx, c ->
                val isSelected = idx in selectedIndices
                val isSealed = (idx == sealedIndex)
                val curR = if (isSelected) nodeRadius * 1.18f else nodeRadius

                if (isSelected) drawCircle(Color(0xFFF59E0B).copy(alpha = 0.35f), curR * 1.3f, c)

                drawCircle(
                    brush = when {
                        isSealed -> Brush.verticalGradient(listOf(Color(0xFF57534E), Color(0xFF292524)), c.y - curR, c.y + curR)
                        isSelected -> Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)), c.y - curR, c.y + curR)
                        else -> Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)), c.y - curR, c.y + curR)
                    },
                    radius = curR,
                    center = c
                )
                drawCircle(
                    color = when {
                        isSealed -> Color(0xFFEF4444)
                        isSelected -> Color.White
                        else -> accentColor
                    },
                    radius = curR,
                    center = c,
                    style = Stroke(width = if (isSelected || isSealed) 5f else 3.5f)
                )
            }
        }

        Text(if (comboCount >= 2) "🔥x$comboCount" else "✦", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, fontSize = if (comboCount >= 2) 13.sp else 16.sp)

        letters.forEachIndexed { idx, ch ->
            val c = letterCenters[idx]
            val isSelected = idx in selectedIndices
            val isSealed = (idx == sealedIndex)
            val scaleAnim by animateFloatAsState(if (isSelected) 1.22f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "s")
            val offsetX = with(density) { (c.x - wheelPx / 2f).toDp() }
            val offsetY = with(density) { (c.y - wheelPx / 2f).toDp() }
            val displayChar = when {
                isSealed -> "🪨$ch"
                hideLettersForMemory && !isSelected -> "?"
                else -> ch.toString()
            }
            Text(
                text = displayChar,
                color = when {
                    isSealed -> Color(0xFFFCA5A5)
                    isSelected -> Color(0xFF0F172A)
                    hideLettersForMemory -> Color(0xFFC4B5FD)
                    else -> Color.White
                },
                fontWeight = FontWeight.Black,
                fontSize = (if (isSealed) 16 else if (letters.size <= 6) 23 else 20).sp,
                modifier = Modifier.offset(x = offsetX, y = offsetY).scale(scaleAnim)
            )
        }
    }
}

// ==========================================
// 11. AYARLAR PENCERESI (GIZLILIK POLITIKASI LINKI ILE)
// ==========================================
@Composable
fun SettingsDialog(
    lang: Lang,
    musicEnabled: Boolean,
    sfxEnabled: Boolean,
    highGraphics: Boolean,
    onToggleMusic: () -> Unit,
    onToggleSfx: () -> Unit,
    onToggleGraphics: () -> Unit,
    onContactEmail: () -> Unit,
    privacyOptionsRequired: Boolean,
    onOpenPrivacyOptions: () -> Unit,
    onResetProgress: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPolicy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1E293B), border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f))) {
            Column(modifier = Modifier.padding(22.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (lang == Lang.TR) "⚙️ OYUN AYARLARI" else "⚙️ GAME SETTINGS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                SettingRow(if (lang == Lang.TR) "🎵 Arka Plan Müziği" else "🎵 Background Music", musicEnabled, onToggleMusic)
                SettingRow(if (lang == Lang.TR) "🔊 Akustik Harp & Efektler" else "🔊 Sound Effects (SFX)", sfxEnabled, onToggleSfx)
                SettingRow(if (lang == Lang.TR) "✨ Yüksek Grafik & Animasyon" else "✨ High Graphics", highGraphics, onToggleGraphics)

                TextButton(onClick = { showPolicy = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (lang == Lang.TR) "🔒 Gizlilik Politikası & Aydınlatma Metni" else "🔒 Privacy Policy",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (privacyOptionsRequired) {
                    TextButton(onClick = onOpenPrivacyOptions, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (lang == Lang.TR) "🛡️ Reklam Tercihleri (Onay Ayarları)" else "🛡️ Ad Privacy Choices",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onResetProgress) { Text(if (lang == Lang.TR) "İlerlemeyi Sıfırla" else "Reset Save", color = Color(0xFFEF4444), fontSize = 13.sp) }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                        Text(if (lang == Lang.TR) "Tamam" else "Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPolicy) {
        PrivacyPolicyDialog(lang = lang, onContactEmail = onContactEmail, onDismiss = { showPolicy = false })
    }
}

@Composable
fun SettingRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

// ==========================================
// 12. UYGULAMA ICI GIZLILIK POLITIKASI (TR / EN) - e-posta ile iletisim
// ==========================================
private val PRIVACY_TR_TITLE = """Gizlilik Politikası"""
private val PRIVACY_TR_UPDATED = """Son güncelleme: 17 Ekim 2026"""
private val PRIVACY_TR_INTRO = """Kelime Efsanesi ("Uygulama"), bağımsız geliştirici Selim tarafından geliştirilmiştir. Bu politika, Uygulama'yı kullanırken hangi bilgilerin işlendiğini ve haklarınızı açıklar."""
private val PRIVACY_TR_SECTIONS: List<Pair<String, String>> = listOf(
    """1. Hesap ve kişisel bilgiler""" to """Uygulamayı kullanmak için hesap açmanız gerekmez. Adınızı, e-posta adresinizi, telefon numaranızı, konumunuzu, kişi listenizi, fotoğraflarınızı, kameranızı veya mikrofonunuzu istemeyiz ve toplamayız. Geliştirici olarak sunucularımızda sizinle ilgili kişisel veri tutmayız.""",
    """2. Oyun verileriniz""" to """Seviyeniz, altınınız, ayarlarınız ve günlük çark hakkınız yalnızca cihazınızda saklanır, bize gönderilmez. Uygulamayı kaldırdığınızda veya Ayarlar'daki "İlerlemeyi Sıfırla" düğmesine bastığınızda silinir.""",
    """3. Reklamlar (Google AdMob)""" to """Uygulama, reklam göstermek için Google AdMob hizmetini kullanır. Reklamları göstermek, ölçmek ve kötüye kullanımı önlemek amacıyla Google; reklam kimliğinizi (Android Advertising ID), IP adresinizi, cihaz ve işletim sistemi bilgilerinizi, uygulama kullanımı ve reklam etkileşimi verilerinizi ve IP adresinden türetilen yaklaşık konumunuzu işleyebilir. Bu veriler Google tarafından işlenir ve Google'ın politikaları geçerlidir: policies.google.com/privacy ve policies.google.com/technologies/partner-sites""",
    """4. Reklam tercihleriniz""" to """Avrupa Ekonomik Alanı, Birleşik Krallık ve İsviçre'de Google'ın onay formu ile izninizi isteriz; tercihinizi Ayarlar bölümündeki "Reklam Tercihleri" düğmesinden istediğiniz zaman değiştirebilirsiniz. Her yerde, reklam kimliğinizi Android Ayarlar > Google > Reklamlar bölümünden sıfırlayabilir veya silebilir, böylece reklamların kişiselleştirilmesini sınırlayabilirsiniz.""",
    """5. İzinler""" to """Uygulama yalnızca reklamların yüklenmesi için internet erişimi, ağ durumu ve reklam kimliği (Advertising ID) izinlerini kullanır. Konum, kişiler, kamera, mikrofon veya depolama izni istemez.""",
    """6. Çocuklar""" to """Uygulama 13 yaşın altındaki çocuklara yönelik değildir ve bu yaştaki çocuklardan bilerek veri toplamayız.""",
    """7. Veri paylaşımı""" to """Kişisel verilerinizi satmayız ve reklam ortağımız Google dışında kimseyle paylaşmayız.""",
    """8. Haklarınız""" to """6698 sayılı KVKK ve (Avrupa'daysanız) GDPR kapsamında verilerinizle ilgili bilgi isteme, düzeltme, silme ve itiraz etme haklarına sahipsiniz. Oyun verileriniz yalnızca cihazınızda olduğundan çoğu işlemi doğrudan cihazınızdan yapabilirsiniz; reklam verileriyle ilgili talepleriniz için Google'a veya bize e-postayla ulaşabilirsiniz.""",
    """9. Değişiklikler""" to """Bu politikayı zaman zaman güncelleyebiliriz. Güncel sürüm Uygulama'nın Ayarlar bölümünde ve bu sayfada yayınlanır; son güncelleme tarihi yukarıda yazar.""",
    """10. İletişim""" to """Soru, talep ve şikayetleriniz için: baydemirel52@gmail.com"""
)

private val PRIVACY_EN_TITLE = """Privacy Policy"""
private val PRIVACY_EN_UPDATED = """Last updated: 17 October 2026"""
private val PRIVACY_EN_INTRO = """Kelime Efsanesi (the "App") is developed by Selim, an independent developer. This policy explains what information is processed when you use the App, and your rights."""
private val PRIVACY_EN_SECTIONS: List<Pair<String, String>> = listOf(
    """1. Account and personal information""" to """You do not need an account to use the App. We do not ask for or collect your name, email address, phone number, location, contacts, photos, camera or microphone. As the developer, we do not keep personal data about you on our servers.""",
    """2. Your game data""" to """Your level, gold, settings and daily spin count are stored only on your device and are never sent to us. They are deleted when you uninstall the App or tap "Reset Save" in Settings.""",
    """3. Ads (Google AdMob)""" to """The App uses Google AdMob to show ads. To serve and measure ads and prevent abuse, Google may process your advertising ID (Android Advertising ID), IP address, device and operating system information, app usage and ad interaction data, and an approximate location derived from your IP address. This data is processed by Google and Google's policies apply: policies.google.com/privacy and policies.google.com/technologies/partner-sites""",
    """4. Your ad choices""" to """In the European Economic Area, the United Kingdom and Switzerland we ask for your consent using Google's consent form; you can change your choice at any time with the "Ad Privacy Choices" button in Settings. Everywhere, you can reset or delete your advertising ID under Android Settings > Google > Ads to limit ad personalization.""",
    """5. Permissions""" to """The App only uses internet access, network state and advertising ID permissions, so that ads can load. It does not request location, contacts, camera, microphone or storage permissions.""",
    """6. Children""" to """The App is not directed at children under 13, and we do not knowingly collect data from children of that age.""",
    """7. Data sharing""" to """We do not sell your personal data and do not share it with anyone other than our ad partner Google.""",
    """8. Your rights""" to """Under Turkish KVKK (Law No. 6698) and, if you are in Europe, the GDPR, you have the right to request information about, correct, delete and object to the processing of your data. Because your game data exists only on your device, you can do most of this directly on your device; for requests about ad data, contact Google or email us.""",
    """9. Changes""" to """We may update this policy from time to time. The current version is published in the App's Settings and on this page, and the last updated date is shown above.""",
    """10. Contact""" to """For questions, requests and complaints: baydemirel52@gmail.com"""
)

@Composable
fun PrivacyPolicyDialog(lang: Lang, onContactEmail: () -> Unit, onDismiss: () -> Unit) {
    val title = if (lang == Lang.TR) PRIVACY_TR_TITLE else PRIVACY_EN_TITLE
    val updated = if (lang == Lang.TR) PRIVACY_TR_UPDATED else PRIVACY_EN_UPDATED
    val intro = if (lang == Lang.TR) PRIVACY_TR_INTRO else PRIVACY_EN_INTRO
    val sections = if (lang == Lang.TR) PRIVACY_TR_SECTIONS else PRIVACY_EN_SECTIONS

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(2.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.padding(18.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔒 " + title, color = Color(0xFF7DD3FC), fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(updated, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    item {
                        Text(intro, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 17.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                    items(sections) { section ->
                        Text(section.first, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(section.second, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onContactEmail, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))) {
                        Text(if (lang == Lang.TR) "✉️ E-posta Gönder" else "✉️ Email Us", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                        Text(if (lang == Lang.TR) "Kapat" else "Close", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// === KODUN TAMAMI BITTI ===

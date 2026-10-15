package com.selim.kelimeoyunu

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                WordGameApp(onExitApp = { finish() })
            }
        }
    }
}

// ==========================================
// 1. TELİFSİZ PROSEDÜREL SES MOTORU (SYNTH)
// ==========================================
class ProceduralSoundEngine {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var musicJob: Job? = null
    var sfxEnabled = true
    var musicEnabled = true
    var isAppInForeground = true

    private val swipeNotes = doubleArrayOf(523.25, 587.33, 659.25, 698.46, 783.99, 880.00, 987.77, 1046.50)

    fun playClick() {
        if (!sfxEnabled) return
        playTone(620.0, 45, 0.25f)
    }

    fun playSwipeNote(index: Int) {
        if (!sfxEnabled) return
        val note = swipeNotes[index.coerceIn(0, swipeNotes.lastIndex)]
        playTone(note, 70, 0.3f)
    }

    fun playWordSuccess() {
        if (!sfxEnabled) return
        scope.launch {
            playToneSync(523.25, 70, 0.3f)
            playToneSync(659.25, 70, 0.3f)
            playToneSync(783.99, 125, 0.35f)
        }
    }

    fun playBonusWord() {
        if (!sfxEnabled) return
        scope.launch {
            playToneSync(587.33, 65, 0.3f)
            playToneSync(880.00, 120, 0.35f)
        }
    }

    fun playHammerHit() {
        if (!sfxEnabled) return
        scope.launch {
            playToneSync(180.0, 60, 0.4f)
            playToneSync(740.0, 110, 0.35f)
        }
    }

    fun playLightningStrike() {
        if (!sfxEnabled) return
        scope.launch {
            playToneSync(1174.66, 55, 0.38f)
            playToneSync(880.0, 55, 0.38f)
            playToneSync(1567.98, 140, 0.4f)
        }
    }

    fun playSealBreak() {
        if (!sfxEnabled) return
        scope.launch {
            playToneSync(329.63, 80, 0.35f)
            playToneSync(440.0, 80, 0.35f)
            playToneSync(659.25, 150, 0.4f)
        }
    }

    fun playError() {
        if (!sfxEnabled) return
        playTone(190.0, 120, 0.25f)
    }

    fun playLevelWin() {
        if (!sfxEnabled) return
        scope.launch {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.50)
            for (n in notes) {
                playToneSync(n, 105, 0.36f)
            }
        }
    }

    fun updateBackgroundMusic() {
        musicJob?.cancel()
        if (!musicEnabled || !isAppInForeground) return
        musicJob = scope.launch {
            val melody = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 392.00, 329.63, 293.66)
            var idx = 0
            while (isActive && musicEnabled && isAppInForeground) {
                playToneSync(melody[idx % melody.size], 280, 0.055f)
                delay(420)
                idx++
            }
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Float) {
        scope.launch { playToneSync(freqHz, durationMs, volume) }
    }

    private suspend fun playToneSync(freqHz: Double, durationMs: Int, volume: Float) = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val angle = 2.0 * PI * i / (sampleRate / freqHz)
                val envelope = when {
                    i < numSamples * 0.1 -> i / (numSamples * 0.1)
                    i > numSamples * 0.7 -> (numSamples - i) / (numSamples * 0.3)
                    else -> 1.0
                }
                samples[i] = (sin(angle) * Short.MAX_VALUE * volume * envelope).toInt().toShort()
            }
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
            delay(durationMs.toLong() + 20)
            track.stop()
            track.release()
        } catch (_: Exception) {}
    }
}

// ==========================================
// 2. HER 10 SEVİYEDE DEĞİŞEN DÜNYA TEMALARI & BOSS MUHAFIZLARI
// ==========================================
enum class Lang(val code: String, val flag: String, val label: String) {
    TR("tr", "🇹🇷", "TR"),
    EN("en", "🇬🇧", "EN")
}

data class WorldTheme(
    val zoneIndex: Int,
    val regionNameTr: String,
    val regionNameEn: String,
    val bossTitleTr: String,
    val bossTitleEn: String,
    val bgGradient: List<Color>,
    val accentColor: Color,
    val openTileGradient: List<Color>
)

fun getWorldThemeForLevel(level: Int): WorldTheme {
    val zone = ((level - 1) / 10) % 10
    return when (zone) {
        0 -> WorldTheme(
            0, "Göbeklitepe Vadisi", "Göbeklitepe Valley",
            "🗿 Göbeklitepe Muhafızı", "🗿 Guardian of Göbeklitepe",
            listOf(Color(0xFF1C140D), Color(0xFF3B2314), Color(0xFF24130B)),
            Color(0xFFF59E0B), listOf(Color(0xFFD97706), Color(0xFFB45309))
        )
        1 -> WorldTheme(
            1, "Ayasofya Meydanı", "Hagia Sophia Square",
            "⚔️ Ayasofya Muhafızı", "⚔️ Guardian of Hagia Sophia",
            listOf(Color(0xFF1F0916), Color(0xFF3F1028), Color(0xFF230A34)),
            Color(0xFFF43F5E), listOf(Color(0xFFE11D48), Color(0xFF9F1239))
        )
        2 -> WorldTheme(
            2, "Efes Antik Kenti", "Ancient Ephesus",
            "🏛️ Celsus Muhafızı", "🏛️ Guardian of Ephesus",
            listOf(Color(0xFF061A2E), Color(0xFF0C365A), Color(0xFF08223A)),
            Color(0xFF38BDF8), listOf(Color(0xFF0284C7), Color(0xFF0369A1))
        )
        3 -> WorldTheme(
            3, "Galata & Boğaz Geecesi", "Galata Midnight",
            "🦅 Galata Muhafızı", "🦅 Guardian of Galata",
            listOf(Color(0xFF0B1120), Color(0xFF1E1B4B), Color(0xFF311042)),
            Color(0xFFA855F7), listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
        )
        4 -> WorldTheme(
            4, "Giza Piramitleri", "Pyramids of Giza",
            "👑 Firavun Muhafızı", "👑 Pharaoh Guardian",
            listOf(Color(0xFF221A09), Color(0xFF42300E), Color(0xFF1C1306)),
            Color(0xFFFBBF24), listOf(Color(0xFFCA8A04), Color(0xFF854D0E))
        )
        5 -> WorldTheme(
            5, "Roma Kolezyumu", "Roman Colosseum",
            "🛡️ Gladyatör Muhafızı", "🛡️ Gladiator Guardian",
            listOf(Color(0xFF230C0C), Color(0xFF451A1A), Color(0xFF1A0B12)),
            Color(0xFFFB923C), listOf(Color(0xFFEA580C), Color(0xFF9A3412))
        )
        6 -> WorldTheme(
            6, "Sümela Zümrüt Ormanı", "Sumela Emerald Forest",
            "🌲 Karadağ Muhafızı", "🌲 Guardian of Sumela",
            listOf(Color(0xFF051F18), Color(0xFF0B3B2D), Color(0xFF072227)),
            Color(0xFF34D399), listOf(Color(0xFF10B981), Color(0xFF047857))
        )
        7 -> WorldTheme(
            7, "Tac Mahal Sarayı", "Taj Mahal Palace",
            "💎 Babür Muhafızı", "💎 Taj Mahal Guardian",
            listOf(Color(0xFF161130), Color(0xFF2D1B4E), Color(0xFF3B1239)),
            Color(0xFFF472B6), listOf(Color(0xFFDB2777), Color(0xFF9D174D))
        )
        8 -> WorldTheme(
            8, "Nemrut Dağı Zirvesi", "Mount Nemrut Peak",
            "☀️ Kommagene Muhafızı", "☀️ Guardian of Nemrut",
            listOf(Color(0xFF181124), Color(0xFF331D4A), Color(0xFF421C24)),
            Color(0xFFFBBF24), listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
        )
        else -> WorldTheme(
            9, "Machu Picchu Bulutları", "Clouds of Machu Picchu",
            "🏔️ İnka Muhafızı", "🏔️ Incan Guardian",
            listOf(Color(0xFF071E22), Color(0xFF113F46), Color(0xFF092327)),
            Color(0xFF2DD4BF), listOf(Color(0xFF0D9488), Color(0xFF115E59))
        )
    }
}

val PRAISE_WORDS_TR = listOf(
    "🔥 HARİKASIN!",
    "⚡ MUHTEŞEMSİN!",
    "👑 ZEKA KÜPÜ!",
    "🚀 DURDURULAMAZSIN!",
    "💎 EFSANE ÇÖZÜM!",
    "🌟 SÜPERSİN!",
    "🎯 TAM İSABET!",
    "💥 ŞOV YAPIYORSUN!"
)

val PRAISE_WORDS_EN = listOf(
    "🔥 AWESOME!",
    "⚡ MAGNIFICENT!",
    "👑 GENIUS MOVE!",
    "🚀 UNSTOPPABLE!",
    "💎 LEGENDARY!",
    "🌟 SUPERB!",
    "🎯 BULLSEYE!"
)

val LEVEL_WIN_PRAISE_TR = listOf(
    "🏆 MUHTEŞEMSİN! BÖLÜMÜ YIKTIN GEÇTİN!",
    "🔥 HARİKASIN! ZEKA ŞOV YAPTIN!",
    "👑 EFSANESİN! KİMSE SENİ TUTAMAZ!",
    "⚡ OLAĞANÜSTÜ! BEYİN FIRTINASI ESTİRDİN!",
    "💎 ŞAMPİYON GİBİ ÇÖZDÜN, HELAL OLSUN!"
)

val LEVEL_WIN_PRAISE_EN = listOf(
    "🏆 MAGNIFICENT! YOU CRUSHED THIS LEVEL!",
    "🔥 AWESOME! PURE GENIUS DISPLAY!",
    "👑 LEGENDARY! NOTHING CAN STOP YOU!",
    "⚡ BRILLIANT! WHAT A BRAINSTORM!"
)

data class HistoricalFact(
    val levelMilestone: Int,
    val titleTr: String,
    val infoTr: String,
    val titleEn: String,
    val infoEn: String
)

val HISTORICAL_FACTS = listOf(
    HistoricalFact(10, "Göbeklitepe (Şanlıurfa, Türkiye)", "M.Ö. 9600 civarında inşa edilen Göbeklitepe, insanlık tarihinin bilinen en eski anıtsal tapınak yerleşkesidir. Mısır Piramitlerinden yaklaşık 7.000 yıl daha eskidir ve yerleşik hayata geçiş tezlerini değiştirmiştir.", "Göbeklitepe (Şanlıurfa, Turkey)", "Built around 9600 BCE, Göbeklitepe is the world's oldest known monumental temple complex. Predating the Egyptian Pyramids by 7,000 years, it reshaped our understanding of early human civilization."),
    HistoricalFact(20, "Ayasofya (İstanbul, Türkiye)", "537 yılında Bizans İmparatoru I. Justinianus tarafından bazilika olarak inşa ettirilmiştir. Devasa kubbesiyle dünya mimarlık tarihinin en önemli başyapıtlarından biridir ve 1453'te Fatih Sultan Mehmet tarafından camiye çevrilmiştir.", "Hagia Sophia (Istanbul, Turkey)", "Completed in 537 CE under Byzantine Emperor Justinian I, Hagia Sophia is an architectural marvel famous for its massive dome. In 1453, Mehmed the Conqueror converted this iconic structure into a mosque."),
    HistoricalFact(30, "Efes Antik Kenti & Celsus Kütüphanesi (İzmir)", "M.S. 117 yılında tamamlanan Celsus Kütüphanesi, antik dünyanın en büyük üçüncü kütüphanesiydi. İçerisinde yaklaşık 12.000 el yazması parşömen rulosu saklanmaktaydı.", "Ephesus & Library of Celsus (Izmir)", "Completed in 117 CE, the Library of Celsus in ancient Ephesus stored around 12,000 scrolls and stood as the third-largest library of the ancient world."),
    HistoricalFact(40, "Galata Kulesi (İstanbul, Türkiye)", "1348 yılında Cenevizliler tarafından 'İsa Kulesi' adıyla savunma ve gözetleme amacıyla inşa edilmiştir. Hezârfen Ahmed Çelebi'nin 1632'de tahta kanatlarla buradan Üsküdar'a uçtuğu rivayet edilir.", "Galata Tower (Istanbul, Turkey)", "Built by the Genoese in 1348 as the 'Tower of Christ', this medieval stone tower overlooked Constantinople's harbor and became legendary for Hezarfen Ahmed Çelebi's 17th-century flight."),
    HistoricalFact(50, "Keops Piramidi (Giza, Mısır)", "M.Ö. 2560 civarında Firavun Khufu adına yapılan Büyük Piramit, yaklaşık 2.3 milyon taş bloktan oluşur. 3.800 yıl boyunca insan eliyle yapılmış en yüksek yapı unvanını korumuştur.", "Great Pyramid of Giza (Egypt)", "Constructed around 2560 BCE for Pharaoh Khufu using 2.3 million stone blocks, the Great Pyramid remained the tallest human-made structure on Earth for over 3,800 years."),
    HistoricalFact(60, "Kolezyum (Roma, İtalya)", "M.S. 80 yılında İmparator Titus döneminde tamamlanan Kolezyum, 50.000'den fazla seyirci kapasitesine sahipti. Gelişmiş alt geçit ve asansör sistemleriyle antik mühendisliğin zirvesidir.", "The Colosseum (Rome, Italy)", "Completed in 80 CE under Emperor Titus, the Colosseum held over 50,000 spectators and featured a complex underground network of trapdoors and elevators."),
    HistoricalFact(70, "Sümela Manastırı (Trabzon, Türkiye)", "Karadağ'ın sarp kayalıklarına denizden 1.200 metre yükseklikte, M.S. 386 yılında inşa edilmeye başlanmıştır. Kayalara oyulmuş mimarisi ve freskleriyle eşsiz bir tarihi mirastır.", "Sumela Monastery (Trabzon, Turkey)", "Founded in 386 CE on a steep cliff 1,200 meters above sea level in Trabzon, Sumela Monastery is famous for its dramatic rock-cut architecture and vibrant frescoes."),
    HistoricalFact(80, "Tac Mahal (Agra, Hindistan)", "1632-1653 yılları arasında Babür İmparatoru Şah Cihan tarafından eşi Mümtaz Mahal anısına beyaz mermerden yaptırılmıştır. Mimaride simetrinin ve aşkın dünyadaki en ünlü sembolüdür.", "Taj Mahal (Agra, India)", "Commissioned in 1632 by Mughal Emperor Shah Jahan in memory of his wife Mumtaz Mahal, this white marble mausoleum is universally admired as a masterpiece of symmetry."),
    HistoricalFact(90, "Nemrut Dağı Tümülüsü (Adıyaman, Türkiye)", "M.Ö. 62 yılında Kommagene Kralı I. Antiochos tarafından 2.150 metre zirveye yaptırılmıştır. Boyları 8-10 metreyi bulan dev taş heykeller doğu ve batı teraslarında güneşin doğuşunu selamlar.", "Mount Nemrut (Adıyaman, Turkey)", "Built in 62 BCE by King Antiochus I of Commagene at a 2,150-meter peak, Mount Nemrut features colossal 9-meter statues guarding a royal tomb sanctuary."),
    HistoricalFact(100, "Machu Picchu (Cusco, Peru)", "1450 yıllarında İnka İmparatoru Pachacuti tarafından And Dağları'nın zirvesine harç kullanılmadan kenetlenen taşlarla inşa edilmiştir. İspanyol istilacılar tarafından hiç bulunamadığı için dokusu bozulmadan kalmıştır.", "Machu Picchu (Cusco, Peru)", "Built around 1450 CE high in the Andes by Incan Emperor Pachacuti using dry-stone walls, Machu Picchu escaped Spanish conquest and remained intact for centuries.")
)

data class LeagueInfo(val index: Int, val nameTr: String, val nameEn: String, val icon: String, val color: Color)

fun getLeagueForLevel(level: Int): LeagueInfo {
    val step = (level - 1) / 10
    return when (step) {
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
}

// ==========================================
// 3. HER 10 SEVİYEDE BİR HARF SAYISI ARTAN KELİME HAVUZU
// ==========================================
val TR_WORDS_TIER_1 = listOf(
    "BAL", "KALE", "MASA", "KAYA", "SIRA", "KAPI", "YURT", "RENK", "DOST", "ARKA"
)
val TR_WORDS_TIER_2 = listOf(
    "KALEM", "KİTAP", "DENİZ", "TARİH", "SEVGİ", "BAHAR", "GÜNEŞ", "ORMAN", "ASLAN", "SARAY"
)
val TR_WORDS_TIER_3 = listOf(
    "KARTAL", "MİMARİ", "EFSANE", "DESTAN", "KORSAN", "KALKAN", "MEYDAN", "CÜZDAN", "FİNCAN", "MERCAN"
)
val TR_WORDS_TIER_4 = listOf(
    "BAŞKENT", "MANZARA", "FABRİKA", "TEHLİKE", "BİLMECE", "ÇEKMECE", "KARINCA", "PALAMUT", "OKYANUS", "GELENEK"
)
val TR_WORDS_TIER_5 = listOf(
    "KAHRAMAN", "TEKERLEK", "KARANFİL", "ŞAMPİYON", "GÜVERCİN", "PORTAKAL", "KARDELEN", "YOLCULUK", "MUTLULUK", "ÖZGÜRLÜK"
)

val TR_WORDS_ADVANCED = """
MASAL KAYIK YATAK TABAK KABAK SOKAK LİMAN ZAMAN SAMAN KEMİK
DEMİR ŞEHİR NEHİR ZEHİR FİKİR ŞÜKÜR BAKIR ÇAKIR SABIR SATIR
KATIR ÇADIR KADIN YARIN KARIN FIRIN BURUN TORUN SORUN BOYUN
KOYUN SABUN YOSUN KAVUN DAVUL BAVUL KABUL KURAL KURAK DURAK
ÇIRAK MERAK SICAK TABAN TAVAN KAVAN KAZAN SAZAN YAZAR PAZAR
HAZAR KARAR ZARAR DAMAR ÇINAR PINAR KENAR FENER KEMER SEMER
BİBER HABER ŞEKER DEĞER KADER KEDER SEFER ZAFER NEFES KAFES
HEVES ADRES TERAS MAKAS TAKAS SAVAŞ YAVAŞ TIRAŞ KUMAŞ YAMAÇ
ÇEKİÇ BARIŞ YARIŞ KARIŞ ALKIŞ BAKIŞ YAKIŞ ÇIKIŞ NAKIŞ SATIŞ
YATIŞ KAÇIŞ DİKİŞ GİRİŞ VERİŞ GELİŞ BİLİŞ GÖRÜŞ DÖNÜŞ GÜLÜŞ
YORGAN HAYVAN ZİNDAN DESTEK GÖMLEK ÇELENK BÖBREK ŞİMŞEK LEYLEK SÖZLÜK
GÖZLÜK TOPRAK YAPRAK BAYRAK KAYNAK BARDAK PARMAK SALKIM YARDIM KAVRUK
SAVRUK KUVVET DEVLET SERVET ŞEFKAT DİKKAT SOHBET GURBET HASRET KISMET
HİZMET HİKMET ZAHMET RAHMET MERKEZ HERKES KELEBEK SEÇENEK YETENEK TENCERE
PENCERE DEMİRCİ BALIKÇI EKMEKÇİ SİMİTÇİ EĞİTİMCİ ÖĞRETMEN MÜHENDİS MİMARLIK CUMHURİYET
""".trimIndent().split(Regex("\\s+")).map { it.uppercase(Locale("tr", "TR")) }.distinct()

val TR_ALL_TIER_WORDS = TR_WORDS_TIER_1 + TR_WORDS_TIER_2 + TR_WORDS_TIER_3 + TR_WORDS_TIER_4 + TR_WORDS_TIER_5 + TR_WORDS_ADVANCED

val TR_DICTIONARY: Set<String> = (TR_ALL_TIER_WORDS + """
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

val EN_DICTIONARY: Set<String> = (EN_BASE_WORDS + """
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

fun canFormWordFromWheel(wheelLetters: String, candidate: String): Boolean {
    if (candidate.length < 2 || candidate.length > wheelLetters.length) return false
    val counts = mutableMapOf<Char, Int>()
    for (ch in wheelLetters) {
        counts[ch] = (counts[ch] ?: 0) + 1
    }
    for (ch in candidate) {
        val left = counts[ch] ?: 0
        if (left <= 0) return false
        counts[ch] = left - 1
    }
    return true
}

// ==========================================
// 4. BULMACA TABLOSU & BOSS MÜHÜR ÜRETİCİSİ
// ==========================================
data class PlacedWord(
    val word: String,
    val row: Int,
    val col: Int,
    val isHorizontal: Boolean
)

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
    val initialSealedWheelIndex: Int?
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
    } else {
        EN_BASE_WORDS[(level - 1) % EN_BASE_WORDS.size]
    }

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

    val subCandidates = allValidWords
        .filter { it != mainWord && it.length >= 3 }
        .shuffled(rng)
        .sortedByDescending { it.length }

    val twoLetterCandidates = allValidWords
        .filter { it != mainWord && it.length == 2 }
        .shuffled(rng)

    val selectedForBoard = mutableListOf(mainWord)
    for (w in subCandidates) {
        if (selectedForBoard.size < targetBoardCount) selectedForBoard.add(w)
    }
    for (w in twoLetterCandidates) {
        if (selectedForBoard.size < targetBoardCount) selectedForBoard.add(w)
    }

    if (selectedForBoard.size < 2 && mainWord.length >= 3) {
        selectedForBoard.add(mainWord.substring(0, 3))
    }

    val boardSize = 20
    val grid = Array(boardSize) { CharArray(boardSize) { ' ' } }
    val placed = mutableListOf<PlacedWord>()

    val startRow = boardSize / 2
    val startCol = (boardSize - mainWord.length) / 2
    for (i in mainWord.indices) {
        grid[startRow][startCol + i] = mainWord[i]
    }
    placed.add(PlacedWord(mainWord, startRow, startCol, true))

    var fallbackRow = startRow + 2
    for (w in selectedForBoard.drop(1)) {
        var isPlaced = false
        outer@ for (pw in placed) {
            val tryHorizontal = !pw.isHorizontal
            for (i in w.indices) {
                for (j in pw.word.indices) {
                    if (w[i] == pw.word[j]) {
                        val r = if (tryHorizontal) pw.row + j else pw.row - i
                        val c = if (tryHorizontal) pw.col - i else pw.col + j
                        if (canPlaceWord(grid, w, r, c, tryHorizontal)) {
                            for (k in w.indices) {
                                val nr = if (tryHorizontal) r else r + k
                                val nc = if (tryHorizontal) c + k else c
                                grid[nr][nc] = w[k]
                            }
                            placed.add(PlacedWord(w, r, c, tryHorizontal))
                            isPlaced = true
                            break@outer
                        }
                    }
                }
            }
        }
        if (!isPlaced) {
            while (fallbackRow < boardSize - 1 && !canPlaceWord(grid, w, fallbackRow, 2, true)) {
                fallbackRow++
            }
            if (fallbackRow < boardSize - 1) {
                for (k in w.indices) {
                    grid[fallbackRow][2 + k] = w[k]
                }
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

    val normalizedPlaced = placed.map {
        it.copy(row = it.row - minR, col = it.col - minC)
    }

    val boardWordSet = normalizedPlaced.map { it.word }.toSet()
    val bonusSet = allValidWords.filter { it !in boardWordSet }.toSet()

    var wheelChars = mainWord.toList().shuffled(rng)
    if (wheelChars.joinToString("") == mainWord && mainWord.length > 2) {
        wheelChars = wheelChars.reversed()
    }

    // Her 10 seviyede bir BOSS BÖLÜMÜ! (En az 1 kelimenin mühürsüz harflerle yazılabildiğinden %100 emin olur)
    val isBossLevel = (level % 10 == 0)
    var sealedIdx: Int? = null
    if (isBossLevel) {
        for (candidateIdx in wheelChars.indices) {
            val remainingStr = wheelChars.filterIndexed { i, _ -> i != candidateIdx }.joinToString("")
            val canSolveAtLeastOne = normalizedPlaced.any { pw -> canFormWordFromWheel(remainingStr, pw.word) }
            if (canSolveAtLeastOne) {
                sealedIdx = candidateIdx
                break
            }
        }
    }

    // Boss bölümüyle zamana karşı bölüm çakışırsa (örn. 30. seviye), Boss öncelikli olsun ki kafa karışmasın
    val isTimeAttack = (level % 15 == 0) && !isBossLevel
    val timeLimit = if (isTimeAttack) (45 + normalizedPlaced.size * 8) else 0

    return LevelPuzzle(
        level = level,
        wheelLetters = wheelChars,
        placedWords = normalizedPlaced,
        bonusWords = bonusSet,
        gridRows = (maxR - minR + 1).coerceAtLeast(1),
        gridCols = (maxC - minC + 1).coerceAtLeast(1),
        isTimeAttack = isTimeAttack,
        timeLimitSec = timeLimit,
        isBossLevel = isBossLevel,
        initialSealedWheelIndex = sealedIdx
    )
}

private fun canPlaceWord(grid: Array<CharArray>, word: String, r: Int, c: Int, isHoriz: Boolean): Boolean {
    val size = grid.size
    if (r < 1 || c < 1) return false
    if (isHoriz && c + word.length >= size - 1) return false
    if (!isHoriz && r + word.length >= size - 1) return false

    if (isHoriz) {
        if (grid[r][c - 1] != ' ' || grid[r][c + word.length] != ' ') return false
    } else {
        if (grid[r - 1][c] != ' ' || grid[r + word.length][c] != ' ') return false
    }

    var newCellCount = 0
    for (i in word.indices) {
        val nr = if (isHoriz) r else r + i
        val nc = if (isHoriz) c + i else c
        val current = grid[nr][nc]

        if (current != ' ' && current != word[i]) return false

        if (current != ' ') {
            val prevSameDir = if (isHoriz) grid[nr][nc - 1] else grid[nr - 1][nc]
            val nextSameDir = if (isHoriz) grid[nr][nc + 1] else grid[nr + 1][nc]
            if (i > 0 && prevSameDir != ' ') return false
            if (i < word.lastIndex && nextSameDir != ' ') return false
        } else {
            newCellCount++
            if (isHoriz) {
                if (grid[nr - 1][nc] != ' ' || grid[nr + 1][nc] != ' ') return false
            } else {
                if (grid[nr][nc - 1] != ' ' || grid[nr][nc + 1] != ' ') return false
            }
        }
    }
    return newCellCount > 0
}

// ==========================================
// 5. ANA UYGULAMA VE EKRAN YÖNETİCİSİ
// ==========================================
enum class Screen { MENU, GAME }

@Composable
fun WordGameApp(onExitApp: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kelime_efsanesi_prefs", Context.MODE_PRIVATE) }
    val soundEngine = remember { ProceduralSoundEngine() }

    val defaultLangCode = remember {
        val sys = Locale.getDefault().language.lowercase()
        if (sys.startsWith("tr")) "tr" else "en"
    }

    var lang by remember {
        val saved = prefs.getString("lang", defaultLangCode) ?: defaultLangCode
        mutableStateOf(if (saved == "tr") Lang.TR else Lang.EN)
    }
    var currentLevel by remember { mutableIntStateOf(prefs.getInt("level", 1).coerceIn(1, 300)) }
    var gold by remember { mutableIntStateOf(prefs.getInt("gold", 150)) }
    var bonusJarCount by remember { mutableIntStateOf(prefs.getInt("bonus_jar", 0).coerceIn(0, 4)) }
    var musicEnabled by remember { mutableStateOf(prefs.getBoolean("music", true)) }
    var sfxEnabled by remember { mutableStateOf(prefs.getBoolean("sfx", true)) }
    var highGraphics by remember { mutableStateOf(prefs.getBoolean("graphics", true)) }
    var lastDailyDay by remember { mutableIntStateOf(prefs.getInt("daily_day", -1)) }

    var screen by remember { mutableStateOf(Screen.MENU) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistoryBook by remember { mutableStateOf(false) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    fun saveState() {
        prefs.edit()
            .putString("lang", lang.code)
            .putInt("level", currentLevel)
            .putInt("gold", gold)
            .putInt("bonus_jar", bonusJarCount)
            .putBoolean("music", musicEnabled)
            .putBoolean("sfx", sfxEnabled)
            .putBoolean("graphics", highGraphics)
            .putInt("daily_day", lastDailyDay)
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

    // Her 10 seviyede bir değişen dinamik dünya teması!
    val worldTheme = getWorldThemeForLevel(currentLevel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = worldTheme.bgGradient))
    ) {
        PuzzleWordBackground(highGraphics = highGraphics, accentColor = worldTheme.accentColor)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            when (screen) {
                Screen.MENU -> {
                    MainMenuScreen(
                        lang = lang,
                        level = currentLevel,
                        gold = gold,
                        worldTheme = worldTheme,
                        onToggleLang = {
                            soundEngine.playClick()
                            lang = if (lang == Lang.TR) Lang.EN else Lang.TR
                            saveState()
                        },
                        onStartGame = {
                            soundEngine.playClick()
                            screen = Screen.GAME
                        },
                        onDailyReward = {
                            soundEngine.playClick()
                            val today = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt()
                            if (lastDailyDay != today) {
                                lastDailyDay = today
                                gold += 75
                                saveState()
                                soundEngine.playLevelWin()
                                bannerMessage = if (lang == Lang.TR) "🎁 Günlük Ödül: +75 Altın Kazandın!" else "🎁 Daily Bonus: +75 Gold Claimed!"
                            } else {
                                soundEngine.playError()
                                bannerMessage = if (lang == Lang.TR) "⏳ Bugünün ödülünü zaten aldın, yarın tekrar gel!" else "⏳ Already claimed today, come back tomorrow!"
                            }
                        },
                        onOpenHistory = {
                            soundEngine.playClick()
                            showHistoryBook = true
                        },
                        onOpenSettings = {
                            soundEngine.playClick()
                            showSettings = true
                        },
                        onExit = {
                            soundEngine.playClick()
                            onExitApp()
                        }
                    )
                }

                Screen.GAME -> {
                    BackHandler {
                        soundEngine.playClick()
                        screen = Screen.MENU
                    }
                    GameplayScreen(
                        lang = lang,
                        level = currentLevel,
                        gold = gold,
                        bonusJarCount = bonusJarCount,
                        worldTheme = worldTheme,
                        soundEngine = soundEngine,
                        onUpdateGoldAndJar = { newGold, newJar ->
                            gold = newGold
                            bonusJarCount = newJar
                            saveState()
                        },
                        onLevelCompleted = { earnedGold ->
                            gold += earnedGold
                            if (currentLevel < 300) currentLevel += 1 else currentLevel = 1
                            saveState()
                        },
                        onBackToMenu = {
                            soundEngine.playClick()
                            screen = Screen.MENU
                        },
                        onOpenSettings = {
                            soundEngine.playClick()
                            showSettings = true
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
            ) {
                LaunchedEffect(bannerMessage) {
                    if (bannerMessage != null) {
                        delay(2500)
                        bannerMessage = null
                    }
                }
                Surface(
                    color = Color(0xFF10B981),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp
                ) {
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

        if (showSettings) {
            SettingsDialog(
                lang = lang,
                musicEnabled = musicEnabled,
                sfxEnabled = sfxEnabled,
                highGraphics = highGraphics,
                onToggleMusic = {
                    soundEngine.playClick()
                    musicEnabled = !musicEnabled
                },
                onToggleSfx = {
                    sfxEnabled = !sfxEnabled
                    soundEngine.sfxEnabled = sfxEnabled
                    soundEngine.playClick()
                },
                onToggleGraphics = {
                    soundEngine.playClick()
                    highGraphics = !highGraphics
                    saveState()
                },
                onResetProgress = {
                    soundEngine.playClick()
                    currentLevel = 1
                    gold = 150
                    bonusJarCount = 0
                    saveState()
                    showSettings = false
                },
                onDismiss = {
                    soundEngine.playClick()
                    showSettings = false
                }
            )
        }

        if (showHistoryBook) {
            HistoryEncyclopediaDialog(
                lang = lang,
                currentLevel = currentLevel,
                onDismiss = {
                    soundEngine.playClick()
                    showHistoryBook = false
                }
            )
        }
    }
}

// ==========================================
// 6. TEMAYA DUYARLI ARKA PLAN + KONFETİ & ŞİMŞEK EFEKTİ
// ==========================================
@Composable
fun PuzzleWordBackground(highGraphics: Boolean, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val floatOffset by if (highGraphics) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float"
        )
    } else {
        remember { mutableFloatStateOf(0.3f) }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val tileSize = w / 7f

        for (r in 0..(h / tileSize).toInt()) {
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
        drawRect(color = Color(0xFF38BDF8).copy(alpha = 0.22f))
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.52f)
            lineTo(w * 0.55f, h * 0.48f)
            lineTo(w * 0.46f, h * 0.26f)
        }
        drawPath(
            path = path,
            color = Color(0xFFFDE047),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ConfettiEffectOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall"
    )
    val colors = remember {
        listOf(
            Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFF38BDF8),
            Color(0xFFF472B6), Color(0xFFA78BFA), Color(0xFFFB7185)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until 45) {
            val seed = i * 37
            val xBase = ((seed * 73) % 100) / 100f * w
            val speed = 0.6f + ((seed % 5) * 0.15f)
            val yPos = ((progress * speed + (seed % 10) * 0.1f) % 1f) * h
            val drift = sin((progress * 6f + i).toDouble()).toFloat() * 24f
            drawCircle(
                color = colors[i % colors.size],
                radius = (8 + (i % 5) * 2).toFloat(),
                center = Offset(xBase + drift, yPos)
            )
        }
    }
}

// ==========================================
// 7. ANA MENÜ EKRANI
// ==========================================
@Composable
fun MainMenuScreen(
    lang: Lang,
    level: Int,
    gold: Int,
    worldTheme: WorldTheme,
    onToggleLang: () -> Unit,
    onStartGame: () -> Unit,
    onDailyReward: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    val league = getLeagueForLevel(level)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onToggleLang,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = lang.flag, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lang.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFFFBBF24))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🪙", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$gold",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val logoLetters = if (lang == Lang.TR) listOf('K', 'E', 'L', 'İ', 'M', 'E') else listOf('W', 'O', 'R', 'D', 'S')
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                logoLetters.forEachIndexed { idx, ch ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(6.dp, RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (idx % 2 == 0) listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                    else worldTheme.openTileGradient
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ch.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (lang == Lang.TR) "🗺️ BÖLGE: ${worldTheme.regionNameTr.uppercase()}"
                else "🗺️ ZONE: ${worldTheme.regionNameEn.uppercase()}",
                color = worldTheme.accentColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, league.color.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = league.icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (lang == Lang.TR) league.nameTr else league.nameEn,
                            color = league.color,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        val nextLeagueLvl = ((level - 1) / 10 + 1) * 10 + 1
                        Text(
                            text = if (lang == Lang.TR) "Seviye $level / 300 • Sonraki Rütbe: Sv.$nextLeagueLvl"
                            else "Level $level / 300 • Next Rank: Lv.$nextLeagueLvl",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MenuButton(
                text = if (lang == Lang.TR) "▶  BAŞLA (SEVİYE $level)" else "▶  PLAY (LEVEL $level)",
                gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                height = 58.dp,
                fontSize = 18,
                onClick = onStartGame
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MenuButton(
                        text = if (lang == Lang.TR) "🎁 Günlük Ödül" else "🎁 Daily Bonus",
                        gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                        height = 48.dp,
                        fontSize = 13,
                        onClick = onDailyReward
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MenuButton(
                        text = if (lang == Lang.TR) "🏛️ Tarih & Lig" else "🏛️ History",
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                        height = 48.dp,
                        fontSize = 13,
                        onClick = onOpenHistory
                    )
                }
            }

            MenuButton(
                text = if (lang == Lang.TR) "⚙️  AYARLAR" else "⚙️  SETTINGS",
                gradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                height = 50.dp,
                fontSize = 16,
                onClick = onOpenSettings
            )

            MenuButton(
                text = if (lang == Lang.TR) "🚪  ÇIKIŞ YAP" else "🚪  EXIT GAME",
                gradient = listOf(Color(0xFFEF4444), Color(0xFFB91C1C)),
                height = 48.dp,
                fontSize = 15,
                onClick = onExit
            )
        }

        Text(
            text = "Galaxy Store Edition • v1.3 (Adım 1 Paketi)",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun MenuButton(
    text: String,
    gradient: List<Color>,
    height: androidx.compose.ui.unit.Dp,
    fontSize: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradient))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp
        )
    }
}

// ==========================================
// 8. OYUN İÇİ EKRANI (ALEV MODU + ÇEKİÇ + KUMBARA + BOSS)
// ==========================================
@Composable
fun GameplayScreen(
    lang: Lang,
    level: Int,
    gold: Int,
    bonusJarCount: Int,
    worldTheme: WorldTheme,
    soundEngine: ProceduralSoundEngine,
    onUpdateGoldAndJar: (Int, Int) -> Unit,
    onLevelCompleted: (Int) -> Unit,
    onBackToMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val puzzle = remember(level, lang) { generateLevelPuzzle(level, lang) }
    var wheelLetters by remember(puzzle) { mutableStateOf(puzzle.wheelLetters) }

    var solvedWords by remember(puzzle) { mutableStateOf(setOf<String>()) }
    var revealedCells by remember(puzzle) { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var foundBonusWords by remember(puzzle) { mutableStateOf(setOf<String>()) }
    var comboCount by remember(puzzle) { mutableIntStateOf(0) }

    // ALEV MODU (FEVER BAR) VE ŞİMŞEK STATE'LERİ
    var feverProgress by remember(puzzle) { mutableFloatStateOf(0f) }
    val isFeverMode = comboCount >= 3 && feverProgress > 0.05f
    var showLightningFlash by remember { mutableStateOf(false) }

    // HEDEFLİ ÇEKİÇ (HAMMER) VE ANTİK KÜP (BONUS JAR) STATE'LERİ
    var isHammerMode by remember(puzzle) { mutableStateOf(false) }
    var showBonusJarDialog by remember { mutableStateOf(false) }

    // BOSS BÖLÜMÜ TAŞLAŞMIŞ HARF MÜHRÜ STATE'İ
    var sealedWheelIndex by remember(puzzle) { mutableStateOf(puzzle.initialSealedWheelIndex) }

    var selectedIndices by remember(puzzle) { mutableStateOf(listOf<Int>()) }
    var currentTouchPos by remember(puzzle) { mutableStateOf<Offset?>(null) }

    var praisePopup by remember { mutableStateOf<String?>(null) }
    var winPraiseTitle by remember(puzzle) { mutableStateOf("") }

    var showTutorialOverlay by remember(level) { mutableStateOf(level == 1) }
    LaunchedEffect(level) {
        if (level == 1) {
            delay(5000)
            showTutorialOverlay = false
        }
    }

    var remainingSeconds by remember(puzzle) { mutableIntStateOf(puzzle.timeLimitSec) }
    var isTimeOut by remember(puzzle) { mutableStateOf(false) }

    var showWinDialog by remember(puzzle) { mutableStateOf(false) }
    var unlockedFact by remember(puzzle) { mutableStateOf<HistoricalFact?>(null) }
    var promotedLeague by remember(puzzle) { mutableStateOf<LeagueInfo?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    // Alev Barı Zamanla Azalma Döngüsü
    LaunchedEffect(puzzle, showWinDialog) {
        while (!showWinDialog) {
            delay(100)
            if (feverProgress > 0f) {
                feverProgress = (feverProgress - 0.013f).coerceAtLeast(0f)
                if (feverProgress <= 0f) {
                    comboCount = 0
                }
            }
        }
    }

    // Tablodaki tüm hücrelerin durumunu hesapla
    val cellMap = remember(puzzle, solvedWords, revealedCells) {
        val map = mutableMapOf<Pair<Int, Int>, Pair<Char, Boolean>>()
        for (pw in puzzle.placedWords) {
            val isWordSolved = pw.word in solvedWords
            for (k in pw.word.indices) {
                val r = if (pw.isHorizontal) pw.row else pw.row + k
                val c = if (pw.isHorizontal) pw.col + k else pw.col
                val pos = r to c
                val prevOpen = map[pos]?.second ?: false
                val isOpen = prevOpen || isWordSolved || (pos in revealedCells)
                map[pos] = pw.word[k] to isOpen
            }
        }
        map
    }

    val allBoardCellsOpen = remember(cellMap) {
        cellMap.isNotEmpty() && cellMap.values.all { it.second }
    }

    fun triggerWinIfReady() {
        if (showWinDialog) return
        soundEngine.playLevelWin()
        winPraiseTitle = if (lang == Lang.TR) LEVEL_WIN_PRAISE_TR.random() else LEVEL_WIN_PRAISE_EN.random()
        if (level % 10 == 0) {
            unlockedFact = HISTORICAL_FACTS.find { it.levelMilestone == level }
                ?: HISTORICAL_FACTS[(level / 10 - 1) % HISTORICAL_FACTS.size].copy(levelMilestone = level)
            promotedLeague = getLeagueForLevel(level + 1)
        }
        showWinDialog = true
    }

    LaunchedEffect(allBoardCellsOpen) {
        if (allBoardCellsOpen && !showWinDialog) {
            delay(350)
            triggerWinIfReady()
        }
    }

    LaunchedEffect(puzzle, solvedWords.size, isTimeOut, showWinDialog) {
        if (puzzle.isTimeAttack && !showWinDialog && !isTimeOut) {
            while (remainingSeconds > 0 && !allBoardCellsOpen) {
                delay(1000)
                remainingSeconds--
            }
            if (remainingSeconds <= 0 && !allBoardCellsOpen) {
                isTimeOut = true
                soundEngine.playError()
            }
        }
    }

    // Şimşek Çakması Efekti Süresi
    LaunchedEffect(showLightningFlash) {
        if (showLightningFlash) {
            delay(650)
            showLightningFlash = false
        }
    }

    fun breakBossSealIfNeeded() {
        if (sealedWheelIndex != null) {
            sealedWheelIndex = null
            soundEngine.playSealBreak()
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
                feverProgress = (feverProgress + 0.48f).coerceAtMost(1f)

                // Boss mührü varsa ilk kelimede kırılır!
                if (sealedWheelIndex != null) {
                    breakBossSealIfNeeded()
                } else {
                    praisePopup = if (lang == Lang.TR) PRAISE_WORDS_TR.random() else PRAISE_WORDS_EN.random()
                }

                // Alev Modu aktifse kelime başına anında 2x ekstra altın!
                if (comboCount >= 3) {
                    onUpdateGoldAndJar(gold + 10, bonusJarCount)
                }

                // 4. Komboda Çarktan Tabloya ŞİMŞEK ÇAKAR ve bedava 1 kutu açar!
                if (comboCount >= 4 && comboCount % 4 == 0) {
                    val closed = cellMap.filter { !it.value.second }.keys.toList()
                    if (closed.isNotEmpty()) {
                        soundEngine.playLightningStrike()
                        showLightningFlash = true
                        revealedCells = revealedCells + closed.random()
                        feedbackBanner = if (lang == Lang.TR) "⚡ ALEV ŞİMŞEĞİ 1 HARF AÇTI! (+10 🪙)" else "⚡ LIGHTNING OPENED A TILE!"
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
                val nextJar = bonusJarCount + 1
                if (nextJar >= 5) {
                    soundEngine.playLevelWin()
                    onUpdateGoldAndJar(gold + 100, 0)
                    praisePopup = if (lang == Lang.TR) "🏺 ANTİK KÜP DOLDU! +100 🪙" else "🏺 BONUS JAR FULL! +100 🪙"
                } else {
                    soundEngine.playBonusWord()
                    onUpdateGoldAndJar(gold + 5, nextJar)
                    feedbackBanner = if (lang == Lang.TR) "🏺 Bonus Kelime ($candidate)! Küp: $nextJar/5 (+5 🪙)"
                    else "🏺 Bonus ($candidate)! Jar: $nextJar/5 (+5 🪙)"
                }
            }
            candidate.length >= 2 -> {
                soundEngine.playError()
                comboCount = 0
                feverProgress = 0f
            }
        }
    }

    val league = getLeagueForLevel(level)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isFeverMode) Modifier.border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. ÜST BAR + BÖLGE İSMİ + ALEV MODU BARI
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToMenu,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Text("⬅", fontSize = 18.sp, color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = league.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (lang == Lang.TR) "SEVİYE $level" else "LEVEL $level",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                        }
                        Text(
                            text = if (lang == Lang.TR) worldTheme.regionNameTr else worldTheme.regionNameEn,
                            color = worldTheme.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24))
                        ) {
                            Text(
                                text = "🪙 $gold",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Text("⚙️", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // BOSS BÖLÜMÜ VEYA ZAMANA KARŞI VEYA ALEV BARI GÖSTERGESİ
                if (puzzle.isBossLevel) {
                    Surface(
                        color = Color(0xFF7F1D1D).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF87171)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == Lang.TR) worldTheme.bossTitleTr else worldTheme.bossTitleEn,
                                color = Color(0xFFFECACA),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (sealedWheelIndex != null) {
                                    if (lang == Lang.TR) "🪨 1 Kelime Bul -> Mührü Kır!" else "🪨 Find 1 Word -> Break Seal!"
                                } else {
                                    if (lang == Lang.TR) "💥 Mühür Kırıldı!" else "💥 Seal Broken!"
                                },
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (puzzle.isTimeAttack) {
                    Text(
                        text = if (lang == Lang.TR) "⏱️ ZAMANA KARŞI YARIŞ: ${remainingSeconds}sn"
                        else "⏱️ TIME ATTACK: ${remainingSeconds}s",
                        color = if (remainingSeconds <= 10) Color(0xFFEF4444) else Color(0xFFFBBF24),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                // İNCE ALEV BARI (FEVER BAR)
                if (feverProgress > 0.02f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFeverMode) "🔥 ALEV 2x!" else "🔥 x$comboCount",
                            color = if (isFeverMode) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(feverProgress.coerceIn(0f, 1f))
                                    .background(
                                        Brush.horizontalGradient(
                                            if (isFeverMode) listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                            else listOf(Color(0xFF38BDF8), Color(0xFF10B981))
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // ÇEKİÇ MODU AKTİF UYARISI VEYA 1. SEVİYE REHBERİ
            AnimatedVisibility(visible = isHammerMode || showTutorialOverlay) {
                Surface(
                    color = if (isHammerMode) Color(0xFFD97706) else Color(0xFF0284C7),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = if (isHammerMode) {
                            if (lang == Lang.TR) "🔨 ÇEKİÇ AKTİF: Yukarıdaki tabloda kırmak istediğin kapalı kutuya dokun!"
                            else "🔨 HAMMER READY: Tap any closed tile on the board to smash it open!"
                        } else {
                            if (lang == Lang.TR) "👋 Parmağını aşağıdaki dev çarkta kaydırarak kelimeleri bul!"
                            else "👋 Swipe across the wheel below to connect letters!"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // 2. BULMACA TABLOSU (Hedefli Çekiç Dokunuşuna Duyarlı)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxW = maxWidth
                val maxH = maxHeight
                val cellW = (maxW / (puzzle.gridCols + 1).coerceAtLeast(4)).coerceAtMost(46.dp)
                val cellH = (maxH / (puzzle.gridRows + 1).coerceAtLeast(4)).coerceAtMost(46.dp)
                val cellSize = minOf(cellW, cellH).coerceAtLeast(22.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (r in 0 until puzzle.gridRows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (c in 0 until puzzle.gridCols) {
                                val pos = r to c
                                val cellData = cellMap[pos]
                                if (cellData != null) {
                                    val (ch, isOpen) = cellData
                                    val popScale by animateFloatAsState(
                                        targetValue = if (isOpen) 1f else if (isHammerMode) 0.96f else 0.92f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "cellPop"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .scale(popScale)
                                            .shadow(if (isOpen) 6.dp else 2.dp, RoundedCornerShape(9.dp))
                                            .background(
                                                if (isOpen) Brush.verticalGradient(worldTheme.openTileGradient)
                                                else Brush.verticalGradient(
                                                    listOf(Color(0xFF334155), Color(0xFF1E293B))
                                                ),
                                                RoundedCornerShape(9.dp)
                                            )
                                            .border(
                                                width = if (isHammerMode && !isOpen) 2.dp else 1.5.dp,
                                                color = when {
                                                    isOpen -> Color.White.copy(alpha = 0.7f)
                                                    isHammerMode -> Color(0xFFFBBF24)
                                                    else -> Color.White.copy(alpha = 0.25f)
                                                },
                                                shape = RoundedCornerShape(9.dp)
                                            )
                                            .clickable(enabled = isHammerMode && !isOpen) {
                                                // HEDEFLİ ÇEKİÇ VURUŞU!
                                                soundEngine.playHammerHit()
                                                onUpdateGoldAndJar(gold - 35, bonusJarCount)
                                                revealedCells = revealedCells + pos
                                                isHammerMode = false
                                                breakBossSealIfNeeded()
                                                praisePopup = if (lang == Lang.TR) "🔨 KUTU KIRILDI!" else "🔨 TILE SMASHED!"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isOpen) {
                                            Text(
                                                text = ch.toString(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = (cellSize.value * 0.50f).sp
                                            )
                                        } else if (isHammerMode) {
                                            Text(
                                                text = "🎯",
                                                fontSize = (cellSize.value * 0.36f).sp
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(cellSize))
                                }
                            }
                        }
                    }
                }
            }

            // ANLIK KELİME ÖNİZLEME BALONU
            val currentPreview = selectedIndices.map { wheelLetters[it] }.joinToString("")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentPreview.isNotEmpty()) {
                    Surface(
                        color = worldTheme.accentColor,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = currentPreview,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                } else if (feedbackBanner != null) {
                    LaunchedEffect(feedbackBanner) {
                        delay(1600)
                        feedbackBanner = null
                    }
                    Text(
                        text = feedbackBanner ?: "",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. GÜÇ BARI (🏺 Kumbara + 🔀 Karıştır + 💡 İpucu + 🔨 Çekiç + 🚀 Roket)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SOL: 🏺 Bonus Kelime Kumbarası + 🔀 Karıştır
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = {
                            soundEngine.playClick()
                            showBonusJarDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF78350F).copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏺", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$bonusJarCount/5",
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            soundEngine.playClick()
                            // Mühür varsa mühürlü harfin indeksini koruyarak karıştır
                            val currentSealedChar = sealedWheelIndex?.let { wheelLetters[it] }
                            wheelLetters = wheelLetters.shuffled()
                            if (currentSealedChar != null) {
                                sealedWheelIndex = wheelLetters.indexOf(currentSealedChar).takeIf { it >= 0 }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔀", fontSize = 15.sp)
                        }
                    }
                }

                // SAĞ: 💡 Rastgele İpucu (25) + 🔨 Hedefli Çekiç (35) + 🚀 3'lü Roket (60)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 💡 Rastgele 1 Harf Aç (25 Altın)
                    Surface(
                        onClick = {
                            val unrevealed = cellMap.filter { !it.value.second }.keys.toList()
                            if (gold >= 25 && unrevealed.isNotEmpty()) {
                                soundEngine.playClick()
                                onUpdateGoldAndJar(gold - 25, bonusJarCount)
                                revealedCells = revealedCells + unrevealed.random()
                                breakBossSealIfNeeded()
                            } else if (gold < 25) {
                                soundEngine.playError()
                                feedbackBanner = if (lang == Lang.TR) "İpucu için 25 🪙!" else "Need 25 🪙!"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("25", color = Color(0xFFFBBF24), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    }

                    // 🔨 Hedefli Çekiç: İstediğin Kutuya Dokunup Aç (35 Altın)
                    Surface(
                        onClick = {
                            if (isHammerMode) {
                                soundEngine.playClick()
                                isHammerMode = false
                            } else if (gold >= 35) {
                                soundEngine.playClick()
                                isHammerMode = true
                            } else {
                                soundEngine.playError()
                                feedbackBanner = if (lang == Lang.TR) "Çekiç için 35 🪙!" else "Need 35 🪙!"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isHammerMode) Color(0xFFF59E0B) else Color(0xFFEA580C).copy(alpha = 0.28f),
                        border = BorderStroke(1.5.dp, if (isHammerMode) Color.White else Color(0xFFFB923C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔨", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "35",
                                color = if (isHammerMode) Color.Black else Color(0xFFFDBA74),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // 🚀 3 Harf Birden Açan Roket (60 Altın)
                    Surface(
                        onClick = {
                            val unrevealed = cellMap.filter { !it.value.second }.keys.shuffled()
                            if (gold >= 60 && unrevealed.isNotEmpty()) {
                                soundEngine.playBonusWord()
                                onUpdateGoldAndJar(gold - 60, bonusJarCount)
                                revealedCells = revealedCells + unrevealed.take(3)
                                breakBossSealIfNeeded()
                            } else if (gold < 60) {
                                soundEngine.playError()
                                feedbackBanner = if (lang == Lang.TR) "Roket için 60 🪙!" else "Need 60 🪙!"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEC4899).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFFF472B6))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚀", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("60", color = Color(0xFFF472B6), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 4. BÜYÜK & ÖZEL TASARIMLI NEON PUSULA HARF ÇARKI (Boss Mührü Destekli)
            WordSwipeWheel(
                letters = wheelLetters,
                selectedIndices = selectedIndices,
                sealedIndex = sealedWheelIndex,
                currentTouchPos = currentTouchPos,
                comboCount = comboCount,
                accentColor = worldTheme.accentColor,
                showTutorialPulse = showTutorialOverlay,
                onSelectIndex = { idx ->
                    if (idx == sealedWheelIndex) {
                        soundEngine.playError()
                        feedbackBanner = if (lang == Lang.TR) "🗿 Bu harf mühürlü! Önce diğer harflerle 1 kelime bul!"
                        else "🗿 Letter sealed! Find 1 word with other letters first!"
                    } else if (idx !in selectedIndices) {
                        selectedIndices = selectedIndices + idx
                        soundEngine.playSwipeNote(selectedIndices.size - 1)
                    }
                },
                onUpdateTouch = { pos -> currentTouchPos = pos },
                onDragEnd = { submitSwipedWord() }
            )
        }

        // ŞİMŞEK ÇAKMASI GÖRSEL EFEKTİ
        if (showLightningFlash) {
            LightningFlashOverlay()
        }

        // DOPİNG / ÖVGÜ ROZETİ
        AnimatedVisibility(
            visible = praisePopup != null,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
        ) {
            LaunchedEffect(praisePopup) {
                if (praisePopup != null) {
                    delay(1150)
                    praisePopup = null
                }
            }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.94f),
                border = BorderStroke(2.dp, Color(0xFFFBBF24)),
                shadowElevation = 12.dp
            ) {
                Text(
                    text = praisePopup ?: "",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }
        }
    }

    // 🏺 ANTİK KÜP (BONUS KELİME KUMBARASI) PENCERESİ
    if (showBonusJarDialog) {
        Dialog(onDismissRequest = { showBonusJarDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(2.dp, Color(0xFFF59E0B))
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏺", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (lang == Lang.TR) "ANTİK KELİME KÜPÜ ($bonusJarCount / 5)"
                        else "ANCIENT BONUS JAR ($bonusJarCount / 5)",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (lang == Lang.TR)
                            "Tabloda olmayan geçerli kelimeler buldukça küp dolar. Her 5 bonus kelimede küp kırılır ve +100 🪙 verir!"
                        else
                            "Find extra valid words not on the board. Every 5 bonus words breaks the jar for +100 🪙!",
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (lang == Lang.TR) "Bu Bölümde Bulduğun Bonus Kelimeler:"
                                else "Bonus Words Found This Level:",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (foundBonusWords.isEmpty()) {
                                    if (lang == Lang.TR) "Henüz yok — çarktan farklı kelimeler dene!" else "None yet — try swiping extra words!"
                                } else {
                                    foundBonusWords.joinToString(", ")
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showBonusJarDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = if (lang == Lang.TR) "Tamam" else "Close",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    // ZAMANA KARŞI SÜRE BİTTİ PENCERESİ
    if (isTimeOut) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(2.dp, Color(0xFFEF4444))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⏰", fontSize = 44.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == Lang.TR) "SÜRE DOLDU!" else "TIME'S UP!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == Lang.TR) "Zamana karşı bonus seviyede süren bitti. Ek süre alıp devam etmek ister misin?"
                        else "You ran out of time in the Time Attack bonus round. Want extra time?",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    MenuButton(
                        text = if (lang == Lang.TR) "+30 Saniye Ekle (Ücretsiz)" else "+30 Seconds (Free)",
                        gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                        height = 46.dp,
                        fontSize = 14,
                        onClick = {
                            soundEngine.playClick()
                            remainingSeconds = 30
                            isTimeOut = false
                        }
                    )
                }
            }
        }
    }

    // SEVİYE TAMAMLANDI + KONFETİ + BOSS HAZİNESİ + RÜTBE & TARİH KARTI
    if (showWinDialog) {
        val baseReward = 25
        val timeBonus = if (puzzle.isTimeAttack) remainingSeconds * 2 else 0
        val bossBonus = if (puzzle.isBossLevel) 100 else 0
        val leagueBonus = if (promotedLeague != null) 75 else 0
        val totalEarned = baseReward + timeBonus + bossBonus + leagueBonus

        Dialog(onDismissRequest = {}) {
            Box(contentAlignment = Alignment.Center) {
                ConfettiEffectOverlay()

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(2.5.dp, Color(0xFFFBBF24)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (puzzle.isBossLevel) "🗿👑🗿" else "⭐⭐⭐", fontSize = 30.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (puzzle.isBossLevel) {
                                if (lang == Lang.TR) "🎉 MUHAFIZ YENİLDİ! (SV.$level)" else "🎉 GUARDIAN DEFEATED! (LV.$level)"
                            } else {
                                if (lang == Lang.TR) "🎉 SEVİYE $level GEÇİLDİ!" else "🎉 LEVEL $level CLEARED!"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = winPraiseTitle,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.dp, Color(0xFF34D399))
                        ) {
                            Text(
                                text = if (lang == Lang.TR) "Toplam Ödül: +$totalEarned 🪙" else "Total Reward: +$totalEarned 🪙",
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        promotedLeague?.let { newLg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            val nextWorld = getWorldThemeForLevel(level + 1)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = newLg.color.copy(alpha = 0.16f),
                                border = BorderStroke(1.5.dp, newLg.color)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = newLg.icon, fontSize = 28.sp)
                                    Text(
                                        text = if (lang == Lang.TR) "YENİ RÜTBE & YENİ BÖLGE!\n${newLg.nameTr} • ${nextWorld.regionNameTr}"
                                        else "NEW RANK & NEW ZONE!\n${newLg.nameEn} • ${nextWorld.regionNameEn}",
                                        color = newLg.color,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        unlockedFact?.let { fact ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (lang == Lang.TR) "🏛️ TARİH KÖŞESİ: ${fact.titleTr}"
                                        else "🏛️ HISTORICAL FACT: ${fact.titleEn}",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (lang == Lang.TR) fact.infoTr else fact.infoEn,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        MenuButton(
                            text = if (lang == Lang.TR) "SEVİYE ${level + 1}'E GEÇ ➔" else "CONTINUE TO LEVEL ${level + 1} ➔",
                            gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                            height = 50.dp,
                            fontSize = 16,
                            onClick = {
                                soundEngine.playClick()
                                showWinDialog = false
                                onLevelCompleted(totalEarned)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. NEON PUSULA HARF ÇARKI (TAŞLAŞMIŞ BOSS MÜHRÜ DESTEKLİ)
// ==========================================
@Composable
fun WordSwipeWheel(
    letters: List<Char>,
    selectedIndices: List<Int>,
    sealedIndex: Int?,
    currentTouchPos: Offset?,
    comboCount: Int,
    accentColor: Color,
    showTutorialPulse: Boolean,
    onSelectIndex: (Int) -> Unit,
    onUpdateTouch: (Offset?) -> Unit,
    onDragEnd: () -> Unit
) {
    val wheelDiameter = 248.dp
    val density = LocalDensity.current
    val wheelPx = with(density) { wheelDiameter.toPx() }
    val center = Offset(wheelPx / 2f, wheelPx / 2f)
    val orbitRadius = wheelPx * 0.35f
    val nodeRadius = (wheelPx * (if (letters.size <= 5) 0.125f else if (letters.size <= 7) 0.11f else 0.098f))

    val letterCenters = remember(letters, wheelPx) {
        letters.indices.map { i ->
            val angle = -PI / 2 + (2 * PI * i / letters.size)
            Offset(
                x = center.x + (orbitRadius * cos(angle)).toFloat(),
                y = center.y + (orbitRadius * sin(angle)).toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wheelAnim")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "ringRot"
    )
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "corePulse"
    )

    Box(
        modifier = Modifier
            .size(wheelDiameter)
            .pointerInput(letters, sealedIndex) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        onUpdateTouch(startOffset)
                        letterCenters.forEachIndexed { idx, c ->
                            if ((startOffset - c).getDistance() <= nodeRadius * 1.32f) {
                                onSelectIndex(idx)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        onUpdateTouch(pos)
                        letterCenters.forEachIndexed { idx, c ->
                            if ((pos - c).getDistance() <= nodeRadius * 1.25f) {
                                onSelectIndex(idx)
                            }
                        }
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF090D16)),
                    center = center,
                    radius = wheelPx / 2f
                ),
                radius = wheelPx / 2f,
                center = center
            )

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(accentColor, Color(0xFFF59E0B), Color(0xFFA855F7), accentColor),
                    center = center
                ),
                radius = wheelPx / 2f - 3f,
                center = center,
                style = Stroke(width = 5f)
            )

            rotate(degrees = ringRotation, pivot = center) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.35f),
                    radius = orbitRadius,
                    center = center,
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                )
            }

            letterCenters.forEach { lc ->
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = center,
                    end = lc,
                    strokeWidth = 2.5f
                )
            }

            val activeGlow = if (selectedIndices.isNotEmpty()) Color(0xFFF59E0B) else accentColor
            drawCircle(
                color = activeGlow.copy(alpha = 0.18f),
                radius = 28f * corePulse,
                center = center
            )
            drawCircle(
                color = activeGlow.copy(alpha = 0.55f),
                radius = 12f,
                center = center
            )

            if (selectedIndices.isNotEmpty()) {
                val path = Path()
                val firstPt = letterCenters[selectedIndices.first()]
                path.moveTo(firstPt.x, firstPt.y)
                for (i in 1 until selectedIndices.size) {
                    val pt = letterCenters[selectedIndices[i]]
                    path.lineTo(pt.x, pt.y)
                }
                currentTouchPos?.let { touch ->
                    path.lineTo(touch.x, touch.y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = path,
                    color = Color(0xFFFDE047),
                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                )
            }

            letterCenters.forEachIndexed { idx, c ->
                val isSelected = idx in selectedIndices
                val isSealed = (idx == sealedIndex)
                val currentRadius = if (isSelected) nodeRadius * 1.18f else nodeRadius

                if (isSelected) {
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                        radius = currentRadius * 1.3f,
                        center = c
                    )
                }

                drawCircle(
                    brush = when {
                        isSealed -> Brush.verticalGradient(
                            colors = listOf(Color(0xFF57534E), Color(0xFF292524)),
                            startY = c.y - currentRadius,
                            endY = c.y + currentRadius
                        )
                        isSelected -> Brush.verticalGradient(
                            colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                            startY = c.y - currentRadius,
                            endY = c.y + currentRadius
                        )
                        else -> Brush.verticalGradient(
                            colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                            startY = c.y - currentRadius,
                            endY = c.y + currentRadius
                        )
                    },
                    radius = currentRadius,
                    center = c
                )

                drawCircle(
                    color = when {
                        isSealed -> Color(0xFFEF4444)
                        isSelected -> Color.White
                        else -> accentColor
                    },
                    radius = currentRadius,
                    center = c,
                    style = Stroke(width = if (isSelected || isSealed) 5f else 3.5f)
                )
            }
        }

        Text(
            text = if (comboCount >= 2) "🔥x$comboCount" else "✦",
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Black,
            fontSize = if (comboCount >= 2) 13.sp else 16.sp
        )

        letters.forEachIndexed { idx, ch ->
            val c = letterCenters[idx]
            val isSelected = idx in selectedIndices
            val isSealed = (idx == sealedIndex)
            val scaleAnim by animateFloatAsState(
                targetValue = if (isSelected) 1.22f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "letterScale"
            )
            val offsetX = with(density) { (c.x - wheelPx / 2f).toDp() }
            val offsetY = with(density) { (c.y - wheelPx / 2f).toDp() }
            Text(
                text = if (isSealed) "🪨$ch" else ch.toString(),
                color = when {
                    isSealed -> Color(0xFFFCA5A5)
                    isSelected -> Color(0xFF0F172A)
                    else -> Color.White
                },
                fontWeight = FontWeight.Black,
                fontSize = (if (isSealed) 16 else if (letters.size <= 6) 23 else 20).sp,
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .scale(scaleAnim)
            )
        }
    }
}

// ==========================================
// 10. AYARLAR & TARİH ANSİKLOPEDİSİ PENCERELERİ
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
    onResetProgress: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (lang == Lang.TR) "⚙️ OYUN AYARLARI" else "⚙️ GAME SETTINGS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp
                )

                SettingRow(
                    title = if (lang == Lang.TR) "🎵 Arka Plan Müziği" else "🎵 Background Music",
                    checked = musicEnabled,
                    onToggle = onToggleMusic
                )

                SettingRow(
                    title = if (lang == Lang.TR) "🔊 Buton & Çark Sesleri" else "🔊 Sound Effects (SFX)",
                    checked = sfxEnabled,
                    onToggle = onToggleSfx
                )

                SettingRow(
                    title = if (lang == Lang.TR) "✨ Yüksek Grafik & Animasyon" else "✨ High Graphics",
                    checked = highGraphics,
                    onToggle = onToggleGraphics
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onResetProgress) {
                        Text(
                            text = if (lang == Lang.TR) "İlerlemeyi Sıfırla" else "Reset Save",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(
                            text = if (lang == Lang.TR) "Tamam" else "Done",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
fun HistoryEncyclopediaDialog(
    lang: Lang,
    currentLevel: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
            modifier = Modifier.fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = if (lang == Lang.TR) "🏛️ TARİH KÖŞESİ & LİGLER" else "🏛️ HISTORY & LEAGUES",
                    color = Color(0xFFC4B5FD),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = if (lang == Lang.TR) "Her 10 seviyede bir yeni tarihi eser ve yeni rütbe açılır!"
                    else "A new historical landmark and rank unlocks every 10 levels!",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(HISTORICAL_FACTS) { fact ->
                        val isUnlocked = currentLevel > fact.levelMilestone
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isUnlocked) Color(0xFF1E293B) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                1.dp,
                                if (isUnlocked) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Text(
                                    text = if (isUnlocked) {
                                        if (lang == Lang.TR) "✅ Sv.${fact.levelMilestone} - ${fact.titleTr}"
                                        else "✅ Lv.${fact.levelMilestone} - ${fact.titleEn}"
                                    } else {
                                        if (lang == Lang.TR) "🔒 Seviye ${fact.levelMilestone} Bittiğinde Açılır"
                                        else "🔒 Unlocks at Level ${fact.levelMilestone}"
                                    },
                                    color = if (isUnlocked) Color(0xFF38BDF8) else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (isUnlocked) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (lang == Lang.TR) fact.infoTr else fact.infoEn,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text(
                        text = if (lang == Lang.TR) "Kapat" else "Close",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

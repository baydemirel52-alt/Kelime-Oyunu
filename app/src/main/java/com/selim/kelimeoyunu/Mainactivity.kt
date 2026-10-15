package com.selim.kelimeoyunu // Kendi paket adınla değiştir kanka

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
        // Çentikli (Notch) ve delikli ekranlara tam uyum sağlar
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

    // Harf seçerken yükselen nota frekansları (Do, Re, Mi, Fa, Sol, La, Si, İnce Do)
    private val swipeNotes = doubleArrayOf(523.25, 587.33, 659.25, 698.46, 783.99, 880.00, 987.77, 1046.50)

    fun playClick() {
        if (!sfxEnabled) return
        playTone(600.0, 45, 0.25f)
    }

    fun playSwipeNote(index: Int) {
        if (!sfxEnabled) return
        val note = swipeNotes[index.coerceIn(0, swipeNotes.lastIndex)]
        playTone(note, 70, 0.3f)
    }

    fun playWordSuccess() {
        if (!sfxEnabled) return
        scope.launch {
            playToneSync(523.25, 80, 0.3f)
            playToneSync(659.25, 80, 0.3f)
            playToneSync(783.99, 140, 0.35f)
        }
    }

    fun playBonusWord() {
        if (!sfxEnabled) return
        scope.launch {
            playToneSync(587.33, 70, 0.3f)
            playToneSync(880.00, 130, 0.35f)
        }
    }

    fun playError() {
        if (!sfxEnabled) return
        playTone(190.0, 120, 0.25f)
    }

    fun playLevelWin() {
        if (!sfxEnabled) return
        scope.launch {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            for (n in notes) {
                playToneSync(n, 110, 0.35f)
            }
        }
    }

    fun updateBackgroundMusic() {
        musicJob?.cancel()
        if (!musicEnabled || !isAppInForeground) return
        musicJob = scope.launch {
            // Sakin, telifsiz pentatonik ambiyans melodisi
            val melody = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 392.00, 329.63, 293.66)
            var idx = 0
            while (isActive && musicEnabled && isAppInForeground) {
                playToneSync(melody[idx % melody.size], 280, 0.06f)
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
                // Yumuşak giriş/çıkış zarfı (tık sesi yapmaması için)
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
// 2. DİL, TARİHİ BİLGİLER, LİG & KELİME HAVUZU
// ==========================================
enum class Lang(val code: String, val flag: String, val label: String) {
    TR("tr", "🇹🇷", "TR"),
    EN("en", "🇬🇧", "EN")
}

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
    val step = (level - 1) / 20
    return when (step) {
        0 -> LeagueInfo(0, "Bronz Ligi", "Bronze League", "🥉", Color(0xFFCD7F32))
        1 -> LeagueInfo(1, "Gümüş Ligi", "Silver League", "🥈", Color(0xFFC0C0C0))
        2 -> LeagueInfo(2, "Altın Ligi", "Gold League", "🥇", Color(0xFFFFD700))
        3 -> LeagueInfo(3, "Platin Ligi", "Platinum League", "💎", Color(0xFF00E5FF))
        4 -> LeagueInfo(4, "Elmas Ligi", "Diamond League", "👑", Color(0xFFB388FF))
        5 -> LeagueInfo(5, "Usta Ligi", "Master League", "🔥", Color(0xFFFF5252))
        6 -> LeagueInfo(6, "Büyük Usta Ligi", "Grandmaster League", "⚡", Color(0xFFFFAB00))
        else -> LeagueInfo(7, "Efsane Ligi", "Legend League", "🌟", Color(0xFF69F0AE))
    }
}

data class WordPack(val letters: String, val words: List<String>)

val TR_WORD_PACKS = listOf(
    // Seviye 1: 5 saniyelik kolay tanıtım bölümü
    WordPack("BAL", listOf("BAL", "AL", "AB")),
    // Kolay (3-4 Harf)
    WordPack("KALE", listOf("KALE", "ELKA", "KELA", "KAL", "ELA", "LAK", "EK", "AL", "EL")),
    WordPack("MASA", listOf("MASA", "ASMA", "SAMA", "AMA", "ASA", "SAM", "AS", "AM")),
    WordPack("KAYA", listOf("KAYA", "AYAK", "YAKA", "KAY", "AYA", "YAK", "AK", "AY")),
    WordPack("SIRA", listOf("SIRA", "ASIR", "SARI", "SIR", "ARI", "ASI", "AS", "IR")),
    WordPack(" KAPI".trim(), listOf("KAPI", "PAKI", "KAP", "PAK", "AKI", "AK", "İP")),
    WordPack("DERT", listOf("DERT", "TER", "RET", "DAR", "ET", "ER")),
    WordPack("YURT", listOf("YURT", "TUR", "YUT", "TUY", "UR", "UT")),
    WordPack("RENK", listOf("RENK", "KEN", "KER", "ERK", "EK", "ER", "EN")),
    WordPack("DOST", listOf("DOST", "SOD", "TOS", "OT", "OD")),
    // Orta (5 Harf)
    WordPack("KALEM", listOf("KALEM", "KEMAL", "EMLAK", "KALE", "ELMA", "ALEM", "KAME", "KAL", "KEM", "ELA", "MAL", "LAK")),
    WordPack("KİTAP", listOf("KİTAP", "TAKİP", "PATİK", "KATİ", "PAKİ", "KAPI", "KAP", "TAK", "TİP", "KİT", "PAK", "ait".uppercase())),
    WordPack("DENİZ", listOf("DENİZ", "ZİNDE", "DİZE", "EDİN", "İZEN", "DİN", "DİZ", "ZEN", "İDE", "İZ", "İN")),
    WordPack("TARİH", listOf("TARİH", "TAHİR", "İHTAR", "HART", "AHİR", "TAR", "HAT", "HAR", "AİT", "AH", "AT")),
    WordPack("SEVGİ", listOf("SEVGİ", "VERGİ", "EVGİ", "SEVİ", "SEV", "KES", "GİV", "EV", "ES", "İS")),
    WordPack("BAHAR", listOf("BAHAR", "HARAB", "ARAB", "HARA", "BARA", "BAR", "HAR", "ABA", "ARA", "AH", "AB")),
    WordPack("ÇİÇEK", listOf("ÇİÇEK", "KEÇİ", "ÇEKİ", "ÇEK", "KEÇ", "ÇİÇ", "EK", "İÇ")),
    WordPack("GÜNEŞ", listOf("GÜNEŞ", "ŞEN", "GÜN", "GEN", "ÜŞE", "NE", "EŞ", "ÜN")),
    WordPack("YILDIZ", listOf("YILDIZ", "YILI", "DILI", "YIL", "DIL", "ZIT", "DIZ", "İZ")),
    WordPack("ORMAN", listOf("ORMAN", "ROMAN", "ONAR", "NORM", "ORAN", "MOR", "NAR", "NAM", "OMA", "AN", "ON")),
    // Zor (6-7 Harf)
    WordPack("KARTAL", listOf("KARTAL", "TARLAK", "KARAT", "TARLA", "KART", "KARA", "TAKA", "ARKA", "KALA", "KAR", "KAT", "TAK", "TAL", "ARA")),
    WordPack("MİMARİ", listOf("MİMARİ", "MİMAR", "İMAM", "MİRİ", "RAMİ", "MİR", "İMA", "MİM", "İRİ", "Mİ")),
    WordPack("EFSANE", listOf("EFSANE", "NEFES", "ESNAF", "SENE", "ENSE", "SEFA", "FENA", "FEN", "SEN", "SAN", "FAS", "AN", "AS")),
    WordPack("DESTAN", listOf("DESTAN", "SANET", "SEDA", "DANS", "SENA", "ESNA", "TANE", "STEN", "NET", "SEN", "TAN", "TAS", "DAL")),
    WordPack("ZAFER", listOf("ZAFER", "FERAZ", "FARE", "FEZA", "ARZ", "FAR", "ZAR", "FER", "ZER", "AZ", "ER")),
    WordPack("KAHRAMAN", listOf("KAHRAMAN", "KARAMAN", "HARMAN", "MAKARA", "ANKARA", "KARMA", "MARKA", "KARA", "HARA", "KAMA", "MANA", "KAYA")),
    WordPack("BAŞKENT", listOf("BAŞKENT", "Şebek".uppercase(), "BANKET", "BAŞAK", "KENAT", "KENT", "BANK", "TANE", "BAŞ", "BEŞ", "KAŞ", "TAŞ", "KAN", "KAT"))
)

val EN_WORD_PACKS = listOf(
    WordPack("CAT", listOf("CAT", "ACT", "AT")),
    WordPack("STAR", listOf("STAR", "RATS", "ARTS", "TARS", "ART", "RAT", "TAR", "SAT", "AT", "AS")),
    WordPack("TIME", listOf("TIME", "ITEM", "EMIT", "MITE", "TIE", "MET", "IT", "ME")),
    WordPack("WORD", listOf("WORD", "ROW", "ROD", "DOW", "DO", "OR")),
    WordPack("GOLD", listOf("GOLD", "GOD", "DOG", "LOG", "OLD", "GO", "DO")),
    WordPack("STONE", listOf("STONE", "NOTES", "TONES", "ONSET", "NOTE", "TONE", "SENT", "NEST", "NOSE", "ONE", "SON", "NET", "SET", "TEN")),
    WordPack("WATER", listOf("WATER", "RATE", "TEAR", "WEAR", "WARE", "WAR", "ART", "EAR", "EAT", "TEA", "RAW")),
    WordPack("HEART", listOf("HEART", "EARTH", "HATER", "HEAR", "HEAT", "HATE", "RATE", "TEAR", "ART", "EAR", "EAT", "HAT", "TEA")),
    WordPack("SMART", listOf("SMART", "MARTS", "STAR", "MARS", "ARMS", "ARTS", "RATS", "MATS", "ART", "ARM", "MAR", "MAT", "RAM", "RAT", "SAT")),
    WordPack("CROWN", listOf("CROWN", "CORN", "CROW", "WORN", "COW", "ROW", "NOW", "OWN", "WON")),
    WordPack("PLANET", listOf("PLANET", "PLANT", "PLATE", "PANEL", "PLANE", "LEANT", "PETAL", "PLAN", "LATE", "TALE", "PALE", "LEAP", "PEAL", "NEAT", "TAPE")),
    WordPack("CASTLE", listOf("CASTLE", "CLEATS", "SCALE", "SLATE", "STALE", "TALES", "LEAST", "STEAL", "CAST", "LATE", "CATS", "EAST", "SEAT", "SALE", "SEAL")),
    WordPack("LEGEND", listOf("LEGEND", "GLEED", "LEDGE", "GENE", "NEED", "EDGE", "LEND", "GLEE", "END", "DEN", "LEG", "GEL"))
)

// ==========================================
// 3. BULMACA TABLOSU (CROSSWORD) ÜRETİCİSİ
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
    val timeLimitSec: Int
)

fun generateLevelPuzzle(level: Int, lang: Lang): LevelPuzzle {
    val packs = if (lang == Lang.TR) TR_WORD_PACKS else EN_WORD_PACKS
    val pack = if (level == 1) {
        packs.first()
    } else {
        // Seviye ilerledikçe kolaydan zora doğru paket seçimi
        val maxIdx = ((level.toFloat() / 300f) * packs.size).toInt().coerceIn(2, packs.size - 1)
        val minIdx = (maxIdx - 5).coerceAtLeast(1)
        val rng = Random(level * 997 + lang.ordinal * 31)
        packs[rng.nextInt(minIdx, maxIdx + 1)]
    }

    val rng = Random(level * 1337)
    val sortedWords = pack.words.distinct().sortedByDescending { it.length }
    val targetCount = when {
        level == 1 -> 2
        level < 10 -> 3
        level < 40 -> 4
        level < 120 -> 5
        else -> 6
    }.coerceAtMost(sortedWords.size)

    val mainWord = sortedWords.first()
    val candidates = sortedWords.drop(1).shuffled(rng)
    val selectedForBoard = mutableListOf(mainWord)
    selectedForBoard.addAll(candidates.take(targetCount - 1))

    // Kelimeleri sanal 15x15 tabloya kesiştirerek yerleştir
    val boardSize = 16
    val grid = Array(boardSize) { CharArray(boardSize) { ' ' } }
    val placed = mutableListOf<PlacedWord>()

    // İlk kelimeyi ortaya yatay koy
    val startRow = boardSize / 2
    val startCol = (boardSize - mainWord.length) / 2
    for (i in mainWord.indices) {
        grid[startRow][startCol + i] = mainWord[i]
    }
    placed.add(PlacedWord(mainWord, startRow, startCol, true))

    // Diğer kelimeleri ortak harflerden kesiştirmeyi dene
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
        // Kesişemezse temiz bir alt satıra yatay yerleştir (asla kelime kaybolmaz)
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

    // Sınırları kırp (normalize et) ki ekrana tam ortalansın
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
    val bonusSet = pack.words.filter { it !in boardWordSet }.toSet()

    val wheelChars = pack.letters.toList().shuffled(rng)
    val isTimeAttack = (level % 15 == 0)
    val timeLimit = if (isTimeAttack) (45 + normalizedPlaced.size * 8) else 0

    return LevelPuzzle(
        level = level,
        wheelLetters = wheelChars,
        placedWords = normalizedPlaced,
        bonusWords = bonusSet,
        gridRows = (maxR - minR + 1).coerceAtLeast(1),
        gridCols = (maxC - minC + 1).coerceAtLeast(1),
        isTimeAttack = isTimeAttack,
        timeLimitSec = timeLimit
    )
}

private fun canPlaceWord(grid: Array<CharArray>, word: String, r: Int, c: Int, isHoriz: Boolean): Boolean {
    val size = grid.size
    if (r < 1 || c < 1) return false
    if (isHoriz && c + word.length >= size - 1) return false
    if (!isHoriz && r + word.length >= size - 1) return false

    // Başlangıç öncesi ve bitiş sonrası boş olmalı
    if (isHoriz) {
        if (grid[r][c - 1] != ' ' || grid[r][c + word.length] != ' ') return false
    } else {
        if (grid[r - 1][c] != ' ' || grid[r + word.length][c] != ' ') return false
    }

    for (i in word.indices) {
        val nr = if (isHoriz) r else r + i
        val nc = if (isHoriz) c + i else c
        val current = grid[nr][nc]
        if (current != ' ' && current != word[i]) return false
        if (current == ' ') {
            // Yan komşular boş olmalı
            if (isHoriz) {
                if (grid[nr - 1][nc] != ' ' || grid[nr + 1][nc] != ' ') return false
            } else {
                if (grid[nr][nc - 1] != ' ' || grid[nr][nc + 1] != ' ') return false
            }
        }
    }
    return true
}

// ==========================================
// 4. ANA UYGULAMA VE EKRAN YÖNETİCİSİ
// ==========================================
enum class Screen { MENU, GAME }

@Composable
fun WordGameApp(onExitApp: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kelime_efsanesi_prefs", Context.MODE_PRIVATE) }
    val soundEngine = remember { ProceduralSoundEngine() }

    // Telefon dilini otomatik algıla (ilk açılışta)
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
    var musicEnabled by remember { mutableStateOf(prefs.getBoolean("music", true)) }
    var sfxEnabled by remember { mutableStateOf(prefs.getBoolean("sfx", true)) }
    var highGraphics by remember { mutableStateOf(prefs.getBoolean("graphics", true)) }
    var lastDailyDay by remember { mutableIntStateOf(prefs.getInt("daily_day", -1)) }

    var screen by remember { mutableStateOf(Screen.MENU) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistoryBook by remember { mutableStateOf(false) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    // Kayıt yardımcıları
    fun saveState() {
        prefs.edit()
            .putString("lang", lang.code)
            .putInt("level", currentLevel)
            .putInt("gold", gold)
            .putBoolean("music", musicEnabled)
            .putBoolean("sfx", sfxEnabled)
            .putBoolean("graphics", highGraphics)
            .putInt("daily_day", lastDailyDay)
            .apply()
    }

    // Ses motoru senkronizasyonu
    LaunchedEffect(musicEnabled, sfxEnabled) {
        soundEngine.musicEnabled = musicEnabled
        soundEngine.sfxEnabled = sfxEnabled
        soundEngine.updateBackgroundMusic()
        saveState()
    }

    // Samsung Store Kuralı: Ekran kapanınca veya uygulama alta atılınca müziği durdur
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042))
                )
            )
    ) {
        // Arka Plan: Puzzle parçaları ve harfler (Grafik ayarına duyarlı)
        PuzzleWordBackground(highGraphics = highGraphics)

        // Güvenli ekran alanı (Çentik, kamera deliği ve alt barlardan korur)
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
                        highGraphics = highGraphics,
                        soundEngine = soundEngine,
                        onUpdateGold = { newGold ->
                            gold = newGold
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

            // Üst Bildirim (Toast/Banner)
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

        // Ayarlar Penceresi
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
                    saveState()
                    showSettings = false
                },
                onDismiss = {
                    soundEngine.playClick()
                    showSettings = false
                }
            )
        }

        // Tarih Ansiklopedisi & Lig Penceresi
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
// 5. PUZZLE & KELİME HARMANLI ARKA PLAN
// ==========================================
@Composable
fun PuzzleWordBackground(highGraphics: Boolean) {
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

        // Silik çapraz bulmaca ızgarası ve puzzle kutuları
        for (r in 0..(h / tileSize).toInt()) {
            for (c in 0..7) {
                if ((r + c) % 3 == 0) {
                    val shift = if (highGraphics) sin((r + c + floatOffset * 6f).toDouble()).toFloat() * 6f else 0f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.035f),
                        topLeft = Offset(c * tileSize + 6f, r * tileSize + shift),
                        size = Size(tileSize - 12f, tileSize - 12f),
                        cornerRadius = CornerRadius(14f, 14f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Dekoratif bağlantı çizgileri (kelime ağları)
        val nodes = listOf(
            Offset(w * 0.15f, h * (0.2f + floatOffset * 0.05f)),
            Offset(w * 0.45f, h * (0.12f - floatOffset * 0.03f)),
            Offset(w * 0.82f, h * (0.25f + floatOffset * 0.04f)),
            Offset(w * 0.22f, h * (0.78f - floatOffset * 0.04f)),
            Offset(w * 0.75f, h * (0.82f + floatOffset * 0.03f))
        )
        for (i in 0 until nodes.size - 1) {
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.06f),
                start = nodes[i],
                end = nodes[i + 1],
                strokeWidth = 4f
            )
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.08f),
                radius = 22f,
                center = nodes[i]
            )
        }
    }
}

// ==========================================
// 6. ANA MENÜ EKRANI
// ==========================================
@Composable
fun MainMenuScreen(
    lang: Lang,
    level: Int,
    gold: Int,
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
        // ÜST BAR: Sol üstte ufak Bayraklı Dil Butonu + Sağ üstte Altın ve Lig
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol Üst Dil Değiştirme Butonu (Bayrak Logolu)
            Surface(
                onClick = onToggleLang,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
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

            // Sağ Üst Altın Göstergesi
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
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

        // ORTA BAŞLIK: Puzzle Harf Kutuları ve Lig Kartı
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
                                    else listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
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
                text = if (lang == Lang.TR) "BULMACA & TARİH MACERASI" else "PUZZLE & HISTORY ADVENTURE",
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Lig Rozeti Kartı (20 seviyede bir değişir)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, league.color.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = league.icon, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (lang == Lang.TR) league.nameTr else league.nameEn,
                            color = league.color,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        val nextLeagueLvl = ((level - 1) / 20 + 1) * 20
                        Text(
                            text = if (lang == Lang.TR) "Seviye $level / 300 • Sonraki Lig: Sv.$nextLeagueLvl"
                            else "Level $level / 300 • Next League: Lv.$nextLeagueLvl",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ORTA-ALT ÖZENLİ MENÜ BUTONLARI
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BAŞLA BUTONU
            MenuButton(
                text = if (lang == Lang.TR) "▶  BAŞLA (SEVİYE $level)" else "▶  PLAY (LEVEL $level)",
                gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                height = 58.dp,
                fontSize = 18,
                onClick = onStartGame
            )

            // 2. GÜNLÜK ÖDÜL & TARİH ANSİKLOPEDİSİ (Yan Yana 2 Ekstra Özellik)
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

            // 3. AYARLAR BUTONU
            MenuButton(
                text = if (lang == Lang.TR) "⚙️  AYARLAR" else "⚙️  SETTINGS",
                gradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                height = 50.dp,
                fontSize = 16,
                onClick = onOpenSettings
            )

            // 4. ÇIKIŞ YAP BUTONU
            MenuButton(
                text = if (lang == Lang.TR) "🚪  ÇIKIŞ YAP" else "🚪  EXIT GAME",
                gradient = listOf(Color(0xFFEF4444), Color(0xFFB91C1C)),
                height = 48.dp,
                fontSize = 15,
                onClick = onExit
            )
        }

        // Alt Bilgi
        Text(
            text = "Galaxy Store Edition • v1.0",
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
// 7. OYUN İÇİ EKRANI (BULMACA + ÇARK + SÜRE + TARİH)
// ==========================================
@Composable
fun GameplayScreen(
    lang: Lang,
    level: Int,
    gold: Int,
    highGraphics: Boolean,
    soundEngine: ProceduralSoundEngine,
    onUpdateGold: (Int) -> Unit,
    onLevelCompleted: (Int) -> Unit,
    onBackToMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var puzzle by remember(level, lang) { mutableStateOf(generateLevelPuzzle(level, lang)) }
    var wheelLetters by remember(puzzle) { mutableStateOf(puzzle.wheelLetters) }

    // Çözülen kelimeler ve ipucuyla açılan hücreler (satır, sütun çifti)
    var solvedWords by remember(puzzle) { mutableStateOf(setOf<String>()) }
    var revealedCells by remember(puzzle) { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var foundBonusWords by remember(puzzle) { mutableStateOf(setOf<String>()) }
    var comboCount by remember(puzzle) { mutableIntStateOf(0) }

    // Çark üzerinde seçili harf indeksleri ve parmak pozisyonu
    var selectedIndices by remember { mutableStateOf(listOf<Int>()) }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }

    // 1. Seviye İlk 5 Saniye Rehber Gösterimi
    var showTutorialOverlay by remember(level) { mutableStateOf(level == 1) }
    LaunchedEffect(level) {
        if (level == 1) {
            delay(5000)
            showTutorialOverlay = false
        }
    }

    // 15 Seviyede Bir Zamana Karşı Yarış Sayacı
    var remainingSeconds by remember(puzzle) { mutableIntStateOf(puzzle.timeLimitSec) }
    var isTimeOut by remember(puzzle) { mutableStateOf(false) }

    // Bölüm Sonu, Tarihi Bilgi ve Lig Atlama Pencereleri
    var showWinDialog by remember(puzzle) { mutableStateOf(false) }
    var unlockedFact by remember(puzzle) { mutableStateOf<HistoricalFact?>(null) }
    var promotedLeague by remember(puzzle) { mutableStateOf<LeagueInfo?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(puzzle, solvedWords.size, isTimeOut) {
        if (puzzle.isTimeAttack && !showWinDialog && !isTimeOut) {
            while (remainingSeconds > 0 && solvedWords.size < puzzle.placedWords.size) {
                delay(1000)
                remainingSeconds--
            }
            if (remainingSeconds <= 0 && solvedWords.size < puzzle.placedWords.size) {
                isTimeOut = true
                soundEngine.playError()
            }
        }
    }

    // Kelime kontrol fonksiyonu
    fun submitSwipedWord() {
        if (selectedIndices.isEmpty()) return
        val candidate = selectedIndices.map { wheelLetters[it] }.joinToString("")
        selectedIndices = emptyList()
        currentTouchPos = null

        val matchingPlaced = puzzle.placedWords.filter { it.word == candidate }
        when {
            matchingPlaced.isNotEmpty() && candidate !in solvedWords -> {
                soundEngine.playWordSuccess()
                val newSolved = solvedWords + candidate
                solvedWords = newSolved
                comboCount++

                // Tüm kelimeler bulunduysa Seviye Bitti!
                if (newSolved.size >= puzzle.placedWords.size) {
                    soundEngine.playLevelWin()
                    // Her 10 seviyede bir tarihi bilgi kontrolü
                    if (level % 10 == 0) {
                        unlockedFact = HISTORICAL_FACTS.find { it.levelMilestone == level }
                            ?: HISTORICAL_FACTS[(level / 10 - 1) % HISTORICAL_FACTS.size].copy(levelMilestone = level)
                    }
                    // Her 20 seviyede bir lig terfisi kontrolü
                    if (level % 20 == 0) {
                        promotedLeague = getLeagueForLevel(level + 1)
                    }
                    showWinDialog = true
                }
            }
            candidate in solvedWords -> {
                soundEngine.playError()
                feedbackBanner = if (lang == Lang.TR) "Bu kelimeyi zaten buldun!" else "Word already found!"
            }
            candidate in puzzle.bonusWords && candidate !in foundBonusWords -> {
                soundEngine.playBonusWord()
                foundBonusWords = foundBonusWords + candidate
                onUpdateGold(gold + 5)
                feedbackBanner = if (lang == Lang.TR) "✨ Bonus Kelime! +5 Altın" else "✨ Bonus Word! +5 Gold"
            }
            candidate.length >= 2 -> {
                soundEngine.playError()
                comboCount = 0
            }
        }
    }

    val league = getLeagueForLevel(level)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. ÜST BAR (Geri, Seviye/Lig, Zamana Karşı Sayaç, Altın, Ayarlar)
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lang == Lang.TR) "SEVİYE $level" else "LEVEL $level",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                }
                if (puzzle.isTimeAttack) {
                    Text(
                        text = if (lang == Lang.TR) "⏱️ ZAMANA KARŞI: ${remainingSeconds}sn"
                        else "⏱️ TIME ATTACK: ${remainingSeconds}s",
                        color = if (remainingSeconds <= 10) Color(0xFFEF4444) else Color(0xFFFBBF24),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                } else if (comboCount >= 2) {
                    Text(
                        text = "🔥 COMBO x$comboCount",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
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

        // 1. SEVİYE 5 SANİYELİK HIZLI REHBER BANDI
        AnimatedVisibility(visible = showTutorialOverlay) {
            Surface(
                color = Color(0xFF0284C7),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (lang == Lang.TR)
                        "👋 Hoş geldin! Aşağıdaki çarkta parmağını harflerin üzerinde kaydırarak kelimeleri oluştur!"
                    else
                        "👋 Welcome! Swipe your finger across the letters on the wheel below to form words!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // 2. BULMACA TABLOSU (Her telefon ekranına otomatik sığan dinamik yapı)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxW = maxWidth
            val maxH = maxHeight
            val cellW = (maxW / (puzzle.gridCols + 1).coerceAtLeast(4)).coerceAtMost(48.dp)
            val cellH = (maxH / (puzzle.gridRows + 1).coerceAtLeast(4)).coerceAtMost(48.dp)
            val cellSize = minOf(cellW, cellH).coerceAtLeast(24.dp)

            // Hangi hücrede hangi harf var ve açıldı mı hesapla
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (r in 0 until puzzle.gridRows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until puzzle.gridCols) {
                            val cellData = cellMap[r to c]
                            if (cellData != null) {
                                val (ch, isOpen) = cellData
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .shadow(if (isOpen) 4.dp else 2.dp, RoundedCornerShape(8.dp))
                                        .background(
                                            if (isOpen) Brush.verticalGradient(
                                                listOf(Color(0xFF10B981), Color(0xFF059669))
                                            )
                                            else Brush.verticalGradient(
                                                listOf(Color(0xFF334155), Color(0xFF1E293B))
                                            ),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isOpen) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.25f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isOpen) {
                                        Text(
                                            text = ch.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = (cellSize.value * 0.48f).sp
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

        // 3. SEÇİLEN HARFLERİN ANLIK ÖNİZLEME KUTUSU & BİLDİRİM
        val currentPreview = selectedIndices.map { wheelLetters[it] }.joinToString("")
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (currentPreview.isNotEmpty()) {
                Surface(
                    color = Color(0xFFF59E0B),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = currentPreview,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
                    )
                }
            } else if (feedbackBanner != null) {
                LaunchedEffect(feedbackBanner) {
                    delay(1500)
                    feedbackBanner = null
                }
                Text(
                    text = feedbackBanner ?: "",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // 4. ALT KISIM: SOLDA KARIŞTIR, ORTADA HARF ÇARKI (DESEN ÇİZME), SAĞDA İPUCU
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Harfleri Karıştır Butonu
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    onClick = {
                        soundEngine.playClick()
                        wheelLetters = wheelLetters.shuffled()
                    },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🔀", fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == Lang.TR) "Karıştır" else "Shuffle",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            // ORTA HARF ÇARKI (Words of Wonders Tarzı Desen Çizme Alanı)
            WordSwipeWheel(
                letters = wheelLetters,
                selectedIndices = selectedIndices,
                currentTouchPos = currentTouchPos,
                showTutorialPulse = showTutorialOverlay,
                onSelectIndex = { idx ->
                    if (idx !in selectedIndices) {
                        selectedIndices = selectedIndices + idx
                        soundEngine.playSwipeNote(selectedIndices.size - 1)
                    }
                },
                onUpdateTouch = { pos -> currentTouchPos = pos },
                onDragEnd = { submitSwipedWord() }
            )

            // İpucu (Harf Açma) Butonu (25 Altın)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    onClick = {
                        if (gold >= 25) {
                            // Henüz açılmamış rastgele bir hücre bul ve aç
                            val unrevealed = mutableListOf<Pair<Int, Int>>()
                            for (pw in puzzle.placedWords) {
                                if (pw.word !in solvedWords) {
                                    for (k in pw.word.indices) {
                                        val r = if (pw.isHorizontal) pw.row else pw.row + k
                                        val c = if (pw.isHorizontal) pw.col + k else pw.col
                                        val pos = r to c
                                        if (pos !in revealedCells) unrevealed.add(pos)
                                    }
                                }
                            }
                            if (unrevealed.isNotEmpty()) {
                                soundEngine.playClick()
                                onUpdateGold(gold - 25)
                                val pick = unrevealed.random()
                                revealedCells = revealedCells + pick
                            }
                        } else {
                            soundEngine.playError()
                            feedbackBanner = if (lang == Lang.TR) "Yetersiz Altın! (25 🪙 gerekir)" else "Need 25 🪙 for Hint!"
                        }
                    },
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("💡", fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "25 🪙",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }

    // ZAMANA KARŞI SÜRE BİTTİ PENCERESİ
    if (isTimeOut) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444))
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

    // SEVİYE TAMAMLANDI + TARİHİ BİLGİ (10 SEVİYEDE BİR) + LİG ATLAMA (20 SEVİYEDE BİR)
    if (showWinDialog) {
        val baseReward = 20
        val timeBonus = if (puzzle.isTimeAttack) remainingSeconds * 2 else 0
        val milestoneBonus = if (unlockedFact != null) 50 else 0
        val leagueBonus = if (promotedLeague != null) 100 else 0
        val totalEarned = baseReward + timeBonus + milestoneBonus + leagueBonus

        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B))
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (lang == Lang.TR) "🎉 SEVİYE $level TAMAMLANDI!" else "🎉 LEVEL $level COMPLETE!",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (lang == Lang.TR) "Kazanılan Ödül: +$totalEarned 🪙" else "Reward Earned: +$totalEarned 🪙",
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )

                    // HER 20 SEVİYEDE BİR LİG TERFİ KUTLAMASI
                    promotedLeague?.let { newLg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = newLg.color.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, newLg.color)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = newLg.icon, fontSize = 32.sp)
                                Text(
                                    text = if (lang == Lang.TR) "YENİ LİGE YÜKSELDİN!\n${newLg.nameTr}"
                                    else "LEAGUE PROMOTION!\n${newLg.nameEn}",
                                    color = newLg.color,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // HER 10 SEVİYEDE BİR TARİHİ BİLGİ KARTI (3-4 Satır)
                    unlockedFact?.let { fact ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (lang == Lang.TR) "🏛️ TARİH KÖŞESİ: ${fact.titleTr}"
                                    else "🏛️ HISTORICAL FACT: ${fact.titleEn}",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (lang == Lang.TR) fact.infoTr else fact.infoEn,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    MenuButton(
                        text = if (lang == Lang.TR) "SONRAKİ SEVİYE ➔" else "NEXT LEVEL ➔",
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

// ==========================================
// 8. HARF ÇARKI (SWIPE WHEEL) BİLEŞENİ
// ==========================================
@Composable
fun WordSwipeWheel(
    letters: List<Char>,
    selectedIndices: List<Int>,
    currentTouchPos: Offset?,
    showTutorialPulse: Boolean,
    onSelectIndex: (Int) -> Unit,
    onUpdateTouch: (Offset?) -> Unit,
    onDragEnd: () -> Unit
) {
    val wheelDiameter = 210.dp
    val density = LocalDensity.current
    val wheelPx = with(density) { wheelDiameter.toPx() }
    val center = Offset(wheelPx / 2f, wheelPx / 2f)
    val orbitRadius = wheelPx * 0.34f
    val nodeRadius = wheelPx * 0.115f

    // Harflerin merkez koordinatlarını hesapla
    val letterCenters = remember(letters, wheelPx) {
        letters.indices.map { i ->
            val angle = -PI / 2 + (2 * PI * i / letters.size)
            Offset(
                x = center.x + (orbitRadius * cos(angle)).toFloat(),
                y = center.y + (orbitRadius * sin(angle)).toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tut")
    val tutProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "tutLine"
    )

    Box(
        modifier = Modifier
            .size(wheelDiameter)
            .pointerInput(letters) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        onUpdateTouch(startOffset)
                        letterCenters.forEachIndexed { idx, c ->
                            if ((startOffset - c).getDistance() <= nodeRadius * 1.3f) {
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
            // Dış Çark Halkası
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = wheelPx / 2f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = wheelPx / 2f - 2f,
                center = center,
                style = Stroke(width = 3f)
            )

            // 1. Seviye İlk 5 Saniye Rehber Çizgisi Animasyonu
            if (showTutorialPulse && letterCenters.size >= 2 && selectedIndices.isEmpty()) {
                val p1 = letterCenters[0]
                val p2 = letterCenters[1]
                val animEnd = Offset(
                    p1.x + (p2.x - p1.x) * tutProgress,
                    p1.y + (p2.y - p1.y) * tutProgress
                )
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                    start = p1,
                    end = animEnd,
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
            }

            // Seçilen Harfler Arası Desen Çizgisi
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
                    color = Color(0xFFF59E0B),
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
            }

            // Harf Daireleri
            letterCenters.forEachIndexed { idx, c ->
                val isSelected = idx in selectedIndices
                drawCircle(
                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                    radius = nodeRadius,
                    center = c
                )
                drawCircle(
                    color = if (isSelected) Color.White else Color(0xFF38BDF8).copy(alpha = 0.7f),
                    radius = nodeRadius,
                    center = c,
                    style = Stroke(width = 4f)
                )
            }
        }

        // Harf Metinlerini Dairelerin Tam Ortasına Yerleştir
        letters.forEachIndexed { idx, ch ->
            val c = letterCenters[idx]
            val offsetX = with(density) { (c.x - wheelPx / 2f).toDp() }
            val offsetY = with(density) { (c.y - wheelPx / 2f).toDp() }
            Text(
                text = ch.toString(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            )
        }
    }
}

// ==========================================
// 9. AYARLAR & TARİH ANSİKLOPEDİSİ PENCERELERİ
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
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f))
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
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
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
                    text = if (lang == Lang.TR) "Her 10 seviyede bir yeni tarihi eser açılır!"
                    else "A new historical landmark unlocks every 10 levels!",
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
                            border = androidx.compose.foundation.BorderStroke(
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

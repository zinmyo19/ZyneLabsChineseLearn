package com.zynelabs.chineselearn

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import org.json.JSONObject
import kotlin.random.Random

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var panels: List<View>

    // Learn
    private lateinit var learnAdapter: EntryAdapter
    private var learnList: List<Entry> = ChineseData.entries

    // Flashcards
    private var flashIdx = 0
    private var flashFlipped = false

    // Quiz
    private var quizQuestions: List<Entry> = emptyList()
    private var quizIdx = 0
    private var quizScore = 0
    private var quizActive = false

    // Translate
    private var zhToMy = true
    private var lastChineseSpoken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Chinese Learn 中文"

        tts = TextToSpeech(this, this)

        panels = listOf(
            findViewById(R.id.panelLearn),
            findViewById(R.id.panelFlash),
            findViewById(R.id.panelQuiz),
            findViewById(R.id.panelDict),
            findViewById(R.id.panelTrans)
        )
        val tabBtns = listOf(
            findViewById<Button>(R.id.tabBtnLearn),
            findViewById<Button>(R.id.tabBtnFlash),
            findViewById<Button>(R.id.tabBtnQuiz),
            findViewById<Button>(R.id.tabBtnDict),
            findViewById<Button>(R.id.tabBtnTrans)
        )
        tabBtns.forEachIndexed { i, b -> b.setOnClickListener { showTab(i) } }
        showTab(0)

        setupLearn()
        setupFlash()
        setupQuiz()
        setupDict()
        setupTrans()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                toast("中文语音包没装 / တရုတ်အသံမရှိသေး — Google TTS settings မှာ Chinese (中文) voice data ထည့်ပါ")
            } else {
                ttsReady = true
            }
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            toast("语音没准备好 / အသံအဆင်သင့်မဖြစ်သေး")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt1")
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun showTab(i: Int) {
        panels.forEachIndexed { idx, v -> v.visibility = if (idx == i) View.VISIBLE else View.GONE }
    }

    // ---------- Learn ----------
    private fun setupLearn() {
        val spinner = findViewById<Spinner>(R.id.categorySpinner)
        val cats = listOf("全部 All") + ChineseData.categories
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cats).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        learnAdapter = EntryAdapter(learnList)
        val list = findViewById<ListView>(R.id.learnList)
        list.adapter = learnAdapter
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            speak(learnAdapter.getItem(pos).hanzi)
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                learnList = if (pos == 0) ChineseData.entries
                else ChineseData.entries.filter { it.category == ChineseData.categories[pos - 1] }
                learnAdapter.update(learnList)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ---------- Flashcards ----------
    private fun setupFlash() {
        val hanzi = findViewById<TextView>(R.id.flashHanzi)
        val pinyin = findViewById<TextView>(R.id.flashPinyin)
        val burmese = findViewById<TextView>(R.id.flashBurmese)
        val count = findViewById<TextView>(R.id.flashCount)
        val card = findViewById<LinearLayout>(R.id.flashCard)

        fun render() {
            val e = ChineseData.entries[flashIdx]
            hanzi.text = e.hanzi
            pinyin.text = e.pinyin
            burmese.text = e.burmese + "  ·  " + e.category
            pinyin.visibility = if (flashFlipped) View.VISIBLE else View.GONE
            burmese.visibility = if (flashFlipped) View.VISIBLE else View.GONE
            count.text = "${flashIdx + 1} / ${ChineseData.entries.size}"
        }
        card.setOnClickListener {
            flashFlipped = !flashFlipped
            render()
        }
        findViewById<Button>(R.id.flashPrev).setOnClickListener {
            flashIdx = (flashIdx - 1 + ChineseData.entries.size) % ChineseData.entries.size
            flashFlipped = false
            render()
        }
        findViewById<Button>(R.id.flashNext).setOnClickListener {
            flashIdx = (flashIdx + 1) % ChineseData.entries.size
            flashFlipped = false
            render()
        }
        findViewById<Button>(R.id.flashShuffle).setOnClickListener {
            flashIdx = Random.nextInt(ChineseData.entries.size)
            flashFlipped = false
            render()
        }
        findViewById<Button>(R.id.flashSpeak).setOnClickListener {
            speak(ChineseData.entries[flashIdx].hanzi)
        }
        render()
    }

    // ---------- Quiz ----------
    private fun setupQuiz() {
        val start = findViewById<Button>(R.id.quizStart)
        listOf(
            findViewById<Button>(R.id.quizOpt0),
            findViewById<Button>(R.id.quizOpt1),
            findViewById<Button>(R.id.quizOpt2),
            findViewById<Button>(R.id.quizOpt3)
        ).forEach { it.visibility = View.GONE }
        start.setOnClickListener { startQuiz() }
    }

    private fun startQuiz() {
        quizQuestions = ChineseData.entries.shuffled().take(10)
        quizIdx = 0
        quizScore = 0
        quizActive = true
        findViewById<Button>(R.id.quizStart).visibility = View.GONE
        renderQuestion()
    }

    private fun renderQuestion() {
        val q = quizQuestions[quizIdx]
        findViewById<TextView>(R.id.quizProgress).text = "第 ${quizIdx + 1} / ${quizQuestions.size} 题"
        findViewById<TextView>(R.id.quizHanzi).text = q.hanzi
        findViewById<TextView>(R.id.quizPinyin).text = q.pinyin
        findViewById<TextView>(R.id.quizScore).text = ""
        speak(q.hanzi)
        val distract = ChineseData.entries.filter { it != q }.shuffled().take(3)
        val options = (distract + q).shuffled()
        val opts = listOf(
            findViewById<Button>(R.id.quizOpt0),
            findViewById<Button>(R.id.quizOpt1),
            findViewById<Button>(R.id.quizOpt2),
            findViewById<Button>(R.id.quizOpt3)
        )
        opts.forEachIndexed { i, b ->
            b.visibility = View.VISIBLE
            b.text = options[i].burmese
            b.setOnClickListener {
                if (options[i] == q) {
                    quizScore++
                    toast("对了! ✅")
                } else {
                    toast("错了 ❌ 答案: ${q.burmese}")
                }
                quizIdx++
                if (quizIdx < quizQuestions.size) renderQuestion() else finishQuiz()
            }
        }
    }

    private fun finishQuiz() {
        quizActive = false
        listOf(
            findViewById<Button>(R.id.quizOpt0),
            findViewById<Button>(R.id.quizOpt1),
            findViewById<Button>(R.id.quizOpt2),
            findViewById<Button>(R.id.quizOpt3)
        ).forEach { it.visibility = View.GONE }
        findViewById<TextView>(R.id.quizHanzi).text = ""
        findViewById<TextView>(R.id.quizPinyin).text = ""
        findViewById<TextView>(R.id.quizProgress).text = ""
        findViewById<TextView>(R.id.quizScore).text = "得分: $quizScore / ${quizQuestions.size} 🎉"
        findViewById<Button>(R.id.quizStart).apply {
            visibility = View.VISIBLE
            text = "再来一次 / နောက်တစ်ခေါက် ▶"
        }
    }

    // ---------- Dictionary ----------
    private fun setupDict() {
        val adapter = EntryAdapter(ChineseData.entries)
        val list = findViewById<ListView>(R.id.dictList)
        list.adapter = adapter
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            speak(adapter.getItem(pos).hanzi)
        }
        findViewById<EditText>(R.id.dictSearch).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim().lowercase()
                val filtered = if (q.isEmpty()) ChineseData.entries
                else ChineseData.entries.filter {
                    it.hanzi.contains(q) || it.pinyin.lowercase().contains(q) || it.burmese.contains(s.toString().trim())
                }
                adapter.update(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    // ---------- Translate ----------
    private fun setupTrans() {
        val dirBtn = findViewById<Button>(R.id.transDir)
        val input = findViewById<EditText>(R.id.transInput)
        val result = findViewById<TextView>(R.id.transResult)

        dirBtn.setOnClickListener {
            zhToMy = !zhToMy
            dirBtn.text = if (zhToMy) "中文 → မြန်မာ" else "မြန်မာ → 中文"
            result.text = ""
        }
        findViewById<Button>(R.id.transGo).setOnClickListener {
            val q = input.text.toString().trim()
            if (q.isEmpty()) {
                toast("先输入文字 / စာအရင်ရိုက်")
                return@setOnClickListener
            }
            result.text = "翻译中… / ပြန်နေတယ်…"
            val pair = if (zhToMy) "zh-CN|my" else "my|zh-CN"
            Thread {
                try {
                    val url = URL("https://api.mymemory.translated.net/get?q=" +
                            URLEncoder.encode(q, "UTF-8") + "&langpair=" + pair)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    val body = conn.inputStream.bufferedReader().readText()
                    var t = JSONObject(body).getJSONObject("responseData").getString("translatedText")
                    if (t.contains("MYMEMORY WARNING") || t.contains("QUERY LENGTH LIMIT") || t.contains("INVALID")) {
                        t = "翻译失败，免费额度用完了 / ဘာသာပြန်မရပါ (free limit ကုန်သွားပြီ)"
                    }
                    val out = t
                    runOnUiThread {
                        result.text = out
                        if (zhToMy) lastChineseSpoken = q else lastChineseSpoken = out
                    }
                } catch (e: Exception) {
                    runOnUiThread { result.text = "翻译失败 / ဘာသာပြန်မရပါ: ${e.message}" }
                }
            }.start()
        }
        findViewById<Button>(R.id.transSpeak).setOnClickListener {
            if (lastChineseSpoken.isNotEmpty()) speak(lastChineseSpoken)
            else toast("先翻译 / အရင်ပြန်")
        }
    }

    // ---------- Adapter ----------
    inner class EntryAdapter(private var items: List<Entry>) : BaseAdapter() {
        fun update(newItems: List<Entry>) {
            items = newItems
            notifyDataSetChanged()
        }
        override fun getCount() = items.size
        override fun getItem(pos: Int) = items[pos]
        override fun getItemId(pos: Int) = pos.toLong()
        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.row_entry, parent, false)
            val e = getItem(pos)
            v.findViewById<TextView>(R.id.rowHanzi).text = e.hanzi
            v.findViewById<TextView>(R.id.rowPinyin).text = e.pinyin
            v.findViewById<TextView>(R.id.rowBurmese).text = e.burmese
            return v
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

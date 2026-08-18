package com.example.taxigoal

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ChatActivity : BaseActivity() {

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val scope = MainScope()
    private val gson = Gson()
    private val repository by lazy { GeminiRepository(this) }

    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnClear: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rvChat = findViewById(R.id.rvChat)
        etInput = findViewById(R.id.etChatInput)
        btnSend = findViewById(R.id.btnSendChat)
        pbLoading = findViewById(R.id.pbChatLoading)
        btnClear = findViewById(R.id.btnClearChat)

        loadChatHistory()
        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter
        if (messages.isNotEmpty()) rvChat.scrollToPosition(messages.size - 1)

        findViewById<View>(R.id.btnBackFromChat).setOnClickListener { finish() }

        val generativeModel = GeminiManager.getModel(this)
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val acc = prefs.getFloat("total_accumulated", 0f).toInt()
        val target = prefs.getFloat("goal_target", 298000f).toInt()
        val history = prefs.getString("shift_history", "{}") ?: "{}"

        val chatHistory = messages.map {
            content(role = if (it.isUser) "user" else "model") { text(it.text) }
        }

        val chat = generativeModel.startChat(
            history = if (chatHistory.isEmpty()) {
                listOf(
                    content(role = "user") { 
                        text("""
                            Ты финансовый ассистент таксиста по имени TaxiBot. 
                            Моя цель: $target ₸.
                            Накоплено: $acc ₸.
                            Моя история данных: $history
                            Помогай мне планировать доходы, расходы и отвечай на вопросы по работе в такси.
                        """.trimIndent()) 
                    },
                    content(role = "model") { text("Привет! Я твой штурман TaxiBot. Вижу твою цель в $target ₸. Чем могу помочь сегодня?") }
                )
            } else chatHistory
        )

        if (messages.isEmpty()) addAndSaveMessage(ChatMessage("Привет! Я твой штурман TaxiBot. Чем могу помочь?", false))

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendMessage(userText, chat)
                etInput.text.clear()
            }
        }
        btnClear.setOnClickListener { showClearChatDialog() }
    }

    private fun sendMessage(text: String, chat: com.google.ai.client.generativeai.Chat) {
        val apiKey = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE).getString("gemini_api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API ключ не настроен", Toast.LENGTH_LONG).show()
            return
        }

        addAndSaveMessage(ChatMessage(text, true))
        rvChat.scrollToPosition(messages.size - 1)
        pbLoading.visibility = View.VISIBLE

        scope.launch {
            val result = repository.sendMessageWithRetry(chat, text)
            runOnUiThread {
                pbLoading.visibility = View.GONE
                result.onSuccess { aiResponse ->
                    addAndSaveMessage(ChatMessage(aiResponse, false))
                    rvChat.scrollToPosition(messages.size - 1)
                }.onFailure { e ->
                    val errorMsg = e.message ?: ""
                    if (errorMsg.contains("503") || errorMsg.contains("demand", true)) {
                        addAndSaveMessage(ChatMessage("🤖 Извините, сервера Google сейчас перегружены. Попробуйте отправить сообщение еще раз через минуту.", false))
                    } else {
                        Toast.makeText(this@ChatActivity, "Ошибка AI: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun addAndSaveMessage(msg: ChatMessage) {
        adapter.addMessage(msg)
        saveChatHistory()
    }

    private fun saveChatHistory() {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val json = gson.toJson(messages)
        prefs.edit().putString("chat_history_json", json).apply()
    }

    private fun loadChatHistory() {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val json = prefs.getString("chat_history_json", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            val saved: List<ChatMessage> = gson.fromJson(json, type)
            messages.clear()
            messages.addAll(saved)
        }
    }

    private fun showClearChatDialog() {
        AlertDialog.Builder(this)
            .setTitle("Очистить чат?")
            .setPositiveButton("ОК") { _, _ ->
                messages.clear()
                getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE).edit().remove("chat_history_json").apply()
                recreate()
            }.show()
    }
}

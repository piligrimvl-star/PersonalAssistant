package com.example.personalassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.personalassistant.databinding.ActivityMainBinding
import com.example.personalassistant.database.DatabaseHelper
import com.example.personalassistant.manager.AIServiceManager
import com.example.personalassistant.manager.CloudSyncManager
import com.example.personalassistant.manager.ErrorHandler
import com.example.personalassistant.model.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var aiServiceManager: AIServiceManager
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var errorHandler: ErrorHandler
    
    private var isListening = false
    private var isTTSInitialized = false
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeVoiceRecognition()
            checkFirstLaunch()
        } else {
            Toast.makeText(this, "Разрешения необходимы для работы приложения", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeManagers()
        setupUI()
        checkPermissions()
    }
    
    private fun initializeManagers() {
        databaseHelper = (application as PersonalAssistantApp).databaseHelper
        cloudSyncManager = (application as PersonalAssistantApp).cloudSyncManager
        aiServiceManager = AIServiceManager(this)
        errorHandler = ErrorHandler(this)
        
        textToSpeech = TextToSpeech(this, this)
    }
    
    private fun setupUI() {
        binding.voiceButton.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }
        
        binding.syncButton.setOnClickListener {
            syncWithCloud()
        }
        
        binding.settingsButton.setOnClickListener {
            showSettings()
        }
    }
    
    private fun checkPermissions() {
        val requiredPermissions = mutableListOf<String>()
        
        requiredPermissions.add(Manifest.permission.RECORD_AUDIO)
        requiredPermissions.add(Manifest.permission.INTERNET)
        requiredPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            initializeVoiceRecognition()
            checkFirstLaunch()
        }
    }
    
    private fun checkFirstLaunch() {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("first_launch", true)
        
        if (isFirstLaunch) {
            sharedPreferences.edit().putBoolean("first_launch", false).apply()
            startActivity(Intent(this, SetupActivity::class.java))
        } else {
            loadEvents()
        }
    }
    
    private fun initializeVoiceRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle) {
                isListening = true
                updateListeningState()
                speak("Слушаю вас")
            }
            
            override fun onBeginningOfSpeech() {}
            
            override fun onRmsChanged(rmsdB: Float) {
                // Можно добавить визуализацию громкости
            }
            
            override fun onBufferReceived(buffer: ByteArray) {}
            
            override fun onEndOfSpeech() {
                isListening = false
                updateListeningState()
            }
            
            override fun onError(error: Int) {
                isListening = false
                updateListeningState()
                errorHandler.handleError(ErrorHandler.VoiceRecognitionError)
            }
            
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processVoiceCommand(matches[0])
                }
            }
            
            override fun onPartialResults(partialResults: Bundle) {}
            
            override fun onEvent(eventType: Int, params: Bundle) {}
        })
    }
    
    private fun processVoiceCommand(command: String) {
        binding.statusText.text = "Обрабатываю: $command"
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = aiServiceManager.processCommand(command)
                
                withContext(Dispatchers.Main) {
                    handleAIResponse(response)
                    speak(response.response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorHandler.handleError(ErrorHandler.AIServiceError, command)
                    speak("Произошла ошибка при обработке команды")
                }
            }
        }
    }
    
    private fun handleAIResponse(response: AIServiceManager.AIResponse) {
        when (response.action) {
            "add_event" -> addEvent(response)
            "show_events" -> showEvents(response)
            "update_event" -> updateEvent(response)
            "delete_event" -> deleteEvent(response)
            "sync_events" -> syncWithCloud()
            else -> showMessage(response.response)
        }
    }
    
    private fun addEvent(response: AIServiceManager.AIResponse) {
        val event = Event(
            id = 0,
            description = response.data?.get("description") ?: "Новое событие",
            date = response.data?.get("date") ?: getCurrentDate(),
            time = response.data?.get("time"),
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            reminderTime = response.data?.get("reminder_time")
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            val id = databaseHelper.addEvent(event)
            if (id != -1L) {
                event.id = id.toInt()
                withContext(Dispatchers.Main) {
                    loadEvents()
                    (application as PersonalAssistantApp).notificationManager.scheduleNotification(event)
                }
            }
        }
    }
    
    private fun showEvents(response: AIServiceManager.AIResponse) {
        loadEvents()
    }
    
    private fun updateEvent(response: AIServiceManager.AIResponse) {
        // Implementation for updating events
        loadEvents()
    }
    
    private fun deleteEvent(response: AIServiceManager.AIResponse) {
        // Implementation for deleting events
        loadEvents()
    }
    
    private fun loadEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            val events = databaseHelper.getAllEvents()
            withContext(Dispatchers.Main) {
                updateEventsList(events)
            }
        }
    }
    
    private fun updateEventsList(events: List<Event>) {
        binding.eventsList.text = events.joinToString("\n\n") { event ->
            "${event.description}\nДата: ${event.date} ${event.time ?: ""}\n${if (event.isCompleted) "✅ Выполнено" else "⏳ Ожидает"}"
        }
    }
    
    private fun syncWithCloud() {
        binding.statusText.text = "Синхронизация с облаком..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = databaseHelper.getAllEvents()
                val success = cloudSyncManager.syncEvents(events)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        speak("Синхронизация завершена успешно")
                        binding.statusText.text = "Синхронизировано с облаком"
                    } else {
                        errorHandler.handleError(ErrorHandler.CloudSyncError)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorHandler.handleError(ErrorHandler.CloudSyncError)
                }
            }
        }
    }
    
    private fun startListening() {
        try {
            speechRecognizer.startListening(recognizerIntent)
        } catch (e: Exception) {
            errorHandler.handleError(ErrorHandler.VoiceRecognitionError)
        }
    }
    
    private fun stopListening() {
        try {
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun updateListeningState() {
        binding.voiceButton.setImageResource(
            if (isListening) R.drawable.ic_mic_on else R.drawable.ic_mic_off
        )
        binding.statusText.text = if (isListening) "Слушаю..." else "Готов к работе"
    }
    
    private fun speak(text: String) {
        if (isTTSInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSettings() {
        // Implementation for settings activity
    }
    
    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("ru", "RU"))
            isTTSInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
            result != TextToSpeech.LANG_NOT_SUPPORTED
            if (isTTSInitialized) {
                speak("Голосовой помощник активирован")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
        speechRecognizer.destroy()
    }
}
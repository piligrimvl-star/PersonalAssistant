package com.example.personalassistant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.personalassistant.databinding.ActivitySetupBinding
import com.example.personalassistant.manager.CloudSyncManager

class SetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySetupBinding
    private lateinit var cloudSyncManager: CloudSyncManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cloudSyncManager = (application as PersonalAssistantApp).cloudSyncManager
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.setupTitle.text = "Настройка облачной синхронизации"
        binding.setupDescription.text = "Для синхронизации ваших событий с облаком введите данные от аккаунта Mail.ru"
        
        binding.emailInput.hint = "email@mail.ru"
        binding.passwordInput.hint = "Пароль"
        
        binding.skipButton.setOnClickListener {
            skipSetup()
        }
        
        binding.completeButton.setOnClickListener {
            completeSetup()
        }
    }
    
    private fun completeSetup() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        
        if (email.isEmpty() || !email.contains("@")) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Сохраняем учетные данные
        val sharedPreferences = getSharedPreferences("cloud_prefs", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("mail_ru_email", email)
            putString("mail_ru_password", password)
            putBoolean("cloud_sync_enabled", true)
            apply()
        }
        
        // Пробуем подключиться к облаку
        Toast.makeText(this, "Проверяем подключение...", Toast.LENGTH_SHORT).show()
        
        cloudSyncManager.testConnection { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Настройка завершена успешно!", Toast.LENGTH_LONG).show()
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Ошибка подключения. Проверьте данные", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun skipSetup() {
        val sharedPreferences = getSharedPreferences("cloud_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("cloud_sync_enabled", false).apply()
        
        Toast.makeText(this, "Облачная синхронизация отключена", Toast.LENGTH_SHORT).show()
        startMainActivity()
    }
    
    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
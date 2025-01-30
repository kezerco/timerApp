package com.example.timerapp

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerTimer: Spinner
    private lateinit var buttonStartStop: Button
    private lateinit var switchRepeat: Switch
    private lateinit var textViewTimer: TextView
    private lateinit var buttonSoundSelection: Button
    private lateinit var textViewTimerStatus: TextView

    private var timer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timeLeftInMillis: Long = 0
    private var selectedSoundUri: Uri? = null

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        spinnerTimer = findViewById(R.id.spinnerTimer)
        buttonStartStop = findViewById(R.id.buttonStartStop)
        switchRepeat = findViewById(R.id.switchRepeat)
        textViewTimer = findViewById(R.id.textViewTimer)
        buttonSoundSelection = findViewById(R.id.buttonSoundSelection)
        textViewTimerStatus = findViewById(R.id.textViewTimerStatus)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("TimerAppPrefs", Context.MODE_PRIVATE)

        // Load saved sound URI
        val savedUriString = sharedPreferences.getString("selectedSoundUri", null)
        selectedSoundUri = if (savedUriString != null) {
            Uri.parse(savedUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Set up Spinner with timer options
        val timerOptions = arrayOf("5 minutes", "10 minutes", "15 minutes", "20 minutes", "30 minutes", "60 minutes", "90 minutes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimer.adapter = adapter

        // Start/Stop button click listener
        buttonStartStop.setOnClickListener {
            if (isTimerRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }

        // Change sound selection
        buttonSoundSelection.setOnClickListener {
            selectNotificationSound()
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun startTimer() {
        val selectedTime = spinnerTimer.selectedItem.toString().split(" ")[0].toLong()
        timeLeftInMillis = selectedTime * 60 * 1000

        // Reset status text
        textViewTimerStatus.text = ""

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                isTimerRunning = false
                buttonStartStop.text = "Start"
                notifyUser()
                if (switchRepeat.isChecked) {
                    startTimer()
                }
            }
        }.start()

        isTimerRunning = true
        buttonStartStop.text = "Stop"
    }

    private fun stopTimer() {
        timer?.cancel()
        isTimerRunning = false
        buttonStartStop.text = "Start"
        timeLeftInMillis = 0
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        textViewTimer.text = timeFormatted
    }

    private fun notifyUser() {
        // Check if selectedSoundUri is null
        if (selectedSoundUri == null) {
            selectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // Play the selected sound
        val ringtone = RingtoneManager.getRingtone(applicationContext, selectedSoundUri)
        ringtone.play()

        // Show a visual indication
        textViewTimerStatus.text = "Timer is up!"

        // Show a toast message
        Toast.makeText(this, "Timer finished!", Toast.LENGTH_SHORT).show()
    }

    private fun selectNotificationSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSoundUri)
        startActivityForResult(intent, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> { // Notification Permission
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification Permission Denied. Enable it from settings.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedSoundUri = uri

                // Save the selected sound URI to SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("selectedSoundUri", uri.toString())
                    apply()
                }

                Toast.makeText(this, "Sound selected!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No sound selected. Using default.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
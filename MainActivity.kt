package com.voicecallassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.voicecallassistant.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true

        if (audioGranted && contactsGranted) {
            setupSpeechRecognizer()
            viewModel.loadContacts()
        } else {
            viewModel.setPermissionDenied()
            if (!audioGranted) {
                Toast.makeText(this, "Microphone permission is required for voice commands", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
        observeState()
    }

    private fun setupUI() {
        binding.btnMic.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }

        binding.btnGrantPermission.setOnClickListener {
            requestPermissions()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchContacts(s?.toString() ?: "")
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString() ?: ""
                if (query.isNotBlank()) {
                    viewModel.searchContacts(query)
                }
                true
            } else false
        }

        setupQuickCommandChips()
    }

    private fun setupQuickCommandChips() {
        val commands = listOf("Call Mom", "Call Dad", "Call Home", "Call Office")
        commands.forEach { command ->
            val chip = Chip(this).apply {
                text = command
                isClickable = true
                setOnClickListener {
                    simulateVoiceCommand(command)
                }
            }
            binding.chipGroupQuick.addView(chip)
        }
    }

    private fun simulateVoiceCommand(command: String) {
        binding.tvStatus.text = "\"$command\""
        viewModel.processVoiceCommand(command)
    }

    private fun checkPermissions() {
        val permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            setupSpeechRecognizer()
            viewModel.loadContacts()
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            )
        )
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    updateMicButton(true)
                    binding.tvStatus.text = "Listening…"
                }

                override fun onBeginningOfSpeech() {
                    binding.tvStatus.text = "Hearing you…"
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val scale = (1f + (rmsdB / 10f).coerceIn(0f, 1f))
                    binding.btnMic.animate().scaleX(scale).scaleY(scale).setDuration(50).start()
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    updateMicButton(false)
                    binding.btnMic.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }

                override fun onError(error: Int) {
                    isListening = false
                    updateMicButton(false)
                    binding.btnMic.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that, try again"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error — using offline mode"
                        SpeechRecognizer.ERROR_NOT_RECOGNIZED -> "Speech not recognised"
                        else -> "Recognition error ($error)"
                    }
                    binding.tvStatus.text = msg
                    viewModel.setIdle()
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topResult = matches?.firstOrNull()
                    if (!topResult.isNullOrBlank()) {
                        binding.tvStatus.text = "\"$topResult\""
                        viewModel.processVoiceCommand(topResult)
                    } else {
                        binding.tvStatus.text = "Couldn't understand, try again"
                        viewModel.setIdle()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        binding.tvStatus.text = "\"$partial\""
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions()
            return
        }
        viewModel.setListening()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        updateMicButton(false)
        viewModel.setIdle()
    }

    private fun updateMicButton(listening: Boolean) {
        if (listening) {
            binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
            binding.btnMic.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.listening_color)
            binding.listeningRipple.visibility = View.VISIBLE
        } else {
            binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
            binding.btnMic.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary)
            binding.listeningRipple.visibility = View.INVISIBLE
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: UiState) {
        binding.cardResult.visibility = View.GONE
        binding.btnGrantPermission.visibility = View.GONE
        binding.tvHint.visibility = View.VISIBLE

        when (state) {
            is UiState.Idle -> {
                binding.tvStatus.text = "Tap the mic and say\n\"Call [Name]\""
            }

            is UiState.Listening -> {
                binding.tvStatus.text = "Listening…"
            }

            is UiState.Processing -> {
                binding.tvStatus.text = "Processing…"
            }

            is UiState.ContactFound -> {
                showContactFoundCard(state.contact, state.spokenText)
            }

            is UiState.ContactNotFound -> {
                binding.cardResult.visibility = View.VISIBLE
                binding.tvResultTitle.text = "Contact not found"
                binding.tvResultSubtitle.text = "No contact named \"${state.name}\""
                binding.tvResultSubtitle.setTextColor(ContextCompat.getColor(this, R.color.error))
                binding.btnResultAction.text = "Try Again"
                binding.btnResultAction.setOnClickListener { viewModel.setIdle() }
            }

            is UiState.Calling -> {
                CallHelper.placeCall(this, state.contact.phoneNumber)
                Handler(Looper.getMainLooper()).postDelayed({
                    viewModel.setIdle()
                }, 2000)
            }

            is UiState.SearchResults -> {
                binding.tvStatus.text = "${state.results.size} contacts found for \"${state.query}\""
            }

            is UiState.PermissionDenied -> {
                binding.tvStatus.text = "Permissions required"
                binding.tvHint.text = "Microphone, Contacts & Phone Call\npermissions are needed"
                binding.btnGrantPermission.visibility = View.VISIBLE
            }

            is UiState.Error -> {
                binding.tvStatus.text = state.message
            }
        }
    }

    private fun showContactFoundCard(contact: Contact, spokenText: String) {
        binding.cardResult.visibility = View.VISIBLE
        binding.tvResultTitle.text = contact.name
        binding.tvResultSubtitle.text = contact.phoneNumber
        binding.tvResultSubtitle.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
        binding.btnResultAction.text = "📞  Call ${contact.name}"
        binding.btnResultAction.setOnClickListener {
            viewModel.confirmCall(contact)
        }

        binding.tvStatus.text = "Found contact"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

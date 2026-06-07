package com.voicecallassistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    data object Idle : UiState()
    data object Listening : UiState()
    data class Processing(val spokenText: String) : UiState()
    data class ContactFound(val contact: Contact, val spokenText: String) : UiState()
    data class ContactNotFound(val name: String) : UiState()
    data class Calling(val contact: Contact) : UiState()
    data class SearchResults(val query: String, val results: List<Contact>) : UiState()
    data object PermissionDenied : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val contactsHelper = ContactsHelper(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults: StateFlow<List<Contact>> = _searchResults.asStateFlow()

    fun loadContacts() {
        viewModelScope.launch {
            try {
                _contacts.value = contactsHelper.getAllContacts()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load contacts: ${e.message}")
            }
        }
    }

    fun setListening() {
        _uiState.value = UiState.Listening
    }

    fun setIdle() {
        _uiState.value = UiState.Idle
    }

    fun setPermissionDenied() {
        _uiState.value = UiState.PermissionDenied
    }

    fun processVoiceCommand(spokenText: String) {
        _uiState.value = UiState.Processing(spokenText)
        viewModelScope.launch {
            val parseResult = VoiceCommandParser.parse(spokenText)
            when (parseResult.action) {
                VoiceCommandParser.Action.CALL_CONTACT -> {
                    val name = parseResult.contactName ?: ""
                    val contact = contactsHelper.findContact(name)
                    if (contact != null) {
                        _uiState.value = UiState.ContactFound(contact, spokenText)
                    } else {
                        _uiState.value = UiState.ContactNotFound(name)
                    }
                }
                VoiceCommandParser.Action.SEARCH_CONTACT -> {
                    val results = contactsHelper.searchContacts(parseResult.contactName ?: "")
                    _uiState.value = UiState.SearchResults(
                        parseResult.contactName ?: "",
                        results
                    )
                }
                VoiceCommandParser.Action.UNKNOWN -> {
                    _uiState.value = UiState.Error(
                        "I heard \"$spokenText\"\nTry saying \"Call [Name]\""
                    )
                }
            }
        }
    }

    fun confirmCall(contact: Contact) {
        _uiState.value = UiState.Calling(contact)
    }

    fun searchContacts(query: String) {
        viewModelScope.launch {
            _searchResults.value = contactsHelper.searchContacts(query)
        }
    }
}

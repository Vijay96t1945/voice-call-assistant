package com.voicecallassistant

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

class ContactsHelper(private val context: Context) {

    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val resolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            val seenIds = mutableSetOf<String>()

            while (it.moveToNext()) {
                val id = it.getString(idIndex) ?: continue
                if (seenIds.contains(id)) continue
                seenIds.add(id)

                val name = it.getString(nameIndex) ?: continue
                val phone = it.getString(phoneIndex) ?: continue
                val photo = if (photoIndex >= 0) it.getString(photoIndex) else null

                contacts.add(Contact(id, name, phone.trim(), photo))
            }
        }

        contacts
    }

    suspend fun findContact(query: String): Contact? = withContext(Dispatchers.IO) {
        val allContacts = getAllContacts()
        val normalizedQuery = query.trim().lowercase()

        if (normalizedQuery.isBlank()) return@withContext null

        val exact = allContacts.firstOrNull {
            it.name.lowercase() == normalizedQuery
        }
        if (exact != null) return@withContext exact

        val startsWith = allContacts.filter {
            it.name.lowercase().startsWith(normalizedQuery)
        }
        if (startsWith.size == 1) return@withContext startsWith.first()
        if (startsWith.isNotEmpty()) return@withContext startsWith.first()

        val contains = allContacts.filter {
            it.name.lowercase().contains(normalizedQuery)
        }
        if (contains.isNotEmpty()) return@withContext contains.first()

        val words = normalizedQuery.split(" ")
        allContacts.firstOrNull { contact ->
            words.any { word ->
                contact.name.lowercase().contains(word) && word.length > 2
            }
        }
    }

    suspend fun searchContacts(query: String): List<Contact> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getAllContacts()
        val normalized = query.lowercase()
        getAllContacts().filter {
            it.name.lowercase().contains(normalized)
        }
    }
}

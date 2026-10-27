package com.example.tagmind.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag

/**
 * Extracts a hex UID string from an NFC Tag object.
 * Used to uniquely identify physical NFC tags.
 */
object NfcIntentParser {

    /**
     * Extract tag UID as uppercase hex string (e.g. "04A3BC1D").
     * Returns null if tag has no ID.
     */
    fun extractUid(tag: Tag?): String? {
        val id = tag?.id ?: return null
        return id.joinToString("") { "%02X".format(it) }
    }
}

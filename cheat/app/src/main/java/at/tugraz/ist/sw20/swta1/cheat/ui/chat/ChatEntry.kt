package at.tugraz.ist.sw20.swta1.cheat.ui.chat

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class ChatEntry(private var message: String, var isByMe: Boolean, var isBySystem: Boolean,
                private val timestamp: Date, private val id : UUID = UUID.randomUUID())
    : Serializable, Cloneable {

    private var deleted = false
    private var editTimestamp: Date? = null

    fun getFormattedTimestamp(): String {
        val df = SimpleDateFormat("HH:mm", Locale.US)
        return df.format(timestamp)
    }

    fun getMessage(): String {
        return message
    }

    fun getId(): UUID {
        return id
    }

    fun isWrittenByMe(): Boolean {
        return isByMe
    }

    fun isSystemMessage(): Boolean {
        return isBySystem
    }

    fun setDeleted() {
        deleted = true
        message = "Deleted"
    }

    fun edit(message: String) {
        this.message = message
        editTimestamp = Date()
    }

    fun isDeleted() = deleted

    fun isEdited() = editTimestamp != null

    fun getEditTimestamp() = editTimestamp

    public override fun clone(): Any {
        return super.clone()
    }
}
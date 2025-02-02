package com.vitorpamplona.amethyst.model

import androidx.lifecycle.LiveData
import com.vitorpamplona.amethyst.service.NostrSingleEventDataSource
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.ui.note.toShortenHex
import java.util.concurrent.ConcurrentHashMap

class Channel(val id: ByteArray) {
    val idHex = id.toHexKey()
    val idDisplayHex = id.toShortenHex()

    var creator: User? = null
    var info = ChannelCreateEvent.ChannelData(null, null, null)

    var updatedMetadataAt: Long = 0;

    val notes = ConcurrentHashMap<HexKey, Note>()

    @Synchronized
    fun addNote(note: Note) {
        notes[note.idHex] = note
    }

    fun updateChannelInfo(creator: User, channelInfo: ChannelCreateEvent.ChannelData, updatedAt: Long) {
        this.creator = creator
        this.info = channelInfo
        this.updatedMetadataAt = updatedAt

        live.refresh()
    }

    fun profilePicture(): String {
        if (info.picture.isNullOrBlank()) info.picture = null
        return info.picture ?: "https://robohash.org/${idHex}.png"
    }

    fun anyNameStartsWith(prefix: String): Boolean {
        return listOfNotNull(info.name, info.about)
            .filter { it.startsWith(prefix, true) }.isNotEmpty()
    }

    // Observers line up here.
    val live: ChannelLiveData = ChannelLiveData(this)

    private fun refreshObservers() {
        live.refresh()
    }
}


class ChannelLiveData(val channel: Channel): LiveData<ChannelState>(ChannelState(channel)) {
    fun refresh() {
        postValue(ChannelState(channel))
    }

    override fun onActive() {
        super.onActive()
        NostrSingleEventDataSource.add(channel.idHex)
    }

    override fun onInactive() {
        super.onInactive()
        NostrSingleEventDataSource.remove(channel.idHex)
    }
}

class ChannelState(val channel: Channel)

package com.rasheed113.worksocial.infrastructure.chat

import com.rasheed113.worksocial.domain.chat.ChatContent
import com.rasheed113.worksocial.domain.chat.MediaDescriptor

object ChatMessageMediaParser {
    private const val PREFIX = "__work_social_media__:"

    fun parse(content: String): ChatContent {
        if (!content.startsWith(PREFIX)) return ChatContent.Text(content)
        val body = content.removePrefix(PREFIX)
        val separator = body.indexOf(':')
        if (separator <= 0 || separator == body.lastIndex) return ChatContent.Text(content)
        val type = body.substring(0, separator).lowercase()
        val payload = body.substring(separator + 1).trim()
        if (payload.isEmpty()) return ChatContent.Text(content)
        return when (type) {
            "image" -> ChatContent.Media(MediaDescriptor.Image(payload))
            "video" -> ChatContent.Media(MediaDescriptor.Video(payload))
            "file" -> ChatContent.Media(MediaDescriptor.File(payload))
            else -> ChatContent.Text(content)
        }
    }
}

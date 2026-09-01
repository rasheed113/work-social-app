package com.rasheed113.worksocial.domain.chat

sealed interface MediaDescriptor {
    val payload: String
    data class Image(override val payload: String) : MediaDescriptor
    data class Video(override val payload: String) : MediaDescriptor
    data class File(override val payload: String) : MediaDescriptor
}

sealed interface ChatContent {
    data class Text(val value: String) : ChatContent
    data class Media(val descriptor: MediaDescriptor) : ChatContent
}

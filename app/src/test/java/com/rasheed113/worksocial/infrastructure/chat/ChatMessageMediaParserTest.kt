package com.rasheed113.worksocial.infrastructure.chat

import com.rasheed113.worksocial.domain.chat.ChatContent
import com.rasheed113.worksocial.domain.chat.MediaDescriptor
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageMediaParserTest {
    @Test fun parsesImage() = assertEquals(ChatContent.Media(MediaDescriptor.Image("https://example.com/a.jpg")), ChatMessageMediaParser.parse("__work_social_media__:image:https://example.com/a.jpg"))
    @Test fun parsesVideo() = assertEquals(ChatContent.Media(MediaDescriptor.Video("https://example.com/a.mp4")), ChatMessageMediaParser.parse("__work_social_media__:video:https://example.com/a.mp4"))
    @Test fun parsesFile() = assertEquals(ChatContent.Media(MediaDescriptor.File("https://example.com/a.pdf")), ChatMessageMediaParser.parse("__work_social_media__:file:https://example.com/a.pdf"))
    @Test fun regularTextRemainsText() = assertEquals(ChatContent.Text("hello"), ChatMessageMediaParser.parse("hello"))
    @Test fun malformedPrefixRemainsProtocolText() = assertEquals(ChatContent.Text("__work_social_media__:image"), ChatMessageMediaParser.parse("__work_social_media__:image"))
    @Test fun emptyPayloadRemainsProtocolText() = assertEquals(ChatContent.Text("__work_social_media__:image:"), ChatMessageMediaParser.parse("__work_social_media__:image:"))
    @Test fun unknownTypeRemainsProtocolText() = assertEquals(ChatContent.Text("__work_social_media__:audio:x"), ChatMessageMediaParser.parse("__work_social_media__:audio:x"))
    @Test fun prefixMustStartAtBeginning() = assertEquals(ChatContent.Text("x __work_social_media__:image:u"), ChatMessageMediaParser.parse("x __work_social_media__:image:u"))
    @Test fun payloadMayContainColons() = assertEquals(ChatContent.Media(MediaDescriptor.File("https://example.com/a?x=1:y")), ChatMessageMediaParser.parse("__work_social_media__:file:https://example.com/a?x=1:y"))
}

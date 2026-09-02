package com.rasheed113.worksocial.presentation.ai

import com.rasheed113.worksocial.domain.ai.AiChatResult
import com.rasheed113.worksocial.domain.ai.AiConfirmationResult
import com.rasheed113.worksocial.domain.ai.AiConversationHistory
import com.rasheed113.worksocial.domain.ai.AiCreatedEntry
import com.rasheed113.worksocial.domain.ai.AiMessage
import com.rasheed113.worksocial.domain.ai.AiPendingAction
import com.rasheed113.worksocial.domain.ai.AiRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiViewModelTest {
    @Test
    fun restoresPersistedConversation() = runTest {
        val repository = FakeAiRepository(history = AiConversationHistory("conversation-1", listOf(AiMessage("m1", "assistant", "Welcome back", "2026-09-02T00:00:00Z"))))
        val viewModel = AiViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.state.value as AiUiState.Ready
        assertEquals("conversation-1", state.conversationId)
        assertEquals("Welcome back", state.messages.single().content)
    }

    @Test
    fun sendPersistsUserAndAssistantMessagesInState() = runTest {
        val repository = FakeAiRepository()
        val viewModel = AiViewModel(repository)
        advanceUntilIdle()
        viewModel.send("Yaar kal presentation hai 😂")
        advanceUntilIdle()

        val state = viewModel.state.value as AiUiState.Ready
        assertEquals(2, state.messages.size)
        assertEquals("user", state.messages[0].role)
        assertEquals("assistant", state.messages[1].role)
        assertEquals("Presentation context understood.", state.messages[1].content)
    }

    @Test
    fun confirmationCreatesRealEntryResultAndClearsAction() = runTest {
        val action = AiPendingAction("action-1", "Title: Presentation\nType: todo\nContent: Finalize presentation", "2026-09-02T01:00:00Z")
        val repository = FakeAiRepository(pendingAction = action)
        val viewModel = AiViewModel(repository)
        advanceUntilIdle()
        viewModel.send("Ek entry bana do")
        advanceUntilIdle()
        viewModel.confirm(action)
        advanceUntilIdle()

        val state = viewModel.state.value as AiUiState.Ready
        assertTrue(repository.confirmed)
        assertEquals(null, state.pendingAction)
        assertTrue(state.messages.last().content.contains("Real entry created"))
    }

    @Test
    fun cancellationClearsPendingActionWithoutExecutingWrite() = runTest {
        val action = AiPendingAction("action-2", "Title: Test", "2026-09-02T01:00:00Z")
        val repository = FakeAiRepository(pendingAction = action)
        val viewModel = AiViewModel(repository)
        advanceUntilIdle()
        viewModel.send("Create a test entry")
        advanceUntilIdle()
        viewModel.cancel(action)
        advanceUntilIdle()

        val state = viewModel.state.value as AiUiState.Ready
        assertTrue(repository.cancelled)
        assertEquals(null, state.pendingAction)
        assertTrue(!repository.confirmed)
    }

    private class FakeAiRepository(
        private val history: AiConversationHistory? = null,
        private val pendingAction: AiPendingAction? = null,
    ) : AiRepository {
        var confirmed = false
        var cancelled = false

        override suspend fun loadHistory(conversationId: String?) = history

        override suspend fun sendMessage(conversationId: String?, message: String) = AiChatResult(
            conversationId = conversationId ?: "conversation-1",
            message = if (pendingAction != null) "Please confirm the entry." else "Presentation context understood.",
            pendingActions = listOfNotNull(pendingAction),
        )

        override suspend fun confirmAction(actionId: String) = run {
            confirmed = true
            AiConfirmationResult(true, AiCreatedEntry("entry-1", "todo", "Presentation", "Finalize presentation", false))
        }

        override suspend fun cancelAction(actionId: String) { cancelled = true }
    }
}

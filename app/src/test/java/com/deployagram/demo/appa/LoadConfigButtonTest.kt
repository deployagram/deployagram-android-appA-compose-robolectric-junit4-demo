package com.deployagram.demo.appa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.deployagram.junit4.DeployagramRule
import com.deployagram.mockwebserver.RecordingDispatcher
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Tests AppA's "Load Config" button, which calls a second back-end, AliceMessager. Both back-ends are MockWebServer
 * doubles on the JVM, so no emulator or network is needed.
 *
 * The button sends "Hello" when a book has already been recommended, and an empty payload when it has not.
 */
@RunWith(RobolectricTestRunner::class)
class LoadConfigButtonTest {

    @get:Rule
    val deployagram = DeployagramRule()

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val defaultBookOverflowUrl = BookOverflowConfig.baseUrl
    private val defaultAliceMessagerUrl = AliceMessagerConfig.baseUrl
    private lateinit var bookOverflow: MockWebServer
    private lateinit var bookOverflowAnswers: SuggestionAnswers
    private lateinit var aliceMessager: MockWebServer
    private lateinit var enteredEmail: String

    @Before
    fun startBackEnds() {
        enteredEmail = "reader-${UUID.randomUUID()}@example.com"

        bookOverflowAnswers = SuggestionAnswers()
        bookOverflow = MockWebServer()
        bookOverflow.dispatcher = RecordingDispatcher(
            bookOverflowAnswers,
            "AliceA",
            "ORG_ctm_01m1j08sx96792d2tysabvyadp:BookOverflow",
        )
        bookOverflow.start()
        BookOverflowConfig.baseUrl = bookOverflow.url("/").toString().trimEnd('/')

        aliceMessager = MockWebServer()
        aliceMessager.dispatcher = RecordingDispatcher(AliceMessagerAnswers(), "AliceA", "AliceMessager")
        aliceMessager.start()
        AliceMessagerConfig.baseUrl = aliceMessager.url("/").toString().trimEnd('/')
    }

    @After
    fun stopBackEnds() {
        bookOverflow.shutdown()
        aliceMessager.shutdown()
        BookOverflowConfig.baseUrl = defaultBookOverflowUrl
        AliceMessagerConfig.baseUrl = defaultAliceMessagerUrl
    }

    @Test
    fun pressingLoadConfigAfterARecommendationSendsHelloAndShowsWorld() {
        givenBookOverflowHasASuggestion()
        givenABookHasBeenRecommended()

        whenTheLoadConfigButtonIsPressed()

        thenTheReplyIsShown("World")
        thenAliceMessagerWasSentTheConfigRequest(AliceMessagerAnswers.HELLO)
    }

    @Test
    fun pressingLoadConfigWithNoRecommendationSendsAnEmptyPayloadAndShowsTheReply() {
        whenTheLoadConfigButtonIsPressed()

        thenTheReplyIsShown("Is someone there?")
        thenAliceMessagerWasSentTheConfigRequest("")
    }

    // Given

    private fun givenBookOverflowHasASuggestion() {
        bookOverflowAnswers.suggestionJson = """
            {
              "name": "Refactoring",
              "edition": "1st",
              "format": "paperback",
              "authors": ["Fowler Martin", "Beck Kent"]
            }
        """.trimIndent()
    }

    private fun givenABookHasBeenRecommended() {
        // The recommendation is made through the screen, as a user would make it.
        composeRule.onNodeWithTag("email").performTextInput(enteredEmail)
        composeRule.onNodeWithText("Recommend book").performClick()
        thenTextIsShown("Refactoring")
    }

    // When

    private fun whenTheLoadConfigButtonIsPressed() {
        composeRule.onNodeWithText("Load Config").performClick()
    }

    // Then

    private fun thenTheReplyIsShown(reply: String) {
        thenTextIsShown(reply)
    }

    private fun thenTextIsShown(text: String) {
        composeRule.waitUntil(timeoutMillis = TimeUnit.SECONDS.toMillis(10)) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
    }

    private fun thenAliceMessagerWasSentTheConfigRequest(expectedBody: String) {
        val sent = aliceMessager.takeRequest(5, TimeUnit.SECONDS) ?: error("AliceMessager was not sent a request")
        check(sent.path == "/AliceMessager/config") { "sent to ${sent.path}" }
        val body = sent.body.clone().readUtf8()
        check(body == expectedBody) { "sent body $body" }
    }
}

// What AliceMessager answers. A POST with {"message":"Hello"} gets 200 with {"message":"World"}. Any other payload,
// including an empty one, gets 400 with {"message":"Is someone there?"}.
class AliceMessagerAnswers : Dispatcher() {

    companion object {
        const val HELLO = "{\"message\":\"Hello\"}"
        const val WORLD = "{\"message\":\"World\"}"
        const val IS_SOMEONE_THERE = "{\"message\":\"Is someone there?\"}"
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val json = "application/json"
        return if (request.body.clone().readUtf8() == HELLO) {
            MockResponse().setResponseCode(200).setHeader("Content-Type", json).setBody(WORLD)
        } else {
            MockResponse().setResponseCode(400).setHeader("Content-Type", json).setBody(IS_SOMEONE_THERE)
        }
    }
}

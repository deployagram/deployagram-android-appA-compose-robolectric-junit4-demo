package com.deployagram.demo.appa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
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
 * Tests AppA's "Recommend book" screen on the JVM, with Robolectric and Jetpack Compose. No emulator is needed.
 *
 * The test never sends an HTTP request itself. It types an email and presses the button, and AppA makes the
 * request to BookOverflow. BookOverflow is a MockWebServer double, and the test checks what the double received
 * and what the screen shows.
 */
@RunWith(RobolectricTestRunner::class)
class RecommendBookScreenTest {

    @get:Rule
    val deployagram = DeployagramRule()

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val defaultBookOverflowUrl = BookOverflowConfig.baseUrl
    private lateinit var bookOverflow: MockWebServer
    private lateinit var bookOverflowAnswers: SuggestionAnswers
    private lateinit var enteredEmail: String

    @Before
    fun startBookOverflow() {
        bookOverflow = MockWebServer()
        bookOverflowAnswers = SuggestionAnswers()
        enteredEmail = "reader-${UUID.randomUUID()}@example.com"
        // Deployagram records the request AppA sends, and the answer BookOverflow gives.
        bookOverflow.dispatcher = RecordingDispatcher(
            bookOverflowAnswers,
            "AliceA",
            "ORG_ctm_01m1j08sx96792d2tysabvyadp:BookOverflow",
        )
        bookOverflow.start()
        BookOverflowConfig.baseUrl = bookOverflow.url("/").toString().trimEnd('/')
    }

    @After
    fun stopBookOverflow() {
        bookOverflow.shutdown()
        BookOverflowConfig.baseUrl = defaultBookOverflowUrl
    }

    @Test
    fun pressingRecommendBookAsksBookOverflowForASuggestionAndShowsIt() {
        givenBookOverflowHasASuggestion()

        whenTheRecommendBookButtonIsPressed()

        thenTheSuggestionIsShown()
        thenBookOverflowWasAskedForTheEnteredEmail()
    }

    @Test
    fun pressingRecommendBookWithNoEmailAsksBookOverflowWhichRefusesAndAsksForAValidEmail() {
        givenBookOverflowRefusesARequestWithNoEmail()
        givenNoEmailIsEntered()

        whenTheRecommendBookButtonIsPressed()

        thenTheInvalidEmailPromptIsShown()
        thenBookOverflowWasCalledWithoutAnEmailAddress()
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

    private fun givenBookOverflowRefusesARequestWithNoEmail() {
        bookOverflowAnswers.refusesRequestsWithNoEmail = true
    }

    private fun givenNoEmailIsEntered() {
        enteredEmail = ""
    }

    // When

    private fun whenTheRecommendBookButtonIsPressed() {
        if (enteredEmail.isNotEmpty()) {
            composeRule.onNodeWithTag("email").performTextInput(enteredEmail)
        }
        composeRule.onNodeWithText("Recommend book").performClick()
    }

    // Then

    private fun thenTheSuggestionIsShown() {
        thenTextIsShown("Refactoring")
    }

    private fun thenTheInvalidEmailPromptIsShown() {
        thenTextIsShown("You must enter a valid email!")
    }

    private fun thenTextIsShown(text: String) {
        // AppA makes the request in the background, so the screen is checked until the result appears.
        composeRule.waitUntil(timeoutMillis = TimeUnit.SECONDS.toMillis(10)) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
    }

    private fun thenBookOverflowWasAskedForTheEnteredEmail() {
        val request = bookOverflow.takeRequest(5, TimeUnit.SECONDS)
            ?: error("BookOverflow was not asked for a suggestion for \"$enteredEmail\"")
        check(request.requestUrl?.encodedPath == "/BookOverflow/suggestion") { "asked at ${request.requestUrl}" }
        check(request.requestUrl?.queryParameter("email") == enteredEmail) { "asked for ${request.requestUrl}" }
    }

    private fun thenBookOverflowWasCalledWithoutAnEmailAddress() {
        val request = bookOverflow.takeRequest(5, TimeUnit.SECONDS)
            ?: error("BookOverflow was not asked for a suggestion")
        check(request.requestUrl?.encodedPath == "/BookOverflow/suggestion") { "asked at ${request.requestUrl}" }
        check(request.requestUrl?.queryParameter("email") == "") { "asked for ${request.requestUrl}" }
    }
}

// What BookOverflow answers. A test sets what it wants with the given steps.
// Until then, BookOverflow has no suggestion and answers 404.
class SuggestionAnswers : Dispatcher() {
    @Volatile
    var suggestionJson: String? = null

    // When true, a request without an email is refused with 400, as BookOverflow refuses it.
    @Volatile
    var refusesRequestsWithNoEmail = false

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (refusesRequestsWithNoEmail && request.requestUrl?.queryParameter("email").isNullOrBlank()) {
            return MockResponse().setResponseCode(400)
        }
        val json = suggestionJson ?: return MockResponse().setResponseCode(404)
        return MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
}

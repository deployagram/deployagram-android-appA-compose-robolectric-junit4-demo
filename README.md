# Deployagram Android AppA Compose Robolectric JUnit 4 Demo

The same small Android app as the other AppA demos, with its screen written in **Jetpack Compose**. AppA has one screen.
The user types an email and presses **Recommend book**, and the app asks BookOverflow for a book suggestion.

This version tests the Compose screen on the JVM, with **Robolectric** and the **Compose UI test** library, in **JUnit 4**.
It needs no emulator, so it runs on any machine or CI runner that has Java 21.

## What it does

AppA calls `GET /BookOverflow/suggestion?email=<email>` and shows the answer:

| Situation                                      | What the screen shows                                        |
|------------------------------------------------|--------------------------------------------------------------|
| BookOverflow suggests a book                   | The book's name, edition, format and authors                 |
| Email is blank, and BookOverflow refuses (400) | `You must enter a valid email!`                              |
| Any other failure                              | `Sorry, no suggestion right now (<reason>)`                  |

## What it is for

* drive a Compose screen from a JVM test, with `createAndroidComposeRule` and Robolectric;
* stand in for BookOverflow with a **MockWebServer** double, and check what the app sent;
* write the test in plain **JUnit 4**.

The test never sends an HTTP request. It types an email and presses the button, and AppA makes the request.

## Tech

* Kotlin 2.1.21 with the Compose compiler plugin, Android Gradle Plugin 8.9.0, Gradle 8.12 (checked-in wrapper)
* Java 17 to build the app, and Java 21 to run the tests (Robolectric's Android 16 runtime needs Java 21)
* Min SDK 26, compile and target SDK 36
* Jetpack Compose (BOM 2025.06.01), Material 3, the Compose UI test library
* Robolectric 4.18.1, JUnit 4.13.2
* OkHttp 5.3.2 in the app, MockWebServer 5.3.2 in the tests

## Run the tests

```bash
./gradlew testDebugUnitTest
```

The test report is in `app/build/reports/tests/testDebugUnitTest/index.html`.

## Project layout

```
app/src/main/java/com/deployagram/demo/appa/
    MainActivity.kt          the Compose screen: email field, button, result
    BookOverflowClient.kt    GET /BookOverflow/suggestion, with OkHttp
    BookSuggestion.kt        the suggestion, and how it is shown
    BookOverflowConfig.kt    where BookOverflow is (the test changes it)
app/src/test/java/com/deployagram/demo/appa/
    RecommendBookScreenTest.kt   the two JVM tests, and the BookOverflow double
```

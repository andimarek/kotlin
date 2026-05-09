// FULL_JDK
// WITH_STDLIB

<!ANNOTATION_HAS_NO_EFFECT!>@file:Log<!>

import lombok.extern.java.Log
import lombok.AccessLevel

@Log(access = AccessLevel.PUBLIC)
class LogExamplePublic

@Log(access = AccessLevel.PROTECTED)
open class LogExampleProtectedBase

class LogExampleProtected : LogExampleProtectedBase() {
    fun test() {
        log.info("Test LogExampleProtected") // OK
    }
}

@Log(access = AccessLevel.PRIVATE)
open class LogExamplePrivateBase {
    fun testBase() {
        log.info("Test LogExamplePrivateBase") // OK
    }
}

class LogExamplePrivate : LogExamplePrivateBase() {
    fun test() {
        <!INVISIBLE_REFERENCE!>log<!>.info("Test LogExamplePrivate") // Invisible
    }
}

<!ANNOTATION_HAS_NO_EFFECT!>@Log<!>
interface Interface

<!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited
typealias TA = String

val logOnAnonymousObject = <!ANNOTATION_HAS_NO_EFFECT!>@Log<!> object {}

fun check() {
    <!ANNOTATION_HAS_NO_EFFECT!>@Log<!>
    class LocalClass

    LogExamplePublic.log.info("Test LogExamplePublic") // OK
    LogExampleProtected.<!UNRESOLVED_REFERENCE!>log<!>.info("Test LogExampleProtected") // INVISIBLE
    LogExamplePrivate.<!UNRESOLVED_REFERENCE!>log<!>.info("Test LogExamplePrivate") // INVISIBLE
}

<!LOG_PROPERTY_ALREADY_EXISTS!>@Log<!>
class LogOnOuterClassWhenItsCompanionHasLogField {
    companion object MyCompanion {
        val log = "No log"
    }
}

class LogOnCompanionWhenCompanionHasLogField {
    <!LOG_PROPERTY_ALREADY_EXISTS!>@Log<!>
    companion object MyCompanion {
        val log = "No log"
    }
}

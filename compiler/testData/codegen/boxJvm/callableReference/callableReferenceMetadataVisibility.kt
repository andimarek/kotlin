// TARGET_BACKEND: JVM
// WITH_STDLIB

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val PROTECTED_VISIBILITY = 2

private fun syntheticClassVisibility(javaClass: Class<*>): Int {
    val extraInt = javaClass.getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

fun foo(): String = "OK"

fun box(): String {
    val callableReference = ::foo

    val visibility = syntheticClassVisibility(callableReference::class.java)
    // TODO maybe make it LOCAL instead
    if (visibility != PROTECTED_VISIBILITY) {
        return "Fail: expected PROTECTED visibility (2), got $visibility"
    }

    return callableReference()
}

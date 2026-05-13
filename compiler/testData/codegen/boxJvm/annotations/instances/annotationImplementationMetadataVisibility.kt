// TARGET_BACKEND: JVM
// WITH_STDLIB

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val INTERNAL_VISIBILITY = 0

private fun syntheticClassVisibility(javaClass: Class<*>): Int {
    val extraInt = javaClass.getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

annotation class Ann(val value: String)

fun box(): String {
    val ann = Ann("OK")

    val visibility = syntheticClassVisibility(ann.javaClass)
    // TODO maybe make it LOCAL instead
    if (visibility != INTERNAL_VISIBILITY) {
        return "Fail: expected INTERNAL visibility (0), got $visibility"
    }

    return ann.value
}

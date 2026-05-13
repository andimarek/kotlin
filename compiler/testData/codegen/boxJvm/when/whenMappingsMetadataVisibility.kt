// TARGET_BACKEND: JVM
// WITH_STDLIB

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val PUBLIC_VISIBILITY = 3

private fun syntheticClassVisibility(javaClass: Class<*>): Int {
    val extraInt = javaClass.getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

enum class E {
    A, B
}

private fun test(e: E): Int = when (e) {
    E.A -> 1
    E.B -> 2
}

fun box(): String {
    test(E.A)

    val whenMappings = Class.forName("WhenMappingsMetadataVisibilityKt").declaredClasses.single { it.simpleName == "WhenMappings" }
    val visibility = syntheticClassVisibility(whenMappings)
    // TODO maybe make it LOCAL instead
    if (visibility != PUBLIC_VISIBILITY) {
        return "Fail: expected PUBLIC visibility (3), got $visibility"
    }

    return "OK"
}

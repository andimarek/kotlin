// JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val PUBLIC_VISIBILITY = 3

private fun syntheticClassVisibility(className: String): Int {
    val extraInt = Class.forName(className).getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

private interface Test {
    fun test(): String = "OK"
}

private class TestClass : Test

fun box(): String {
    val visibility = syntheticClassVisibility("${Test::class.java.name}\$DefaultImpls")
    // TODO: maybe change visibility of DefaultImpls to the same as of the original interface
    if (visibility != PUBLIC_VISIBILITY) {
        return "Fail: expected PUBLIC visibility (3), got $visibility"
    }

    return TestClass().test()
}

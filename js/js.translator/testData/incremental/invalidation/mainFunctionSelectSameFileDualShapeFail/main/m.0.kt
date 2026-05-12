import value.ok

fun box(stepId: Int, isWasm: Boolean): String {
    val correct = when (stepId) {
        0 -> "C0"
        1 -> "C0" // Error: Expected C0 but have BAD1
        else -> "Unknown"
    }
    callMainFromSecond()
    return if (correct == value.ok) "OK" else "Expected $correct but have ${value.ok}"
}

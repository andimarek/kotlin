import value.ok

fun box(stepId: Int, isWasm: Boolean): String {
    val correct = when (stepId) {
        0 -> "C0"
        1 -> "P2" // Error: Expected P2 but have C0
        else -> "Unknown"
    }
    callMainFromSecond()
    return if (correct == value.ok) "OK" else "Expected $correct but have ${value.ok}"
}

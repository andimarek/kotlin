// JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val INTERNAL_VISIBILITY = 0
private const val PRIVATE_VISIBILITY = 1
private const val PROTECTED_VISIBILITY = 2
private const val PUBLIC_VISIBILITY = 3

private fun syntheticClassVisibility(className: String): Int {
    val extraInt = Class.forName(className).getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

internal interface IInternal {
    fun test(): String = "OK"
}
private interface IPrivate {
    fun test(): String = "OK"
}
interface IPublic {
    fun test(): String = "OK"
}
open class Outer {
    protected interface IProtected {
        fun test(): String = "OK"
    }

    companion object {
        fun getIProtectedClass(): Class<*> = IProtected::class.java
    }
}

fun box(): String {
    var visibility = syntheticClassVisibility("${IInternal::class.java.name}\$DefaultImpls")
    if (visibility != INTERNAL_VISIBILITY) {
        return "Fail: expected INTERNAL visibility (0), got $visibility"
    }

    visibility = syntheticClassVisibility("${IPrivate::class.java.name}\$DefaultImpls")
    if (visibility != PRIVATE_VISIBILITY) {
        return "Fail: expected PRIVATE visibility (1), got $visibility"
    }

    val protectedClass = Outer.getIProtectedClass()
    visibility = syntheticClassVisibility("${protectedClass.name}\$DefaultImpls")
    if (visibility != PROTECTED_VISIBILITY) {
        return "Fail: expected PROTECTED visibility (2), got $visibility"
    }

    visibility = syntheticClassVisibility("${IPublic::class.java.name}\$DefaultImpls")
    if (visibility != PUBLIC_VISIBILITY) {
        return "Fail: expected PUBLIC visibility (3), got $visibility"
    }
    return "OK"
}

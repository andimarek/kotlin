// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6
// See KT-84267 K/Wasm: init order of companion objects is different from JVM

var l = ""
private fun log(t: String) {
    l += t + "\n"
}

// Each test uses its own class hierarchy so companions are initialized fresh.

// test1: accessing companion only
open class B1 {
    init { log("B1.init#1") }
    companion object { init { log("B1.Companion") } }
    init { log("B1.init#2") }
}
class A1 : B1() {
    init { log("A1.init#1") }
    companion object { init { log("A1.Companion") } }
    init { log("A1.init#2") }
}

// test2: creating an instance
open class B2 {
    init { log("B2.init#1") }
    companion object { init { log("B2.Companion") } }
    init { log("B2.init#2") }
}
class A2 : B2() {
    init { log("A2.init#1") }
    companion object { init { log("A2.Companion") } }
    init { log("A2.init#2") }
}

// test3: companion access then instance creation
open class B3 {
    init { log("B3.init#1") }
    companion object { init { log("B3.Companion") } }
    init { log("B3.init#2") }
}
class A3 : B3() {
    init { log("A3.init#1") }
    companion object { init { log("A3.Companion") } }
    init { log("A3.init#2") }
}

// test4: instance creation then companion access
open class B4 {
    init { log("B4.init#1") }
    companion object { init { log("B4.Companion") } }
    init { log("B4.init#2") }
}
class A4 : B4() {
    init { log("A4.init#1") }
    companion object { init { log("A4.Companion") } }
    init { log("A4.init#2") }
}

fun box(): String {
    l = ""
    A1
    val r1 = l
    if (r1 != "B1.Companion\nA1.Companion\n") return "fail test1: '$r1'"

    l = ""
    A2()
    val r2 = l
    if (r2 != "B2.Companion\nA2.Companion\nB2.init#1\nB2.init#2\nA2.init#1\nA2.init#2\n") return "fail test2: '$r2'"

    l = ""
    A3
    log("--")
    A3()
    val r3 = l
    if (r3 != "B3.Companion\nA3.Companion\n--\nB3.init#1\nB3.init#2\nA3.init#1\nA3.init#2\n") return "fail test3: '$r3'"

    l = ""
    A4()
    log("--")
    A4
    val r4 = l
    if (r4 != "B4.Companion\nA4.Companion\nB4.init#1\nB4.init#2\nA4.init#1\nA4.init#2\n--\n") return "fail test4: '$r4'"

    return "OK"
}

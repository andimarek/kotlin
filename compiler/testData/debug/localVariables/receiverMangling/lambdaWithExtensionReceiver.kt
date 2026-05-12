
// FILE: test.kt
fun foo(block: Long.() -> String): String {
    return 1L.block()
}

fun box() {
    foo {
        "OK"
    }
}

// EXPECTATIONS JVM_IR
// test.kt:8 box:
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:9 box$lambda$0: $this$foo:long=1:long
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:8 box:
// test.kt:11 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:4 foo: block=Function1
// test.kt:4 foo: block=Function1
// test.kt:9 box$lambda: $this$foo=kotlin.Long
// test.kt:11 box:

// EXPECTATIONS WASM
// test.kt:8 $box: (8, 4)
// test.kt:4 $foo: $block:(ref $kotlin.test.Function1)=(ref $kotlin.test.Function1) (14, 11, 11, 11, 11, 11, 11, 14, 14, 14, 14, 14, 14, 14, 14, 14)
// test.kt:8 $box$lambda.invoke: $$this$foo:(ref $kotlin.Long)=(ref $kotlin.Long) (8, 8, 8, 8)
// test.kt:9 $box$lambda.invoke: $$this$foo:(ref $kotlin.Long)=(ref $kotlin.Long) (8, 8, 8, 12)
// test.kt:4 $foo: $block:(ref $kotlin.test.Function1)=(ref $kotlin.test.Function1) (14, 14, 14, 14, 14, 14, 14, 14, 14, 4)
// test.kt:8 $box: (4)
// test.kt:11 $box: (1)

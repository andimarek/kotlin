// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Store (
    val storeID: Int,
    val storeRegion: String,
    val revenue: Long,
    val expenses: Long
)

fun box(): String {
    val frameCol = columnOf(
        dataFrameOf(
            Store(23, "Chicago", 500, 400),
            Store(71, "Chicago", 600, 480),
        ),
        dataFrameOf(
            Store(23, "Chicago", 500, 400),
            Store(71, "Chicago", 600, 480),
        ),
        null
    )

    val df = dataFrameOf("group" to frameCol)

    checkExactType<DataFrame<Store>?>(
        df.group[0]
    )

    checkExactType<DataFrame<Store>?>(
        df.add("dummyCol") { 123 }.group[0]
    )

    return "OK"
}

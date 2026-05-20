@_exported import ExportedKotlinPackages
@_spi(kotlinx$cinterop$ExperimentalForeignApi) import FooKit
@_implementationOnly import KotlinBridges_CinteropReexport
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.main {
    @_spi(kotlinx$cinterop$ExperimentalForeignApi)
    public static func consumesFoo(
        x: FooKit.Foo
    ) -> Swift.Int32 {
        return main_consumesFoo__TypesOfArguments__FooKit_Foo__(x)
    }
    @_spi(kotlinx$cinterop$ExperimentalForeignApi)
    public static func producesFoo() -> FooKit.Foo? {
        return main_producesFoo().map { it in it as! FooKit.Foo }
    }
}

// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// FILECHECK_STAGE: CStubs
// WITH_PLATFORM_LIBS

// KT-77446: Subclassing AVPlayerItem (whose inherited -init is unavailable) must not
// emit a MissingInitImp entry for the bare `init` selector.

@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.ObjCObjectBase.OverrideInit
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVPlayerItem

class DarwinPlaybackItem : AVPlayerItem {
    @OverrideInit
    constructor(asset: AVAsset, automaticallyLoadedAssetKeys: List<*>? = null) :
            super(asset, automaticallyLoadedAssetKeys)
}

// The generated Obj-C class metadata for DarwinPlaybackItem must contain exactly three instance
// method descriptions: the real Kotlin override for initWithAsset:automaticallyLoadedAssetKeys: plus
// MissingInitImp entries for the two available, non-overridden inits inherited from AVPlayerItem
// (initWithURL: and initWithAsset:). Most importantly - a fourth MissingInitImp entry for the unavailable
// bare -init selector should NOT be emitted.

// CHECK-DAG: [[INIT_WITH_ASSET_KEYS:@[0-9]+]] = internal constant [44 x i8] c"initWithAsset:automaticallyLoadedAssetKeys:\00"
// CHECK-DAG: [[INIT_WITH_URL:@[0-9]+]] = internal constant [13 x i8] c"initWithURL:\00"
// CHECK-DAG: [[INIT_WITH_ASSET:@[0-9]+]] = internal constant [15 x i8] c"initWithAsset:\00"
// CHECK-DAG: = internal constant [3 x %struct.ObjCMethodDescription] [
// CHECK-DAG: %struct.ObjCMethodDescription { ptr @MissingInitImp, ptr [[INIT_WITH_URL]],
// CHECK-DAG: %struct.ObjCMethodDescription { ptr @MissingInitImp, ptr [[INIT_WITH_ASSET]],

// The bare "init" selector constant must not be emitted at all.
// CHECK-NOT: = internal constant [5 x i8] c"init\00"

fun box(): String = "OK"

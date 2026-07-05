# General Android
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class com.google.android.material.** { *; }
-keep class androidx.test.** { *; }
-dontwarn android.test.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}

# Hilt / Dagger
-keep class com.rkdevstudios.voxly.VoxlyApp { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Agora (Video/Audio)
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# Razorpay
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-dontwarn com.razorpay.**
-keep class com.razorpay.** {*;}
-optimizations !method/inlining/*

# Coil (Image Loading)
-keep class coil.** { *; }
-dontwarn coil.**

# Data Classes (JSON Parsing / Firestore)
-keepclassmembers class com.rkdevstudios.voxly.data.model.** { *; }

# Navigation
-keep class androidx.navigation.** { *; }

# OkHttp / Retrofit (if used, or transitive)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

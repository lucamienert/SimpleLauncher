# Standard ProGuard/R8 rules for Jetpack Compose & Kotlin
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep launcher components
-keep class com.example.simplelauncher.** { *; }

# Kotlinx Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

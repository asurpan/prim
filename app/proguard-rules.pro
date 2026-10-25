# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# App specific rules
-keep class com.sagon.prim.** { *; }
-keepclassmembers class com.sagon.prim.** {
    *;
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# WorkManager
-keep class androidx.work.** { *; }

# Coil
-dontwarn coil.**

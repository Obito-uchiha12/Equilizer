# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep audio effect models and settings entities
-keep class com.example.audio.model.** { *; }
-keep class com.example.settings.model.** { *; }
-keep class com.example.device.model.** { *; }
-keep class com.example.core.result.** { *; }

# Keep AudioEffect system reflection and classes
-keep class android.media.audiofx.** { *; }

# Preserve line numbers for release stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile


# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Hilt rules
-keep class com.aiim.android.di.** { *; }
-keep @dagger.hilt.InstallIn class *
-keep class * extends java.lang.annotation.Annotation { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase { *; }

# Gson rules
-keep class com.google.gson.** { *; }
-keep class com.aiim.android.data.remote.model.** { *; }

# Socket classes
-keep class com.aiim.android.core.im.** { *; }

# Timber
-keep class timber.log.Timber { *; }
 -assumenosideeffects class timber.log.Timber {
     public static void d(...);
     public static void i(...);
     public static void w(...);
     public static void e(...);
 }

# Optional MNN rules (for future SDK/JNI integration)
-keep class com.alibaba.mnn.** { *; }
-keep class com.taobao.meta.** { *; }
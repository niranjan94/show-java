# Proguard rules that are applied to your test apk/code.
-ignorewarnings

-keepattributes *Annotation*

-dontnote junit.framework.**
-dontnote junit.runner.**

-keep class androidx.test.**
-keep class androidx.test.** { *; }
-keep class com.njlabs.showjava.test.** { *; }
-keep class com.android.dex.** { *; }

-dontwarn androidx.test.**
-dontwarn androidx.**
-dontwarn android.support.test.**
-dontwarn org.junit.**

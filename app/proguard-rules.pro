-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-libraryjars libs

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable
-allowaccessmodification

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.billing.IInAppBillingService
-keep class com.android.vending.billing.**
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

-keep class com.njlabs.showjava.utils.AesCbcWithIntegrity$PrngFixes$* { *; }

-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }

-keep class android.support.** { *; }
-keep interface android.support.** { *; }

-keep class jadx.** { *; }
-keep class org.benf.cfr.reader.** { *; }
-keep class com.googlecode.** { *; }
-keep class com.android.** { *; }

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes InnerClasses,EnclosingMethod

-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**
-keep class org.jetbrains.java.decompiler.** { *; }
-keep class org.jetbrains.java.decompiler.**

-keep class cz.msebera.android.httpclient.** { *; }
-keep class com.loopj.android.http.** { *; }

-keepattributes SourceFile,LineNumberTable

-keep public class * extends java.lang.Exception

-dontwarn com.google.**
-dontwarn com.android.**
-dontwarn antlr.debug.**
-dontwarn org.antlr.**
-dontwarn com.njlabs.showjava.**
-dontwarn rx.internal.**
-dontwarn com.googlecode.dex2jar.**
-dontwarn android.test.**
-dontwarn p.rn.**
-dontwarn junit.runner.**
-dontwarn net.dongliu.**
-dontwarn com.**
-dontwarn android.**
-dontwarn org.**
-dontwarn net.**
-dontwarn uk.**
-dontwarn io.**
-dontwarn jadx.**
-dontwarn java.**
-dontwarn org.jetbrains.java.decompiler.**
-keepattributes *Annotation*
-keepattributes Signature
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keep class dev.tvdeck.onecontroller.core.model.** { *; }
-dontwarn java.lang.invoke.**

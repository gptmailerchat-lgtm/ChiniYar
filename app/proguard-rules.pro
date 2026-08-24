# Keep ML Kit classes that may be accessed through generated registrars or reflection.
# This protects future minified builds from release-only runtime failures.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.gms.internal.vision.** { *; }

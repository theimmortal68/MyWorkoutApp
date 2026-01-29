# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.workoutgen.data.local.entity.** { *; }

# Keep domain models for Gson
-keep class com.workoutgen.domain.model.** { *; }

# Gson specific
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

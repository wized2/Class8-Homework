-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**

-dontwarn io.noties.markwon.**
-keep class io.noties.markwon.** { *; }

# JLatexMath / Markwon LaTeX
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**
-dontwarn ru.noties.jlatexmath.**

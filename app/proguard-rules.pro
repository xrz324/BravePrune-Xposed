# Xposed discovers the entry point from META-INF/xposed/java_init.list.
-keep public class moe.brave.prune.ModuleMain { *; }
-keep class moe.brave.prune.compat.** { *; }
-keep class moe.brave.prune.feature.** { *; }
-keep class moe.brave.prune.settings.** { *; }
-keep class moe.brave.prune.prefs.** { *; }

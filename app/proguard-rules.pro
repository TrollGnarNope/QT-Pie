# Keep all classes in the com.veigar.questtracker.model package
-keep class com.veigar.questtracker.model.** { *; }
-keepattributes Signature, InnerClasses,Deprecated

# Remove all logging statements in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove System.out.println statements
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Remove System.err.println statements
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Additional optimizations for release builds
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

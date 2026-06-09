// ... (باقي الملف)
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
        // استخدم keystore التصحيح الافتراضي المضمن في SDK
        storeFile = file(androidDefaultConfig.debugKeystoreFile ?: 
            File(System.getenv("HOME") + "/.android/debug.keystore"))
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
}
// ...

package com.edirear.app;

public class Config {
    /**
     * The base URL for the Edirear platform.
     * Both the Android App and the Web browser connect to this same server,
     * ensuring identical databases, accounts, videos, likes, comments, and storage.
     *
     * Production: "https://edirear.com"
     * Local Wi-Fi Testing: "http://192.168.1.X:5000" (replace X with your PC's local IP)
     * Android Emulator Testing: "http://10.0.2.2:5000"
     */
    public static final String DEFAULT_URL = " http://192.168.10.6:5000";

    // Application identifier
    public static final String APP_NAME = "Edirear";
    
    // User Agent suffix
    public static final String USER_AGENT_SUFFIX = " EdirearApp/1.0";
}

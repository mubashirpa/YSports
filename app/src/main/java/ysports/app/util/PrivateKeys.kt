package ysports.app.util

class PrivateKeys {

    /**
     * A native method that is implemented by the 'ysports' native library,
     * which is packaged with this application.
     */
    external fun matchesUrlPath(): String
    external fun leaguesUrlPath(): String

    companion object {
        // Used to load the 'ysports' library on application startup.
        init {
            System.loadLibrary("ysports")
        }
    }
}
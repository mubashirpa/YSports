package ysports.app

class PrivateKeys {

    /**
     * A native method that is implemented by the 'ysports' native library,
     * which is packaged with this application.
     */
    external fun newsApiKey(): String
    external fun youtubeApiKey(): String

    companion object {
        // Used to load the 'ysports' library on application startup.
        init {
            System.loadLibrary("ysports")
        }
    }
}
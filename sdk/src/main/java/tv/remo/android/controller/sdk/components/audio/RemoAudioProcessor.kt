package tv.remo.android.controller.sdk.components.audio

import android.util.Log
import org.btelman.controlsdk.streaming.audio.processors.FFmpegAudioProcessor
import org.btelman.controlsdk.streaming.models.StreamInfo
import tv.remo.android.controller.sdk.RemoSettingsUtil

/**
 * Audio processor that will pass in our values to the output options
 */
class RemoAudioProcessor : FFmpegAudioProcessor() {

    override fun getVideoOutputOptions(props: StreamInfo): Collection<String> {
        var bitrate = 32
        var volumeBoost = 1
        context?.let { ctx ->
            RemoSettingsUtil.with(ctx){
                try{
                    bitrate = it.microphoneBitrate.getPref().toInt()
                }catch(_ : Exception){}
                try{
                    volumeBoost = it.micVolume.getPref().toInt()
                }catch(_ : Exception){}
            }
        }
        return arrayListOf(
            "-f mpegts",
            "-codec:a mp2",
            "-b:a ${bitrate}k",
            "-ar 44100",
            "-muxdelay 0.001",
            "-filter:a volume=$volumeBoost"
        )
    }

    override fun onProgress(message: String?) {
        super.onProgress(message)
        Log.d(TAG, "onProgress : $message")
    }
}
package tv.remo.android.controller.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseIntArray
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.btelman.controlsdk.enums.ComponentStatus
import org.btelman.controlsdk.models.Component
import tv.remo.android.controller.R
import tv.remo.android.controller.RemoApplication
import tv.remo.android.controller.sdk.components.StatusBroadcasterComponent

/**
 * Status view that will communicate directly with a chosen component
 *
 * TODO replace with ViewModel
 */
class RemoStatusView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CustomImageView(context, attrs, defStyleAttr), Runnable{

    private var log = RemoApplication.getLogger(this)
    private var colorLookup = SparseIntArray().also{
        appendColor(context, it, R.color.colorIndicatorDisabledFromSettings)
        appendColor(context, it, R.color.colorIndicatorDisabled)
        appendColor(context, it, R.color.colorIndicatorConnecting)
        appendColor(context, it, R.color.colorIndicatorStable)
        appendColor(context, it, R.color.colorIndicatorUnstable)
        appendColor(context, it, R.color.colorIndicatorError)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                onBroadcastDataReceived(it)
            }
        }
    }

    private val broadcastManager = LocalBroadcastManager.getInstance(context)

    @Suppress("DEPRECATION")
    private fun appendColor(context: Context, it: SparseIntArray, resId: Int) {
        val color : Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(resId)
        }
        else{
            context.resources.getColor(resId)
        }
        it.append(resId, color)
    }

    private var uiHandler : Handler = Handler(Looper.getMainLooper())
    private var component: String? = null
    private var status : ComponentStatus? = null
    val values = ComponentStatus.values()
    init{
        uiHandler.post(this)
        post {
            onCreated()
        }
    }

    private fun onCreated() {

    }

    fun onDestroy(){
        uiHandler.removeCallbacksAndMessages(null)
    }

    fun <T : Component> registerStatusEvents(statusClassName : Class<T>){
        broadcastManager.unregisterReceiver(receiver)
        val filter = IntentFilter(StatusBroadcasterComponent.ACTION_SERVICE_STATUS)
        StatusBroadcasterComponent.generateComponentStatusAction(statusClassName.name).also {
            filter.addAction(it)
            log.d { "switching log to $it" }
            log = RemoApplication.getLogger(this, it)
        }
        broadcastManager.registerReceiver(receiver, filter)
    }

    fun unregisterStatusEvents(){
        broadcastManager.unregisterReceiver(receiver)
    }

    private fun onBroadcastDataReceived(intent: Intent) {
        log.v{
            "onBroadcastDataReceived ${intent.action} ${intent.getSerializableExtra(StatusBroadcasterComponent.STATUS_NAME)}"
        }
        if(intent.action == StatusBroadcasterComponent.ACTION_SERVICE_STATUS) return
        val componentStatus = intent.getSerializableExtra(StatusBroadcasterComponent.STATUS_NAME)
        status = componentStatus as? ComponentStatus
    }

    fun setDrawableColor(id : Int){
        val color : Int = colorLookup.get(id, Color.BLACK)
        setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }

    fun postStatus(status: ComponentStatus){
        post {
            this.status = status
            setStatus(status)
        }
    }

    private fun setStatus(componentStatus: ComponentStatus){
        when(componentStatus){
            ComponentStatus.DISABLED_FROM_SETTINGS -> setDrawableColor(R.color.colorIndicatorDisabledFromSettings)
            ComponentStatus.DISABLED -> setDrawableColor(R.color.colorIndicatorDisabled)
            ComponentStatus.CONNECTING -> setDrawableColor(R.color.colorIndicatorConnecting)
            ComponentStatus.STABLE -> setDrawableColor(R.color.colorIndicatorStable)
            ComponentStatus.INTERMITTENT -> setDrawableColor(R.color.colorIndicatorUnstable)
            ComponentStatus.ERROR -> setDrawableColor(R.color.colorIndicatorError)
        }
    }

    override fun run() {
        status?.let {
            setStatus(it)
            uiHandler.postDelayed(this, 100)
        } ?: run{
            setStatus(loopStatus())
            uiHandler.postDelayed(this, 1000)
        }
    }

    var i = 0
    private fun loopStatus(): ComponentStatus {
        var status : ComponentStatus = ComponentStatus.DISABLED_FROM_SETTINGS
        values.forEachIndexed { index, componentStatus ->
            if (index == i) {
                status = componentStatus
            }
        }
        i++
        if(i > values.size)
            i = 0
        return status
    }
}

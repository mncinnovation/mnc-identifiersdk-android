package id.mncinnovation.identification.core.utils

import android.app.Activity
import android.app.ActivityManager

class MemoryUsageMonitor(private val activity: Activity, private val activityManager: ActivityManager, private val lowMemoryThreshold: Int?) {
    private var defaultLowMemoryThreshold: Int = 50
    private var isShowDialog = false

    fun checkMemoryAndProceed(action: () -> Unit) {
        checkMemory(true, action)
    }


    fun checkMemory(isShowBtnContinue: Boolean = false, action: (() -> Unit)? = null) {
        val runtime: Runtime = Runtime.getRuntime()
        val usedMemInMB: Long = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB: Long =runtime.maxMemory() / 1048576L
        val availHeapSizeInMB: Long = maxHeapSizeInMB - usedMemInMB

        val memoryInfo = ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }

        if(!isShowDialog){
            if ((availHeapSizeInMB < (lowMemoryThreshold ?: defaultLowMemoryThreshold)) || activityManager.isLowRamDevice || memoryInfo.lowMemory) {
                isShowDialog = true
                val memoryStats = "Terdeteksi alokasi memory rendah pada sistem yang dapat mempengaruhi proses pada gambar. Tutup aplikasi lain yang tidak terpakai untuk hasil yang optimal."
//                val memoryStats = "Heap Used: $usedMemInMB MB, Heap Available: $availHeapSizeInMB MB"
                activity.showDismissableErrorDialog(
                    memoryStats,
                    onDismiss = {
                        isShowDialog = false
                    },
                    isShowBtnContinue,
                    onContinue = {
                        isShowDialog = false
                        action?.invoke()
                    })
            } else {
                action?.invoke()
            }
        }
    }
}
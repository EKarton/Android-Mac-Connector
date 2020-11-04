package com.androidmacconnector.androidapp.mqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Is a receiver used to listen to when the device has booted up
 */
class MqttBootDeviceReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
//            val uploadFcmToServerRequest = OneTimeWorkRequestBuilder<UploadFcmTokenToServerWorker>().build()
//            val startMqttServiceRequest = OneTimeWorkRequestBuilder<StartMqttService>().build()
//
//            WorkManager.getInstance(context).enqueue(uploadFcmToServerRequest)
//            WorkManager.getInstance(context).enqueue(startMqttServiceRequest)
        }
    }
}
//
//class UploadFcmTokenToServerWorker(private val appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
//    companion object {
//        private const val LOG_TAG = "UploadFcmTokenToServer"
//    }
//
//    override fun doWork(): Result {
//        try {
//            val fcmToken = getFcmToken()
//            val deviceId = getDeviceIdSafely(appContext) ?: return Result.failure()
//            val accessToken = getAccessToken()
//
//            uploadFcmTokenToServer(accessToken, deviceId, fcmToken)
//            return Result.success()
//
//        } catch (e: Exception) {
//            return Result.failure()
//        }
//    }
//
//    private fun getFcmToken(): String {
//        val latch = CountDownLatch(1)
//        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//            latch.countDown()
//        }
//
//        // Wait for the task to be complete
//        latch.await()
//
//        val tokenResult = FirebaseMessaging.getInstance().token
//        if (!tokenResult.isSuccessful || tokenResult.result == null) {
//            throw Exception("Cannot get token!")
//        }
//
//        return tokenResult.result!!
//    }
//
//    private fun getAccessToken(): String {
//        val latch = CountDownLatch(1)
//        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
//            latch.countDown()
//        }
//
//        // Wait for the task to be complete
//        latch.await()
//
//        val tokenResult = FirebaseAuth.getInstance().currentUser?.getIdToken(false)
//        if (!tokenResult?.isSuccessful!! || tokenResult.result == null) {
//            throw Exception("Cannot get token!")
//        }
//
//        return tokenResult.result?.token ?: throw Exception("Cannot get token")
//    }
//
//    private fun uploadFcmTokenToServer(accessToken: String, deviceId: String, fcmToken: String) {
//        val deviceWebService = DeviceWebService(appContext)
//        val latch = CountDownLatch(1)
//        deviceWebService.updatePushNotificationToken(accessToken, deviceId, fcmToken, object : UpdatePushNotificationTokenHandler() {
//            override fun onSuccess() {
//                Log.d(LOG_TAG, "Successfully updated fcm token")
//                latch.countDown()
//            }
//
//            override fun onError(exception: Exception) {
//                throw exception
//            }
//        })
//
//        latch.await()
//    }
//}
//
//class StartMqttService(private val appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
//    override fun doWork(): Result {
//        val startServiceIntent = Intent(appContext, MqttService::class.java)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            appContext.startForegroundService(startServiceIntent)
//        } else {
//            appContext.startService(startServiceIntent)
//        }
//
//        return Result.success()
//    }
//}

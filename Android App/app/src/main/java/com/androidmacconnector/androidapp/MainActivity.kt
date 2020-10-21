package com.androidmacconnector.androidapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.data.*
import com.androidmacconnector.androidapp.sms.SmsQueryService
import com.androidmacconnector.androidapp.sms.SmsReceiverService
import com.androidmacconnector.androidapp.sms.SmsSenderService
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var smsBroadcastReceiver: SmsReceiverService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textbox = findViewById<EditText>(R.id.myTextBox)

        val webService = AndroidMacConnectorServiceImpl(this)
        val smsQueryService = SmsQueryService(this)
        val smsSenderService = SmsSenderService()
        val smsReceiverService = SmsReceiverService(webService)

        val requiredPermissions =
            smsReceiverService.getRequiredPermissions() + smsQueryService.getRequiredPermissions() + smsSenderService.getRequiredPermissions()

        this.smsBroadcastReceiver = smsReceiverService

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW)
            )
        }

        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d(TAG, token!!)
            textbox.setText(textbox.text.toString() + "\n\n" + token)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })

        Dexter.withContext(this)
            .withPermissions(requiredPermissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    registerReceiver(
                        smsBroadcastReceiver,
                        IntentFilter("android.provider.Telephony.SMS_RECEIVED")
                    )

//                    smsSenderService.sendSmsMessage("+1 647-608-4809", "Testing")
                    smsQueryService.getSmsThreadsSummary().forEach {
                        val msgData = arrayOf(
                            it.threadId,
                            it.phoneNumber,
                            it.contactName,
                            it.numUnreadMessages,
                            it.numMessages,
                            it.lastMessageTime,
                            it.lastMessage
                        ).joinToString()

                        textbox.setText(textbox.text.toString() + "\n\n" + msgData)
                    }

                    smsQueryService.getSmsMessagesFromThread("8").forEach {
                        val msgData = arrayOf(
                            it.messageId,
                            it.address,
                            it.person,
                            it.type,
                            it.readState,
                            it.time,
                            it.body
                        ).joinToString()

                        textbox.setText(textbox.text.toString() + "\n\n" + msgData)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest?>?, token: PermissionToken?) {
                    token?.continuePermissionRequest();
                }
            })
            .onSameThread()
            .check()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            smsBroadcastReceiver,
            IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        )

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(smsBroadcastReceiver)
    }

    fun testing() {
        val subscriber1 = SendSmsRequestFcmSubscriber {

        }

        val subscriber2 = UpdateSmsThreadsRequestFcmSubscriber {

        }

        val subscriber3 = UpdateSmsForThreadRequestFcmSubscriber {

        }

        val pubSubService = FcmSubscriptionServiceImpl()
        pubSubService.addSubscriber(subscriber1)
        pubSubService.addSubscriber(subscriber2)
        pubSubService.addSubscriber(subscriber3)
    }
}
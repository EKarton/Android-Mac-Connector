# Introduction:

Macs and IPhones have the ability to sync notifications, sms, and perform tasks seemlessly between devices through a technology called [continuity](https://support.apple.com/en-us/HT204681). However, this feature does not apply to Android phones syncing notifications to Macs. The existing solutions out there do not support devices being on a separate network, requires monthly subscriptions, and are not open source.

# Problem Statement:

We would like to build an application that would allow notifications, sms, and tasks to be shared between Mac and Android devices. The features we would like to have includes:

1. Receive sms text from my Android phone to my Mac
2. Send sms text from my Mac to my Android phone
3. See sms text from a list of contacts
4. Recieve notifications from my Android phone to my Mac
5. Reply to notifications from my Mac to my Android phone
6. Have the ability to obtain copied text (clipboard) from Android device to Mac
7. Have the ability to obtain copied text (clipboard) from Mac to Android device
8. Have the ability to open up hotspot from Android device to Mac
9. Have the ability to control the Mac's mouse from an Android device

# Proposed Solution:

There were several solutions explored:

1. Make a MacOS app that integrates with KDE Connect:
2. Make a MacOS app and Android app that uses a server:
3. Make a MacOS app and Android app that uses APN, FCM, and a server
4. Make a MacOS app, Android app, and a server, that uses FCM and long polling

We propose the solution to create a MacOS app, Android app, and a server that uses FCM and long polling (solution #4) because:

1. More flexible: the devices don't have to be in the same network in order for syncing to work
2. More robust: if the devices lose connection, they can pick up the notifications from the server
3. Is cheaper: FCM, a small server, and a small database on GCP is free

This is a high-order architecture of the proposed solution:

# Implementation:

1. Recieving sms text from my Android phone to my Mac:

* To receive sms messages from my Android phone, you can create a broadcast receiver and enable READ_SMS permissions ([link](https://stackoverflow.com/questions/848728/how-can-i-read-sms-messages-from-the-device-programmatically-in-android))
* The problem is to map the messages to the thread ID in the sms inbox, which would require doing a query of the context

2. Send sms text from my Mac to my Android phone:

* This is possible via SmsManager ([link](https://google-developer-training.github.io/android-developer-phone-sms-course/Lesson%202/2_p_sending_sms_messages.html))

3. See sms text from a list of contacts:

* This is possible via the context query if you have the thread id. We can get a list of thread IDs via that same context query again
* Link: [tutorial](https://google-developer-training.github.io/android-developer-phone-sms-course/Lesson%202/2_p_sending_sms_messages.html)

4. Recieve notifications from my Android phone to my Mac:

5. Reply to notifications from my Mac to my Android phone:

6. Have the ability to obtain copied text from my Android device to my Mac:

7. Have the ability to obtain copied text from my Mac to my Android device:

8. Have the ability to open up a hotspot from my Android device to my Mac:

9. Have the ability to control the Mac's mouse from an Android device:

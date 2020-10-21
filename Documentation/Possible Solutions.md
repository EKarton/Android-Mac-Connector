# Introduction

Currently, there is no reliable, cheap, open-source way to sync notifications between Mac and Android devices. Existing solutions require monthly subscriptions, and a lack of features. Hence, we would like to have a reliable way to sync notifications between Mac and Android devices.

# Solutions:

These are the solutions currently being considered:

### 1. Make a MacOS app that integrates with KDE Connect:

* Intro:
	* This involves creating a MacOS app that sets up a TCP connection with the KDE connect app 100% of the time
* Pros: 
	* Less work on the Android device: will use the existing KDE connect app
	* More supported features, such as mouse tracking, etc
* Cons:
	* Will only work on the local network. Having the Mac device on a different network from the Android device will not work.

### 2. Make a MacOS app and Android app that uses a server:

* Intro: 
	* This involves creating a MacOS app, Android app, and a server.
	* When the MacOS app sends a notification to the server, the server saves that notification to a database. It then sends the notification to the Android app via long polling and displays the notification to the user. 
	* On the other hand, if the Android app wants to send a notification, it will send it to the server, which the server saves the notification, before the MacOS app polls for new changes to the notification.
* Pros:
	* It is simple
	* Can work when devices are on different networks
* Cons:
	* More expensive
	* Requires polling for new notifications - is more expensive
	* When the MacOS app or Android app is running in the background, the device might not be able to get that notification.

### 3. Make a MacOS app and Android app that uses APN, FCM, and a server

* Intro:
	* This involves creating a MacOS app, an Android app, a server; and integrate it with APN and FCM
	* When a MacOS app sends a notification to the server, the server sends the notification to the Android device via FCM (Firebase cloud messaging). FCM then passes the notification to the Android app. Since FCM has a data limit of 256 bytes, if the content of the notification is larger than 256 bytes, it will save it to a database, and send a simple view of the notification to the Android app. When the user clicks on the push notification on their Android device, it will poll for that message from the database.
	* The other way happens as well. When an Android app sends a notification to the server, the server sends a notification to the MacOS app via APN (apple push notification). APN will then pass the notification to the MacOS app. Since APN has a data limit of 256 bytes, if the content of the notification is larger than 256 bytes, it will save it to a database, and send a simple view of the notification to the MacOS app. When the user clicks on the push notification on their MacOS app, it will poll for that message from the database
	* For other tasks, such as controlling the mouse of a MacOS device, or viewing the files in a device, we can limit it so that they are on the same network and uses regular TCP connection

* Pros:
	* Can work when the app on the MacOS or Android devices are idle
	* Can work when devices are on different networks

* Cons:
	* More expensive and more complicated
	* Requires polling for the message when the message is too long
	* Longer latency since it needs to hop from the server to APN / FCM server, and then to the device
	* When the app is not running, it will continue to get the notifications without our ability to control it, which is annoying
	* Requires Apple Developer Membership to get the p12 certificate which is $99 / month

### 4. Make a MacOS app, Android app, and a server, that uses FCM and long polling

* Intro:
	* This involves creating a MacOS app, an Android app, and a server; which integrates with FCM
	* When a MacOS app sends a notification to the server, the server saves the notification to a database. The server then sends the notification to the Android device via FCM (Firebase Cloud Messaging). FCM then passes the notification to the Android app. Since FCM has a data limit of 4kb, if the content of the notification is larger than 4kb, it will save it in the database, and send a simple view of the notification to the Android app. When the user clicks on the push notification on their Android device, it will poll for that message from the database.
	* The other way is different. When the Android app sends a notification to the server, the server saves the notification in a database. When the MacOS app is running, it will continuously poll the database until it gets new updates. When it receives new notifications, it will tell the server that it has received the notification, thereby removing that notification from the database.

* Pros:
	* Can with without an Apple developer membership, and FCM is free
	* Can work when the Android device are idle
	* Can work when the devices are on different networks

* Cons:
	* More expensive and more complicated, but less expensive than solution #3 (not requiring Apple developer membership)
	* For the MacOS app to recieve notifications, it needs to long poll which is expensive

# Proposed Solution:
We propose the fourth solution because it is:
1. More flexible: the devices don't have to be in the same network in order for syncing to work
2. More robust: if the devices lose connection, they can pick up the notifications from the server
3. Is cheaper: FCM, a small server, and a small database on GCP is free

If we have more money around, we can enable APN on the MacOS app


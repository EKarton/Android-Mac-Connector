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

5. Make a Mac Catalyst app, Android app, and a server, that uses MQTT and remote push notifications

* Intro:
  * This involves creating a SwiftUI app, an Android app, MQTT broker, and a REST server
  * When the MacOS app sends a notification to the server, it will publish a message to a topic to the MQTT broker.
  * The server saves the message in an in-memory cache. The server then sends an empty notification to the Android device via FCM (Firebase Cloud Messaging). FCM then passes the notification to the Android app. 
  * When the Android app is still connected to the MQTT broker, it will receive the notification. But if the Android app is not connected to the MQTT broker, the Android device will still receive the FCM notification, thereby allowing the Android device to wake up from sleep mode [(link)](https://firebase.google.com/docs/cloud-messaging/concept-options#setting-the-priority-of-a-message), and reconnect to the MQTT broker.
  * The other way is similar; the Android device will publish a message to a topic to the MQTT broker. The broker will then transmit the message to the MacOS app, who is a subscriber to the topic that the Android device has published to. The MacOS app will then receive the message and perform actions related to that message

* Pros:
  * Can with without an Apple developer membership, and FCM is free
  * Can work when the Android device are idle
  * Can work when the devices are on different networks (since it uses a server)
  * Mac Catalyst can target different Apple devices (iOS, iPad) [(link)](https://developer.apple.com/mac-catalyst/)
  * Is network efficient - uses MQTT protocol which has small message payload sizes [(link)](http://www.steves-internet-guide.com/mqtt-protocol-messages-overview/#:~:text=The%20minimum%20size%20of%20the,1%20byte%20packet%20length%20field.)
  * Can be used with web sockets [(link)](https://www.hivemq.com/blog/mqtt-essentials-special-mqtt-over-websockets/)
  * Can work in unreliable network connections [(link)](https://mqtt.org/faq/)

* Cons:
  * More expensive (requires a server)
  * Requires an MQTT broker

# Proposed Solution:
We propose the fifth solution because it is:
1. Can target different Apple devices: the Mac Catalyst app could be run on iOS, iPad, and more [(link to wiki)](https://developer.apple.com/mac-catalyst/)
2. More flexible: the devices don't have to be in the same network in order for syncing to work
3. More robust: if the devices lose connection, they can pick up the lost notifications from the MQTT server [(link)](https://internetofthingsagenda.techtarget.com/definition/MQTT-MQ-Telemetry-Transport)
4. Is cheaper: FCM (Firebase remote notifications service), Firebase Auth, and Firebase servers are initially free to try
5. Is efficient: MQTT messages use less bandwidth since they are smaller [(link)](https://internetofthingsagenda.techtarget.com/definition/MQTT-MQ-Telemetry-Transport)

If we have the Apple Developer program subscription, we can enable APN on the MacOS app so that it can receive notifications when the MacOS app is in the background


# Android Mac Connector

The Android Mac Connector project aims to bridge the connectivity between Android and Mac devices. Equipped with MQTT, you can send and read SMS, receive app notifications and take photos to your Android device from your Mac without sacrificing battery power. In addition, they can continue to remain connected even on different networks! Best of all, you can host this project on your own Firebase infrastructure for free (as at the time of writing)!

## Overview of the Project

This project consists of an Android app, a SwiftUI app, and a Node JS app. A video of the project can be seen below:

[![Video of our project](docs/video-image.png)](link to youtube video)

This is the system architecture of the project:

![](docs/diagram.png)

The technologies behind this project first relies on an [MQTT](https://mqtt.org/) connection to establish a two-way communication between a device and the server while guaranteeing message delivery and low power consumption. 

However, keeping an MQTT connection open is problematic for Android and Mac devices as the OS tend to terminate background services (thereby closing our MQTT connections). This is in the their effort to save battery power. Hence, the project depends on [Push Notification Services](https://en.wikipedia.org/wiki/Push_technology#Push_notification) to send updates from the server to the device, allowing us to re-establish the MQTT connection if needed. The push notification services this project currently uses is [Firebase Messaging](https://firebase.google.com/docs/cloud-messaging), which is used to send push notifications to the Android device. We are hoping to use [APNs](https://developer.apple.com/go/?id=push-notifications) to send updates to the Mac device.

In addition, this project consists of a RESTful Web Api to query the registered devices a user has. Registered devices are stored in [Firestore](https://firebase.google.com/docs/firestore).

Lastly, this project utilizes [Firebase Authentication Service](https://firebase.google.com/docs/auth) to authenticate MQTT / HTTP connections, and register new users. 

## Setup
Required programs and tools: 

* Android studio
* XCode
* CocoaPods
* Google account

#### Setting up Firebase
1. Log into your Google Account and go to the [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Set up Firestore in **test mode** by following the *Create a Cloud Firestore database* section of this [wiki](https://firebase.google.com/docs/firestore/quickstart#create)
4. Set up Firebase Auth by going to the console, selecting *Authentication*, and enabling **Email/Password**

#### Setting up server (```./server```)
1. Put the credentials file in the server project's root directory by:

    1. Go to your [GCP Console](https://console.cloud.google.com)
    2. Select your project
    3. Create a [custom role](https://cloud.google.com/iam/docs/creating-custom-roles) with ```cloudmessaging.messages.create``` and ```firebaseauth.users.get``` permissions
    4. Create a new [service account](https://cloud.google.com/iam/docs/creating-managing-service-accounts#creating) with the role you have made from (3) and with the ```Cloud Datastore User``` role
    5. Under the service account, create a new key (in JSON format), and save it as ```service-account-file.json``` in the project's root directory

2. Export the path of the credentials file to an environment variable

    ```
    export GOOGLE_APPLICATION_CREDENTIALS=service-account-file.json
    ```

3. [Optional] You can ignore authentication and authorization during MQTT connections by setting these environment variables:

    ```
    export VERIFY_AUTHENTICATION=false
    export VERIFY_AUTHORIZATION=false
    ```

4. Run the server by running ```npm start```. It should spawn up a new server process under port 3000

#### Setting up Android device (```./android-app```)
1. Open the folder ```android-app``` in Android Studio
2. Add the Android app to Firebase by:

    1. Go to the [Firebase Console](https://console.firebase.google.com/), click on *Add App*, select *Android*, 
    2. Set the package name to ```com.androidmacconnector.androidapp```
    3. Download the ```google-services.json``` config file to the Android app project's app directory
    4. Skip the *Add Firebase SDK* step

3. Build and run the project in Android Studio by clicking on the green arrow button. It should succeed

#### Setting up SwiftUI app (```./mac-catalyst-app```)
1. Run ```$ pod install``` in the project's root directory
2. Open ```Mac Catalyst App.xcworkspace``` in XCode
3. Add the SwiftUI app to Firebase by:

    1. Go to the [Firebase Console](https://console.firebase.google.com/), click on *Add App*, select *iOS*, 
    2. Set the iOS bundle ID to ```com.androidmacconnector.Mac-Catalyst-App```
    3. Download the ```GoogleService-Info.plist``` config file to the Mac Catalyst project
    4. Skip the *Add Firebase SDK* and *Add Initialization Code* steps

4. In XCode, set the device to run the app on to *Mac*
5. Build and run the project in XCode by clicking on the arrow button. It should succeed

## Usage:
Please note that this project is used for educational purposes and is not intended to be used commercially. We are not liable for any damages/changes done by this project.

## Credits:

Emilio Kartono, who made the entire project

## License:

This project is protected under the GNU licence. Please refer to the Licence.txt for more information.


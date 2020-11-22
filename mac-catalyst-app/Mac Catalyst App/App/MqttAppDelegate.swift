//
//  MqttAppDelegate.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-13.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import os
import SwiftUI
import FirebaseAuth
import BackgroundTasks

class MqttAppDelegate: NSObject, UIApplicationDelegate {
    private let mqttClient: MQTTClient
    private let mqttSubscriber: MQTTSubscriptionClient
    private let mqttPublisher: MQTTPublisherClient
    
    private let sessionStore: SessionStore
    private let deviceWebService: DeviceWebService
    private let deviceRegistrationService: DeviceRegistrationService
    
    private let incomingPingHandler: IncomingPingHandler
        
    init(
        _ mqttClient: MQTTClient,
        _ mqttSubscriber: MQTTSubscriptionClient,
        _ mqttPublisher: MQTTPublisherClient,
        _ sessionStore: SessionStore,
        _ deviceWebService: DeviceWebService,
        _ deviceRegistrationService: DeviceRegistrationService,
        _ pingRequestHandler: IncomingPingHandler
    ) {
        self.mqttClient = mqttClient
        self.mqttSubscriber = mqttSubscriber
        self.mqttPublisher = mqttPublisher
        
        self.sessionStore = sessionStore
        self.deviceWebService = deviceWebService
        self.deviceRegistrationService = deviceRegistrationService
        
        self.incomingPingHandler = pingRequestHandler
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        self.startService()
        
        // Perform background fetch every 30 seconds
        UIApplication.shared.setMinimumBackgroundFetchInterval(30)
        
        return true
    }
    
    // MARK: Background fetching
    func application(_ application: UIApplication, performFetchWithCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        let newData = false
        let oldListener = self.mqttClient.mqttDidReceiveMessageListener
        self.mqttClient.mqttDidReceiveMessageListener = MQTTDidReceiveMessageListener({ (mqtt, msg, id) in
            oldListener?.handler?(mqtt, msg, id)
        })
        
        self.startService()
        
        let seconds = 10.0
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds) {
            self.mqttClient.mqttDidReceiveMessageListener = oldListener
            
            print("Done background fetching")
            
            if newData {
                completionHandler(.newData)
            } else {
                completionHandler(.noData)
            }
        }
    }
    
    // MARK: This occurs when the application enters the background
    // You only have 5 seconds to run this method or else the OS will terminate it
    func applicationDidEnterBackground(_ application: UIApplication) {
        self.stopService()
    }
    
    func applicationWillTerminate(_ application: UIApplication) {
        self.stopService()
    }
    
    // MARK: Start Service functions
    private func startService() {
        self.askNotificationPermission()
        
        self.deviceRegistrationService.getDeviceId { deviceId, err in
            if let err = err {
                print("Error getting device id: \(err.localizedDescription)")
                return
            }
            
            self.sessionStore.getAuthToken { authToken in
                guard let authToken = authToken else {
                    print("Error getting auth token")
                    return
                }
                
                self.connectMqtt(deviceId, authToken) {
                    self.subscribeToPingRequests(deviceId, authToken)
                    self.subscribeToDevices(authToken)
                }
            }
        }
    }
    
    private func askNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { (granted, error) in
            if granted {
                print("Notifications allowed")
            } else {
                print("Notifications denied")
            }
        }
    }
    
    private func connectMqtt(_ deviceId: String, _ authToken: String, _ handler: @escaping () -> Void) {
        self.mqttClient.setClientId(deviceId)
        self.mqttClient.setPassword(authToken)
        
        self.mqttClient.mqttDidConnectAckListener = MQTTDidConnectAckListener({ mqtt, ack in
            handler()
        })
        
        let isConnected = self.mqttClient.connect()
        print("Is connected? \(isConnected)")
    }
    
    private func subscribeToPingRequests(_ deviceId: String, _ authToken: String) {
        let topic = "\(deviceId)/ping/requests"
        self.mqttSubscriber.subscribe(topic) { err in
            if let err = err {
                fatalError(err.localizedDescription)
            }
            print("Successfully subscribed to \(topic)")
        }
        
        let subscriber = MQTTSubscriptionListener(topic)
        subscriber.setHandler { (msg: String?, err: Error?) in
            guard err == nil else {
                return
            }

            self.incomingPingHandler.dispatchNotification()
        }
        self.mqttSubscriber.addSubscriptionListener(subscriber)
    }
    
    private func subscribeToDevices(_ authToken: String) {
        self.deviceWebService.getDevices(authToken) { devices, err in
            guard err == nil else {
                print("Error: \(err.debugDescription)")
                return
            }

            devices.forEach { device in
                self.subscribeToDevice(device)
            }
        }
    }
    
    private func subscribeToDevice(_ device: Device) {
        if device.hasReceiveSmsCapability {
            let topic = "\(device.id)/sms/new-messages"
            let subscriber = MQTTSubscriptionListener(topic)
            subscriber.setHandler { (msg: String?, err: Error?) in
                print("Received incoming sms message")
                guard let msg = msg else {
                    return
                }
                
                guard err == nil else {
                    return
                }
                
                guard let msgStruct = ReceivedSmsMessage.fromJson(msg) else {
                    return
                }
                
                self.showNotification(msgStruct, device)
            }
            self.mqttSubscriber.addSubscriptionListener(subscriber)
            self.subscribeToTopic(topic)
        }
    }
    
    private func showNotification(_ msg: ReceivedSmsMessage, _ device: Device) {
        let content = UNMutableNotificationContent()
        content.title = msg.phoneNumber
        content.subtitle = "From \(device.name)"
        content.body = msg.body
        content.sound = UNNotificationSound.default

        // Show this notification 1 second from now
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)

        // Add our notification request
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }
    
    private func subscribeToTopic(_ topic: String) {
        print("Subscribing to \(topic)")
        self.mqttSubscriber.subscribe(topic) { err in
            if let err = err {
                fatalError(err.localizedDescription)
            }
            print("Successfully subscribed to \(topic)")
        }
    }
    
    private func stopService() {
        self.mqttClient.disconnect()
    }
}

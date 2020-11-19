//
//  MqttAppDelegate.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-13.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import FirebaseAuth

class MqttAppDelegate: NSObject, UIApplicationDelegate {
    
    let mqttClient: MQTTClient
    let mqttSubscriber: MQTTSubscriptionClient
    let mqttPublisher: MQTTPublisherClient
    let deviceWebService: DeviceWebService
    let incomingPingHandler: IncomingPingHandler
    let incomingSmsHandler: IncomingSmsHandler
    
    init(
        _ mqttClient: MQTTClient,
        _ mqttSubscriber: MQTTSubscriptionClient,
        _ mqttPublisher: MQTTPublisherClient,
        _ deviceWebService: DeviceWebService,
        _ pingRequestHandler: IncomingPingHandler,
        _ incomingSmsHandler: IncomingSmsHandler
    ) {
        self.mqttClient = mqttClient
        self.mqttSubscriber = mqttSubscriber
        self.mqttPublisher = mqttPublisher
        self.deviceWebService = deviceWebService
        self.incomingPingHandler = pingRequestHandler
        self.incomingSmsHandler = incomingSmsHandler
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        self.startService()
        return true
    }
    
    private func startService() {
        self.askNotificationPermission()
        
        // Get the device ID from cache
        let deviceId = UserDefaults.standard.string(forKey: "device_id") ?? "client"
        
        // Get the token immediately
        Auth.auth().currentUser?.getIDToken { token, err in
            guard let token = token else {
                print("Token not found")
                return
            }
            
            guard err == nil else {
                print("Encountered error: \(err.debugDescription)")
                return
            }
            
            self.connectMqtt(deviceId, token) {
                self.subscribeToPingRequests(deviceId, token)
                self.subscribeToDevices(token)
            }
        }
    }
    
    private func askNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { (granted, error) in
            if granted {
                print("allowed")
            } else {
                print("denied")
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
        
        let subscriber = MQTTSubscriber(topic)
        subscriber.setHandler { (msg: String?, err: Error?) in
            guard err == nil else {
                return
            }

            self.incomingPingHandler.dispatchNotification()
        }
        self.mqttSubscriber.addSubscriberHandle(subscriber)
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
        if device.hasReadSmsCapability {
            self.subscribeToTopic("\(device.id)/sms/messages/query-results")
            self.subscribeToTopic("\(device.id)/sms/threads/query-results")
        }
        
        if device.hasReceiveSmsCapability {
            let topic = "\(device.id)/sms/new-messages"
            
            self.subscribeToTopic(topic)
            let subscriber = MQTTSubscriber(topic)
            subscriber.setHandler { (msg: String?, err: Error?) in
                guard let msg = msg else {
                    return
                }
                
                guard err == nil else {
                    return
                }
                
                guard let msgStruct = ReceivedSmsMessage.fromJson(msg) else {
                    return
                }
                
                self.incomingSmsHandler.dispatchNotification(msgStruct, device)
            }
        }
        
        if device.hasSendSmsCapability {
            self.subscribeToTopic("\(device.id)/sms/send-message-results")
        }
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

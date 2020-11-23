//
//  MQTTService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-23.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI


/// A service that handles subscriptions, publishing
/// It also automatically re-authenticates when the client changes
class MQTTService: SessionStoreObserver, DeviceRegistrationStoreObserver {
    private let mqttClient: MQTTClient
    private let mqttSubscriber: MQTTSubscriptionClient
    private let mqttPublisher: MQTTPublisherClient
    
    private let sessionStore: SessionStore
    private let deviceWebService: DeviceWebService
    private let deviceRegistrationService: DeviceRegistrationService
    private let deviceRegistrationStore: DeviceRegistrationStore
    
    private let incomingPingHandler: IncomingPingHandler
    
    init(
        _ mqttClient: MQTTClient,
        _ mqttSubscriber: MQTTSubscriptionClient,
        _ mqttPublisher: MQTTPublisherClient,
        _ sessionStore: SessionStore,
        _ deviceWebService: DeviceWebService,
        _ deviceRegistrationService: DeviceRegistrationService,
        _ deviceRegistrationStore: DeviceRegistrationStore,
        _ pingRequestHandler: IncomingPingHandler
    ) {
        self.mqttClient = mqttClient
        self.mqttSubscriber = mqttSubscriber
        self.mqttPublisher = mqttPublisher
        
        self.sessionStore = sessionStore
        self.deviceWebService = deviceWebService
        self.deviceRegistrationService = deviceRegistrationService
        self.deviceRegistrationStore = deviceRegistrationStore
        
        self.incomingPingHandler = pingRequestHandler
                
        self.sessionStore.addObserver(self)
        self.deviceRegistrationStore.addObserver(self)
    }
    
    func startService() {
        print("MQTTService: starting service")
        self.askNotificationPermission()
        
        self.deviceRegistrationService.getDeviceId { deviceId, err in
            if let err = err {
                print("Error getting device id: \(err.localizedDescription)")
                return
            }
            
            guard let authToken = self.sessionStore.currentSession?.accessToken else {
                print("Error getting auth token")
                return
            }
                
            self.connectMqtt(deviceId, authToken) {
                self.subscribeToPingRequests(deviceId, authToken)
                self.subscribeToDevices(authToken)
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
            self.mqttClient.mqttDidConnectAckListener = nil
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
    
    func stopService() {
        print("MQTTService: stopping service")
        self.mqttClient.disconnect()
    }
    
    func onSessionChanged(_ oldSession: Session?, _ newSession: Session?) {
        print("MQTTService: session has changed")
        self.stopService()
        
        if let newSession = newSession {
            self.mqttClient.setPassword(newSession.accessToken)
            self.startService()
        }
    }
    
    func onDeviceRegistrationChanged(_ oldDeviceId: String?, _ newDeviceId: String?) {
        print("MQTTService: registration has changed")
        
        self.stopService()
        
        if let newDeviceId = newDeviceId {
            self.mqttClient.setClientId(newDeviceId)
            self.startService()
        }
    }
}

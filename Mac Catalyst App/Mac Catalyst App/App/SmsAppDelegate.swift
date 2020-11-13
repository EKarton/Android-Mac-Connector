//
//  SmsAppDelegate.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-13.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import FirebaseAuth

class SmsAppDelegate: NSObject, UIApplicationDelegate {
    let smsSenderService: SmsSenderService
    let getSmsThreadsService: GetSmsThreadsService
    let getSmsMessageService: GetSmsMessageService
    let receivedSmsMessageService: ReceivedSmsMessageService
    
    let mqttSubscriptionClient: MQTTSubscriptionClient
    let deviceService: DeviceService
    
    init(_ mqtt: MqttAppDelegate, _ device: DeviceAppDelegate) {
        self.smsSenderService = SmsSenderService(mqtt.mqttSubscriptionClient, mqtt.mqttPublisherClient)
        self.getSmsThreadsService = GetSmsThreadsService(mqtt.mqttSubscriptionClient, mqtt.mqttPublisherClient)
        self.getSmsMessageService = GetSmsMessageService(mqtt.mqttSubscriptionClient, mqtt.mqttPublisherClient)
        self.receivedSmsMessageService = ReceivedSmsMessageService()
        
        self.mqttSubscriptionClient = mqtt.mqttSubscriptionClient
        self.deviceService = device.deviceService
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        requestPermission()
        setupSubscriptions()
        return true
    }
    
    private func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { (granted, error) in
            if granted {
                print("denied")
            } else {
                print("allowed")
            }
        }
    }
    
    private func setupSubscriptions() {
        // Create the sms notifications
        Auth.auth().currentUser?.getIDToken { (token: String?, err: Error?) in
            guard err == nil else {
                print("Error: \(err.debugDescription)")
                return
            }
            guard let token = token else {
                print("No token")
                return
            }
            
            self.deviceService.getDevices(token) { (devices: [Device], err: Error?) in
                guard err == nil else {
                    print("Error: \(err.debugDescription)")
                    return
                }
                
                
                devices.forEach { device in
                    let handle: MQTTSubscriber = self.createOnNewSmsMessageSubscriberHandle(device)
                    self.mqttSubscriptionClient.addSubscriberHandle(handle)
                }
            }
        }
    }
    
    private func createOnNewSmsMessageSubscriberHandle(_ device: Device) -> MQTTSubscriber {
        let topic = "\(device.id)/sms/new-messages"
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
            
            self.receivedSmsMessageService.dispatchNotification(msgStruct, device)
        }
        return subscriber
    }
}

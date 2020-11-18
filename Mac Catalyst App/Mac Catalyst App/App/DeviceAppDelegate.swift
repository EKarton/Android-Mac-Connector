//
//  DeviceAppDelegate.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-13.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import FirebaseAuth

class DeviceAppDelegate: NSObject, UIApplicationDelegate {
    let deviceService: DeviceService
    let deviceWebService: DeviceWebService
    let deviceViewModel: DeviceViewModel
    let mqttSubscriptionClient: MQTTSubscriptionClient
    let receivedPingService: ReceivedPingService
    
    init(_ mqtt: MqttAppDelegate) {
        deviceService = DeviceService()
        deviceWebService = DeviceWebServiceImpl()
        deviceViewModel = DeviceViewModel(deviceWebService)
        mqttSubscriptionClient = mqtt.mqttSubscriptionClient
        receivedPingService = ReceivedPingService()
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        Auth.auth().currentUser?.getIDToken { (token: String?, err: Error?) in
            guard let token = token else {
                print("Token not found")
                return
            }
            
            guard err == nil else {
                print("Encountered error: \(err.debugDescription)")
                return
            }
            
            self.deviceService.getDevices(token) { (devices: [Device], err: Error?) in
                guard err == nil else {
                    print("Error: \(err.debugDescription)")
                    return
                }

                devices.forEach { device in
                    self.setupSubscriptionsForDevice(device)
                }
                
                if let deviceId = UserDefaults.standard.string(forKey: "device_id") {
                    self.setupPingRequestSubscription(deviceId)
                }
            }
        }
        return true
    }
    
    private func setupPingRequestSubscription(_ deviceId: String) {
        let topic = "\(deviceId)/ping/requests"
        self.mqttSubscriptionClient.subscribe(topic) { err in
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

            self.receivedPingService.dispatchNotification()
        }
        self.mqttSubscriptionClient.addSubscriberHandle(subscriber)
    }
    
    private func setupSubscriptionsForDevice(_ device: Device) {
        let deviceId = device.id
        print("Subscribing to device \(deviceId)")
        self.mqttSubscriptionClient.subscribe("\(deviceId)/sms/messages/query-results") { err in
            if let err = err {
                fatalError(err.localizedDescription)
            }
            print("Successfully subscribed to \("\(deviceId)/sms/messages/query-results")")
        }

        self.mqttSubscriptionClient.subscribe("\(deviceId)/sms/threads/query-results") { err in
            if let err = err {
                fatalError(err.localizedDescription)
            }
            print("Successfully subscribed to \("\(deviceId)/sms/threads/query-results")")
        }

        self.mqttSubscriptionClient.subscribe("\(deviceId)/sms/send-message-results") { err in
            if let err = err {
                fatalError(err.localizedDescription)
            }
            print("Successfully subscribed to \("\(deviceId)/sms/send-message-results")")
        }

        self.mqttSubscriptionClient.subscribe("\(deviceId)/sms/new-messages") { err in
            if let err = err {
                fatalError(err.localizedDescription)
            }
            print("Successfully subscribed to \("\(deviceId)/sms/new-messages")")
        }
    }
}

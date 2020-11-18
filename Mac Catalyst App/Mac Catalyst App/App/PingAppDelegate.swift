//
//  PingAppDelegate.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import FirebaseAuth

class PingAppDelegate: NSObject, UIApplicationDelegate {
    
    let mqttSubscriptionClient: MQTTSubscriptionClient
    let pingDeviceService: PingDeviceService
    let receivedPingService: ReceivedPingService
    
    init(_ mqtt: MqttAppDelegate) {
        self.mqttSubscriptionClient = mqtt.mqttSubscriptionClient
        self.pingDeviceService = PingDeviceService(mqtt.mqttPublisherClient)
        self.receivedPingService = ReceivedPingService()
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        return true
    }
}

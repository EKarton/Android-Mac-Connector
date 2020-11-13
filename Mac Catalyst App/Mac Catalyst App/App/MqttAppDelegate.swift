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
    let mqttSubscriptionClient: MQTTSubscriptionClient
    let mqttPublisherClient: MQTTPublisherClient
    
    override init() {
        self.mqttClient = MQTTClient("192.168.0.102", 8888, "client", "username", "password")
        self.mqttSubscriptionClient = MQTTSubscriptionClient(self.mqttClient)
        self.mqttPublisherClient = MQTTPublisherClient(self.mqttClient)
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Get the token immediately
        Auth.auth().currentUser?.getIDToken { (token: String?, err: Error?) in
            guard let token = token else {
                print("Token not found")
                return
            }
            
            guard err == nil else {
                print("Encountered error: \(err.debugDescription)")
                return
            }
            
            self.mqttClient.setPassword(token)
            let isConnected = self.mqttClient.connect()
            print("Is connected? \(isConnected) | access token: \(token)")
        }
        return true
    }
}

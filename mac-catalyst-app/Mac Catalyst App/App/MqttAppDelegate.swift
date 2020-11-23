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
    private let mqttService: MQTTService
        
    init(
        _ mqttService: MQTTService
    ) {
        self.mqttService = mqttService
        super.init()
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        self.mqttService.startService()
        
        // Perform background fetch every 30 seconds
        UIApplication.shared.setMinimumBackgroundFetchInterval(30)
        
        return true
    }
    
    // MARK: Background fetching
    func application(_ application: UIApplication, performFetchWithCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        self.mqttService.startService()
    }
    
    // MARK: This occurs when the application enters the background
    // You only have 5 seconds to run this method or else the OS will terminate it
    func applicationDidEnterBackground(_ application: UIApplication) {
        self.mqttService.stopService()
    }
    
    func applicationWillTerminate(_ application: UIApplication) {
        self.mqttService.stopService()
    }
}

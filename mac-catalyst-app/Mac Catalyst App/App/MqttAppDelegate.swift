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

class MqttAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private let mqttService: MQTTService
    private let mqttPublisher: MQTTPublisherClient
    private let jsonEncoder = JSONEncoder()
        
    init(_ mqttService: MQTTService, _ mqttPublisher: MQTTPublisherClient) {
        self.mqttService = mqttService
        self.mqttPublisher = mqttPublisher
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
        
        super.init()
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        self.mqttService.startService()
        
        // Perform background fetch every 30 seconds
        UIApplication.shared.setMinimumBackgroundFetchInterval(30)
        
        UNUserNotificationCenter.current().delegate = self
        
        return true
    }
    
    // MARK: Background fetching
    func application(_ application: UIApplication, performFetchWithCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        self.mqttService.startService()
    }
    
    // MARK: notification handler
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        print("Handling notifications")
        
        // Get the meeting ID from the original notification.
        let userInfo = response.notification.request.content.userInfo
        guard let deviceId = userInfo["device_id"] as? String else {
            return
        }
        
        guard let notificationId = userInfo["notification_id"] as? String else {
            return
        }
        
        // Perform the task associated with the action.
        if let textResponse = response as? UNTextInputNotificationResponse {
            let jsonStruct = NotificationResponse(
                key: notificationId,
                actionType: "direct_reply_action",
                actionTitle: textResponse.actionIdentifier,
                actionReplyMessage: textResponse.userText
            )
            
            guard let jsonData = try? jsonEncoder.encode(jsonStruct) else {
                return
            }
            
            guard let jsonString = String(data: jsonData, encoding: .utf8) else {
                return
            }
            
            let topic = "\(deviceId)/notification/responses"
            self.mqttPublisher.publish(topic, jsonString)
            
        } else {
            let jsonStruct = NotificationResponse(
                key: notificationId,
                actionType: "action_button",
                actionTitle: response.actionIdentifier
            )
            
            guard let jsonData = try? jsonEncoder.encode(jsonStruct) else {
                return
            }
            
            guard let jsonString = String(data: jsonData, encoding: .utf8) else {
                return
            }
            
            let topic = "\(deviceId)/notification/responses"
            self.mqttPublisher.publish(topic, jsonString)
        }
        
       // Always call the completion handler when done.
       completionHandler()
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

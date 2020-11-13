//
//  AppDelegate.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import UIKit
import FirebaseCore
import FirebaseAuth

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    // MQTT
    let mqttClient: MQTTClient
    let mqttSubscriptionClient: MQTTSubscriptionClient
    let mqttPublisherClient: MQTTPublisherClient
    
    // Stores and services
    let sessionStore: SessionStore
    let deviceService: DeviceService
    
    let smsSenderService: SmsSenderService
    let getSmsThreadsService: GetSmsThreadsService
    let getSmsMessageService: GetSmsMessageService
    let receivedSmsMessageService: ReceivedSmsMessageService
    
    override init() {
        self.mqttClient = MQTTClient("192.168.0.102", 8888, "client", "username", "password")
        self.mqttSubscriptionClient = MQTTSubscriptionClient(self.mqttClient)
        self.mqttPublisherClient = MQTTPublisherClient(self.mqttClient)
        
        self.sessionStore = SessionStore()
        self.deviceService = DeviceService()
        
        self.smsSenderService = SmsSenderService(mqttSubscriptionClient, mqttPublisherClient)
        self.getSmsThreadsService = GetSmsThreadsService(mqttSubscriptionClient, mqttPublisherClient)
        self.getSmsMessageService = GetSmsMessageService(mqttSubscriptionClient, mqttPublisherClient)
        self.receivedSmsMessageService = ReceivedSmsMessageService()
    }
    
    // Override point for customization after application launch.
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        FirebaseApp.configure()
        requestPermission()
        connectMqtt()
        
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
    
    private func connectMqtt() {
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
            
            self.deviceService.getDevices(token) { (devices: [Device], err: Error?) in
                guard err == nil else {
                    print("Error: \(err.debugDescription)")
                    return
                }

                devices.forEach { device in
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
                    
                    self.mqttSubscriptionClient.addSubscriberHandle(
                        self.createOnNewSmsMessageSubscriberHandle(device)
                    )
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

    // Called when a new scene session is being created.
    // Use this method to select a configuration to create the new scene with.
    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    // Called when the user discards a scene session.
    // If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
    // Use this method to release any resources that were specific to the discarded scenes, as they will not return.
    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        
    }
    
    // Called after didFinishLaunchingWithOptions or when the app becomes active
    func applicationWillEnterForeground(_ application: UIApplication) {
        
    }
    
    // Called after applicationWillEnterForeground(), to finish up transition to the foreground
    func applicationDidBecomeActive(_ application: UIApplication) {
        
    }
    
    // This occurs when the app is about to move from active to inactive state
    // Happens when the phone call or SMS message or Calendar alerts or when the user quits the app
    func applicationWillResignActive(_ application: UIApplication) {
        
    }
    
    // This occurs when the application enters the background
    // You only have 5 seconds to run this method or else the OS will terminate it
    func applicationDidEnterBackground(_ application: UIApplication) {
        
    }
    
    // Called when your app is going to terminate
    // You only have 5 seconds to clean your app or the OS will terminate it
    func applicationWillTerminate(_ application: UIApplication) {
        self.mqttClient.disconnect()
    }
}


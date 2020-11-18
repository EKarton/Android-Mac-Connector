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
    
    let mqtt: MqttAppDelegate
    let auth: AuthAppDelegate
    let device: DeviceAppDelegate
    let sms: SmsAppDelegate
    let ping: PingAppDelegate
    
    override init() {
        self.mqtt = MqttAppDelegate()
        self.auth = AuthAppDelegate()
        self.device = DeviceAppDelegate(mqtt)
        self.sms = SmsAppDelegate(mqtt, device)
        self.ping = PingAppDelegate(mqtt)
    }
    
    // Override point for customization after application launch.
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        return self.auth.application(application, didFinishLaunchingWithOptions: launchOptions)
            && self.mqtt.application(application, didFinishLaunchingWithOptions: launchOptions)
            && self.device.application(application, didFinishLaunchingWithOptions: launchOptions)
            && self.sms.application(application, didFinishLaunchingWithOptions: launchOptions)
            && self.ping.application(application, didFinishLaunchingWithOptions: launchOptions)
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
    }
}


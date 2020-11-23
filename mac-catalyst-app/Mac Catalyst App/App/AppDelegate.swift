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
    
    let context: AppContext
    let mqtt: MqttAppDelegate
    let firebase: FirebaseAppDelegate
    
    override init() {
        self.context = AppContext()
        self.mqtt = MqttAppDelegate(context.mqttService)
        self.firebase = FirebaseAppDelegate(context.sessionStore)
    }
    
    // Override point for customization after application launch.
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        return self.firebase.application(application, didFinishLaunchingWithOptions: launchOptions)
            && self.mqtt.application(application, didFinishLaunchingWithOptions: launchOptions)
    }
    
    // Called when a background fetch is triggered by the OS
    func application(_ application: UIApplication, performFetchWithCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        print("Background fetching")
        self.mqtt.application(application, performFetchWithCompletionHandler: completionHandler)
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
        print("applicationWillEnterForeground")
    }
    
    // Called after applicationWillEnterForeground(), to finish up transition to the foreground
    func applicationDidBecomeActive(_ application: UIApplication) {
        print("applicationDidBecomeActive")
    }
    
    // This occurs when the app is about to move from active to inactive state
    // Happens when the phone call or SMS message or Calendar alerts or when the user quits the app
    func applicationWillResignActive(_ application: UIApplication) {
        print("applicationWillResignActive")
    }
    
    // This occurs when the application enters the background
    // You only have 5 seconds to run this method or else the OS will terminate it
    func applicationDidEnterBackground(_ application: UIApplication) {
        print("applicationDidEnterBackground")
        self.mqtt.applicationDidEnterBackground(application)
    }
    
    // Called when your app is going to terminate
    // You only have 5 seconds to clean your app or the OS will terminate it
    func applicationWillTerminate(_ application: UIApplication) {
        print("applicationWillTerminate")
        self.mqtt.applicationWillTerminate(application)
    }
}


//
//  AuthAppDelegate.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-13.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import FirebaseCore

class FirebaseAppDelegate: NSObject, UIApplicationDelegate {
    
    private let sessionStore: SessionStore
    
    init(_ sessionStore: SessionStore) {
        self.sessionStore = sessionStore
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        FirebaseApp.configure()
        sessionStore.listen()
        return true
    }
}

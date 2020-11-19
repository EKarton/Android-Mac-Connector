//
//  ReceivedPingService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class IncomingPingHandler {
    func dispatchNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Ping!"
        content.body = "Hello!"
        content.sound = UNNotificationSound.default

        // Show this notification 1 second from now
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)

        // Add our notification request
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }
}

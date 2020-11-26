//
//  Notification.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-25.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

// Represents a notification that was received by a device
struct NewNotification: Codable {
    var id: String
    var title: String?
    var text: String?
    var appName: String?
    var timePosted: Int?
    var actions: [NotificationActions]
}

// Represents an action that the user can perform on the notification
struct NotificationActions: Codable {
    var type: String
    var text: String
}

// Represents a response a user has made with the notification
struct NotificationResponse: Codable {
    var key: String
    var actionType: String
    var actionTitle: String
    var actionReplyMessage: String?
}


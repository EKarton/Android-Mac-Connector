//
//  SmsThread.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

struct SmsThread: Codable {
    var threadId: String
    var phoneNumber: String
    var contactName: String?
    var numUnreadMessages: Int
    var numMessages: Int
    var lastMessageSent: String
    var timeLastMessageSent: Int
}

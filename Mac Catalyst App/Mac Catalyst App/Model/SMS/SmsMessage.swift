//
//  SmsMessage.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

struct SmsMessage: Codable {
    var messageId: String
    var phoneNumber: String
    var person: String?
    var body: String
    var readState: Bool
    var time: Int
    var type: String
}

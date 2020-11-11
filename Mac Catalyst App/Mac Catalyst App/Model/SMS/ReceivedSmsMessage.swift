//
//  ReceivedSmsMessage.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

struct ReceivedSmsMessage {
    var phoneNumber: String
    var body: String
    var timestamp: Int
}

#if DEBUG
let receivedSmsMessages = [
    ReceivedSmsMessage(phoneNumber: "647-607-6358", body: "Hi there", timestamp: 10),
    ReceivedSmsMessage(phoneNumber: "647-607-6358", body: "Hi there again", timestamp: 11)
]
#endif

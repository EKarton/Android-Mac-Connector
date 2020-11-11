//
//  SmsReaderService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class SmsReaderService: ObservableObject {
    func fetchSmsThreads(_ device: Device, _ handler: ([SmsThread]) -> Void) {
        let smsThreads = [
            SmsThread(threadId: "1", phoneNumber: "647-607-6358", contactName: "Bob Smith", lastMessageSent: "Hey Sam! Wanna go out for lunch sometime?", timeLastMessageSent: 1604955495),
            
            SmsThread(threadId: "2", phoneNumber: "193-230-3423", contactName: "Tom Cleveland", lastMessageSent: "What are your plans for today?", timeLastMessageSent: 1604233800),
            
            SmsThread(threadId: "3", phoneNumber: "193-230-3423", contactName: "Swift UI", lastMessageSent: "Check out this new thing I got for Christmas", timeLastMessageSent: 1602331200),
            
            SmsThread(threadId: "4", phoneNumber: "123-1231-1231", contactName: "Work", lastMessageSent: "Are you almost done with your task?", timeLastMessageSent: 1262347200)
        ]
        handler(smsThreads)
    }
    
    func fetchSmsMessages(_ device: Device, _ threadId: String, _ handler: ([SmsMessage]) -> Void) {
        
        let messages = [
            SmsMessage(messageId: "1", address: "123-456-7890", person: "Bob Smith", body: "Hey there!", time: 1),
            SmsMessage(messageId: "2", address: "647-607-6358", person: "Sam Smith", body: "Hi!", time: 1),
            SmsMessage(messageId: "3", address: "123-456-7890", person: "Bob Smith", body: "Are you almost done with your project?", time: 1),
            SmsMessage(messageId: "4", address: "647-607-6358", person: "Sam Smith", body: "Not yet", time: 1),
            SmsMessage(messageId: "5", address: "123-456-7890", person: "Bob Smith", body: "Cool, lemme know when it's done.", time: 1)
        ]
        
        handler(messages)
    }
}

//
//  SmsMessagesViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class SmsMessageViewModel: ObservableObject {
    @Published var messages = [SmsMessage]()
    @Published var messagesInFlight = [String]()
    
    private var getSmsMessageService: GetSmsMessageService
    private var sendSmsService: SmsSenderService
    
    init(_ smsMessageService: GetSmsMessageService, _ sendSmsService: SmsSenderService) {
        self.getSmsMessageService = smsMessageService
        self.sendSmsService = sendSmsService
    }
    
    func fetchMessages(_ device: Device, _ threadId: String, _ limit: Int, _ start: Int, handler: @escaping (Error?) -> Void) {
        self.getSmsMessageService.fetchSmsMessages(device, threadId, limit, start) { msgs, err in
            if let err = err {
                handler(err)
                return
            }
            
            self.messages = msgs
            handler(nil)
        }
    }
    
    func sendMessage(_ device: Device, _ phoneNumber: String, _ message: String, _ handler: @escaping (Error?) -> Void) {
        self.sendSmsService.sendSms(device, phoneNumber, message) { err in
            handler(err)
        }
    }
}

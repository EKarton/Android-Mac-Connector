//
//  IncomingSmsListener.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class ReceivedSmsListener: MQTTSubscriptionListener {
    
    private let deviceId: String
    private let phoneNumber: String
    
    public var onMessageReceived: (ReceivedSmsMessage) -> Void = { msg in }
    public var onErrorReceived: (Error) -> Void = { err in }
    
    init(_ deviceId: String, _ phoneNumber: String) {
        self.deviceId = deviceId
        self.phoneNumber = phoneNumber
        
        super.init("\(deviceId)/sms/new-messages")
        self.setHandler()
    }
    
    private func setHandler() {
        super.setHandler() { msg, err in
            if let err = err {
                self.onErrorReceived(err)
                return
            }
            
            guard let msg = msg else {
                return
            }
            
            guard let receivedMessage = ReceivedSmsMessage.fromJson(msg) else {
                return
            }
            
            guard receivedMessage.phoneNumber == self.phoneNumber else {
                return
            }
            
            self.onMessageReceived(receivedMessage)
        }
    }
}

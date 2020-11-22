//
//  SentSmsResultsListener.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SendSmsResultsPayload: Codable {
    var messageId: String
    var status: String
    var reason: String?
}

class SentSmsResultsListener: MQTTSubscriptionListener {
    private let deviceId: String
    private var jsonDecoder = JSONDecoder()
    
    public var onResultsReceived: (SendSmsResultsPayload) -> Void = { payload in }
    public var onErrorReceived: (Error) -> Void = { err in }
    
    override init(_ deviceId: String) {
        self.deviceId = deviceId
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
        
        super.init("\(deviceId)/sms/send-message-results")
        self.setHandler()
    }
    
    private func setHandler() {
        super.setHandler { (msg, err) in
            print("Got sent sms results")
            
            if let err = err {
                self.onErrorReceived(err)
                return
            }
            
            guard let msg = msg else {
                return
            }
            
            guard let json = msg.data(using: .utf8) else {
                return
            }
            
            guard let payload = try? self.jsonDecoder.decode(SendSmsResultsPayload.self, from: json) else {
                return
            }
            
            self.onResultsReceived(payload)
        }
    }
}

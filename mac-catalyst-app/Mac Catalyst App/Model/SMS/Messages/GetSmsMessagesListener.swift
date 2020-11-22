//
//  GetSmsMessagesListener.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import Foundation

struct GetSmsMessagesRequestPayload: Codable {
    var threadId: String
    var limit: Int
    var start: Int
}

struct GetSmsMessagesResponsePayload: Codable {
    var threadId: String
    var limit: Int
    var start: Int
    var messages: [SmsMessage]
}

class GetSmsMessagesListener: MQTTSubscriptionListener {
    private let deviceId: String
    private let threadId: String
    private let jsonDecoder = JSONDecoder()
    
    public var onResponseReceived: (GetSmsMessagesResponsePayload) -> Void = { payload in }
    public var onErrorReceived: (Error) -> Void = { err in }
    
    init(_ deviceId: String, _ threadId: String) {
        self.deviceId = deviceId
        self.threadId = threadId
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
        
        super.init("\(deviceId)/sms/messages/query-results")
        self.setHandler()
    }
    
    private func setHandler() {
        super.setHandler { (msg, err) in
            print("Got sms messages")
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
                        
            guard let payload = try? self.jsonDecoder.decode(GetSmsMessagesResponsePayload.self, from: json) else {
                return
            }
            
            guard payload.threadId == self.threadId else {
                return
            }
            
            self.onResponseReceived(payload)
        }
    }
}

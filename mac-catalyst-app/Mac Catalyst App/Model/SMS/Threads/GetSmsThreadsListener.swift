//
//  GetSmsThreadsListener.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import Foundation

struct GetSmsThreadsResponsePayload: Codable {
    var limit: Int
    var start: Int
    var threads: [SmsThread]
}

class GetSmsThreadsListener: MQTTSubscriptionListener {
    
    private let deviceId: String
    private let jsonDecoder = JSONDecoder()
    
    public var onMessageReceived: (GetSmsThreadsResponsePayload) -> Void = { msg in }
    public var onErrorHandler: (Error) -> Void = { err in }
    
    override init(_ deviceId: String) {
        self.deviceId = deviceId
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
        
        super.init("\(deviceId)/sms/threads/query-results")
        self.setHandler()
    }
    
    func setHandler() {
        super.setHandler { (msg, err) in
            print("Got sms threads results")
            
            if let err = err {
                self.onErrorHandler(err)
                return
            }
            
            guard let msg = msg else {
                return
            }
            
            guard let json = msg.data(using: .utf8) else {
                return
            }
            
            guard let payload = try? self.jsonDecoder.decode(GetSmsThreadsResponsePayload.self, from: json) else {
                return
            }
            
            self.onMessageReceived(payload)
        }
    }
}

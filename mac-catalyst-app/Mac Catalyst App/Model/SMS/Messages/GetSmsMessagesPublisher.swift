//
//  GetSmsMessagesPublisher.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class GetSmsMessagesPublisher {
    
    private let mqttPublisher: MQTTPublisherClient
    private let deviceId: String
    private let threadId: String
    private let jsonEncoder = JSONEncoder()
    
    init(_ mqttPublisher: MQTTPublisherClient, _ deviceId: String, _ threadId: String) {
        self.mqttPublisher = mqttPublisher
        self.deviceId = deviceId
        self.threadId = threadId
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
    }
    
    func requestSmsMessages() {
        let topic = "\(deviceId)/sms/messages/query-requests"
        let publishPayload = GetSmsMessagesRequestPayload(
            threadId: self.threadId, limit: 10000, start: 0
        )
        
        let jsonData = try! jsonEncoder.encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        self.mqttPublisher.publish(topic, jsonString)
    }
}

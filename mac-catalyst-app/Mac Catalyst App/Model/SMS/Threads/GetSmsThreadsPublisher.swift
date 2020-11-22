//
//  GetSmsThreadsPublisher.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import Foundation

struct GetSmsThreadsRequestPayload: Codable {
    var limit: Int
    var start: Int
}

class GetSmsThreadsPublisher {
    
    private let mqttPublisher: MQTTPublisherClient
    private let deviceId: String
    private let jsonEncoder = JSONEncoder()
    
    init(_ mqttPublisher: MQTTPublisherClient, _ deviceId: String) {
        self.mqttPublisher = mqttPublisher
        self.deviceId = deviceId
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
    }
    
    func publish(_ limit: Int, _ start: Int) {
        let publishTopic = "\(deviceId)/sms/threads/query-requests"
        let publishPayload = GetSmsThreadsRequestPayload(limit: limit, start: start)
        let jsonData = try! JSONEncoder().encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        self.mqttPublisher.publish(publishTopic, jsonString)
    }
}

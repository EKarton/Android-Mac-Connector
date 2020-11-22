//
//  SendSmsPublisher.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import Foundation

struct SendSmsRequestPayload: Codable {
    var phone_number: String
    var message: String
    var messageId: String = UUID().uuidString
}

class SendSmsPublisher {
    
    private let mqttPublisher: MQTTPublisherClient
    private let deviceId: String
    private let phoneNumber: String
    
    private let jsonEncoder = JSONEncoder()
    
    init(_ mqttPublisher: MQTTPublisherClient, _ deviceId: String, _ phoneNumber: String) {
        self.mqttPublisher = mqttPublisher
        self.deviceId = deviceId
        self.phoneNumber = phoneNumber
        
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
    }
    
    func sendMessage(_ message: String) {
        let publishTopic = "\(deviceId)/sms/send-message-requests"
        let publishPayload = SendSmsRequestPayload(phone_number: phoneNumber, message: message)
        
        let jsonData = try! jsonEncoder.encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        self.mqttPublisher.publish(publishTopic, jsonString)
    }
}

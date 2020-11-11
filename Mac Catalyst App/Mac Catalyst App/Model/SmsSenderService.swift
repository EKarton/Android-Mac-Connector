//
//  SmsSenderService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SendSmsRequestPayload: Codable {
    var phone_number: String
    var message: String
    var message_id: String = UUID().uuidString
}

struct SendSmsResultsPayload: Codable {
    var message_id: String
    var result_code: String
}

class SmsSenderService: ObservableObject {
    private var jsonEncoder = JSONEncoder()
    private var jsonDecoder = JSONDecoder()
    private var mqttClient: MQTTClient
    
    init(_ mqttClient: MQTTClient) {
        self.mqttClient = mqttClient
    }
    
    func sendSms(_ device: Device, _ phoneNumber: String, _ message: String, _ handler: @escaping (Error?) -> Void) {
        do {
            let publishTopic = "\(device.id)/send-sms-request"
            let publishPayload = SendSmsRequestPayload(phone_number: phoneNumber, message: message)
            let jsonData = try JSONEncoder().encode(publishPayload)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            
            let subscriberTopic = "\(device.id)/send-sms-results"
            let subscriber = MQTTSubscriber(subscriberTopic)
            
            subscriber.setHandler { msg in
                guard let json = msg.data(using: .utf8) else {
                    return
                }
                
                guard let payload = try? JSONDecoder().decode(SendSmsResultsPayload.self, from: json) else {
                    return
                }
                
                guard payload.message_id == publishPayload.message_id else {
                    return
                }
                
                self.mqttClient.unsubscribe(subscriber)
                handler(nil)
            }
            
            self.mqttClient.publish(publishTopic, jsonString)
            handler(nil)
            
        } catch { handler(error) }
    }
}

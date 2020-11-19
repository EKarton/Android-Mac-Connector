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
    var messageId: String = UUID().uuidString
}

struct SendSmsResultsPayload: Codable {
    var messageId: String
    var status: String
    var reason: String?
}

class SmsSenderService: ObservableObject {
    private var jsonEncoder = JSONEncoder()
    private var jsonDecoder = JSONDecoder()
    
    private var mqttSubcription: MQTTSubscriptionClient
    private var mqttPublisher: MQTTPublisherClient
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient) {
        self.mqttSubcription = mqttSubcription
        self.mqttPublisher = mqttPublisher
        
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
    }
    
    func sendSms(_ device: Device, _ phoneNumber: String, _ message: String, _ handler: @escaping (Error?) -> Void) {
        print("Sending message \(message) to \(phoneNumber)")
        
        let publishTopic = "\(device.id)/sms/send-message-requests"
        let publishPayload = SendSmsRequestPayload(phone_number: phoneNumber, message: message)
        
        let jsonData = try! jsonEncoder.encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        let subscriber = MQTTSubscriber("\(device.id)/sms/send-message-results")
        
        subscriber.setHandler { (msg, err) in
            print("Got sent sms results:", msg!)
            
            guard err == nil else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler(err)
                return
            }
            
            guard let msg = msg else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler(nil)
                return
            }
            
            guard let json = msg.data(using: .utf8) else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler(nil)
                return
            }
            
            guard let payload = try? self.jsonDecoder.decode(SendSmsResultsPayload.self, from: json) else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler(nil)
                return
            }
            
            // Check if the msg is for us, and if not, we keep on waiting
            guard payload.messageId == publishPayload.messageId else {
                return
            }
            
            self.mqttSubcription.removeSubscriberHandle(subscriber)
            handler(nil)
        }
        
        self.mqttSubcription.addSubscriberHandle(subscriber)
        self.mqttPublisher.publish(publishTopic, jsonString)
    }
}

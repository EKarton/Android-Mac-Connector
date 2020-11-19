//
//  GetSmsMessageService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-11.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

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
class GetSmsMessageService: ObservableObject {
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
    
    func fetchSmsMessages(_ device: Device, _ threadId: String, _ limit: Int, _ start: Int, _ handler: @escaping ([SmsMessage], Error?) -> Void) {
        let publishTopic = "\(device.id)/sms/messages/query-requests"
        let publishPayload = GetSmsMessagesRequestPayload(
            threadId: threadId, limit: limit, start: start
        )
        
        let jsonData = try! jsonEncoder.encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        let subscriber = MQTTSubscriber("\(device.id)/sms/messages/query-results")
        subscriber.setHandler { (msg, err) in
            print("Got sms msg results")
            
            guard err == nil else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler([SmsMessage](), err)
                return
            }
            
            guard let msg = msg else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler([SmsMessage](), nil)
                return
            }
            
            guard let json = msg.data(using: .utf8) else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler([SmsMessage](), nil)
                return
            }
                        
            guard let payload = try? self.jsonDecoder.decode(GetSmsMessagesResponsePayload.self, from: json) else {
                self.mqttSubcription.removeSubscriberHandle(subscriber)
                handler([SmsMessage](), nil)
                return
            }
            
            // Check if the messaeg is for us, and if not, we keep waiting for the message to come to us
            let isProperResponse = (payload.limit == publishPayload.limit) &&
                (payload.start == publishPayload.start) &&
                (payload.threadId == publishPayload.threadId)
            
            guard isProperResponse else {
                return
            }
            
            self.mqttSubcription.removeSubscriberHandle(subscriber)
            handler(payload.messages, nil)
        }
        
        self.mqttSubcription.addSubscriberHandle(subscriber)
        self.mqttPublisher.publish(publishTopic, jsonString)
    }
}

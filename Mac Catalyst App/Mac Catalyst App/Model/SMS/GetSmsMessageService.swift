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
        
        do {
            let publishTopic = "\(device.id)/sms/messages/query-requests"
            let publishPayload = GetSmsMessagesRequestPayload(
                threadId: threadId, limit: limit, start: start
            )
            
            let jsonData = try jsonEncoder.encode(publishPayload)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            
            let subscriberTopic = "\(device.id)/sms/messages/query-results"
            let subscriber = MQTTSubscriber(subscriberTopic)
            
            subscriber.setHandler { msg in
                print("Got sms msg: \(msg)")
                guard let json = msg.data(using: .utf8) else {
                    return
                }
                
                do {
                    let payload = try self.jsonDecoder.decode(GetSmsMessagesResponsePayload.self, from: json)
                    let isProperResponse = (payload.limit == publishPayload.limit) &&
                            (payload.start == publishPayload.start) &&
                            (payload.threadId == publishPayload.threadId)
                    
                    if !isProperResponse {
                        return
                    }
                    
                    self.mqttSubcription.unsubscribe(subscriber) { _ in
                        handler(payload.messages, nil)
                    }
                } catch {
                    self.mqttSubcription.unsubscribe(subscriber) { _ in
                        handler([SmsMessage](), error)
                    }
                }
            }
            
            self.mqttSubcription.subscribe(subscriber) { error in
                if error == nil {
                    self.mqttPublisher.publish(publishTopic, jsonString)
                }
            }
            
        } catch { handler([SmsMessage](), error) }
    }
}

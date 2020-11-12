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
    private var mqttClient: MQTTClient
    
    init(_ mqttClient: MQTTClient) {
        self.mqttClient = mqttClient
    }
    
    func fetchSmsMessages(_ device: Device, _ threadId: String, _ limit: Int, _ start: Int, _ handler: @escaping ([SmsMessage], Error?) -> Void) {
        
        do {
            let publishTopic = "\(device.id)/sms/messages/query-requests"
            let publishPayload = GetSmsMessagesRequestPayload(
                threadId: threadId, limit: limit, start: start
            )
            
            let jsonEncoder = JSONEncoder()
            jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
            
            let jsonData = try jsonEncoder.encode(publishPayload)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            
            let subscriberTopic = "\(device.id)/sms/messages/query-results"
            let subscriber = MQTTSubscriber(subscriberTopic)
            
            subscriber.setHandler { msg in
                guard let json = msg.data(using: .utf8) else {
                    return
                }
                
                let jsonDecoder = JSONDecoder()
                jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
                
                do {
                    let payload = try jsonDecoder.decode(GetSmsMessagesResponsePayload.self, from: json)
                    let isProperResponse = (payload.limit == publishPayload.limit) &&
                            (payload.start == publishPayload.start) &&
                            (payload.threadId == publishPayload.threadId)
                    
                    if !isProperResponse {
                        return
                    }
                    
                    self.mqttClient.unsubscribe(subscriber) { _ in
                        handler(payload.messages, nil)
                    }
                } catch {
                    self.mqttClient.unsubscribe(subscriber) { _ in
                        handler([SmsMessage](), error)
                    }
                }
            }
            
            self.mqttClient.subscribe(subscriber) { error in
                if error == nil {
                    self.mqttClient.publish(publishTopic, jsonString)
                }
            }
            
        } catch { handler([SmsMessage](), error) }
    }
}

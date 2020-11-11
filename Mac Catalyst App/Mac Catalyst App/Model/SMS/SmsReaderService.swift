//
//  SmsReaderService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct GetSmsThreadsRequestPayload: Codable {
    var limit: Int
    var start: Int
}

struct GetSmsThreadsResponsePayload: Codable {
    var limit: Int
    var start: Int
    var threads: [SmsThread]
}

class SmsReaderService: ObservableObject {
    private var mqttClient: MQTTClient
    
    init(_ mqttClient: MQTTClient) {
        self.mqttClient = mqttClient
    }
    
    func fetchSmsThreads(_ device: Device, _ limit: Int, _ start: Int, _ handler: @escaping ([SmsThread], Error?) -> Void) {
        do {
            let publishTopic = "\(device.id)/sms/threads/query-requests"
            let publishPayload = GetSmsThreadsRequestPayload(limit: limit, start: start)
            let jsonData = try JSONEncoder().encode(publishPayload)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            
            let subscriberTopic = "\(device.id)/sms/threads/query-results"
            let subscriber = MQTTSubscriber(subscriberTopic)
            
            subscriber.setHandler { msg in
                guard let json = msg.data(using: .utf8) else {
                    return
                }
                
                let jsonDecoder = JSONDecoder()
                jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
                
                do {
                    let payload = try jsonDecoder.decode(GetSmsThreadsResponsePayload.self, from: json)
                    guard payload.limit == publishPayload.limit && payload.start == publishPayload.start else {
                        return
                    }

                    self.mqttClient.unsubscribe(subscriber) { _ in
                        handler(payload.threads, nil)
                    }
                } catch {
                    self.mqttClient.unsubscribe(subscriber) { _ in
                        handler([SmsThread](), error)
                    }
                }
            }
            
            self.mqttClient.subscribe(subscriber) { error in
                if error == nil {
                    self.mqttClient.publish(publishTopic, jsonString)
                }
            }
            
        } catch { handler([SmsThread](), error) }
    }
    
    func fetchSmsMessages(_ device: Device, _ threadId: String, _ handler: ([SmsMessage]) -> Void) {
        
        let messages = [
            SmsMessage(messageId: "1", phoneNumber: "123-456-7890", person: "Bob Smith", body: "Hey there!", time: 1),
            SmsMessage(messageId: "2", phoneNumber: "647-607-6358", person: "Sam Smith", body: "Hi!", time: 1),
            SmsMessage(messageId: "3", phoneNumber: "123-456-7890", person: "Bob Smith", body: "Are you almost done with your project?", time: 1),
            SmsMessage(messageId: "4", phoneNumber: "647-607-6358", person: "Sam Smith", body: "Not yet", time: 1),
            SmsMessage(messageId: "5", phoneNumber: "123-456-7890", person: "Bob Smith", body: "Cool, lemme know when it's done.", time: 1)
        ]
        
        handler(messages)
    }
}

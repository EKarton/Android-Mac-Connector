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

class GetSmsThreadsService: ObservableObject {
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
}

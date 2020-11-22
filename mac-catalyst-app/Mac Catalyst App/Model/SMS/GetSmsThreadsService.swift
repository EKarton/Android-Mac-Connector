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
    private var mqttSubcription: MQTTSubscriptionClient
    private var mqttPublisher: MQTTPublisherClient
    
    private var jsonDecoder = JSONDecoder()
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient) {
        self.mqttSubcription = mqttSubcription
        self.mqttPublisher = mqttPublisher
        
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
    }
    
    func fetchSmsThreads(_ device: Device, _ limit: Int, _ start: Int, _ handler: @escaping ([SmsThread], Error?) -> Void) {
        let publishTopic = "\(device.id)/sms/threads/query-requests"
        let publishPayload = GetSmsThreadsRequestPayload(limit: limit, start: start)
        let jsonData = try! JSONEncoder().encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        let subscriber = MQTTSubscriptionListener("\(device.id)/sms/threads/query-results")
        subscriber.setHandler { (msg, err) in
            print("Got sms threads results")
            
            guard err == nil else {
                self.mqttSubcription.removeSubscriptionListener(subscriber)
                handler([SmsThread](), err)
                return
            }
            
            guard let msg = msg else {
                self.mqttSubcription.removeSubscriptionListener(subscriber)
                handler([SmsThread](), nil)
                return
            }
            
            guard let json = msg.data(using: .utf8) else {
                handler([SmsThread](), nil)
                return
            }
            
            guard let payload = try? self.jsonDecoder.decode(GetSmsThreadsResponsePayload.self, from: json) else {
                handler([SmsThread](), nil)
                return
            }
            
            guard payload.limit == publishPayload.limit && payload.start == publishPayload.start else {
                return
            }
            
            self.mqttSubcription.removeSubscriptionListener(subscriber)
            handler(payload.threads, nil)
        }
        
        self.mqttSubcription.addSubscriptionListener(subscriber)
        self.mqttPublisher.publish(publishTopic, jsonString)
    }
}

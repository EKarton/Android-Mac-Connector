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

class GetSmsMessagesClient: ObservableObject {
    private var jsonEncoder = JSONEncoder()
    private var jsonDecoder = JSONDecoder()
    
    private var mqttSubscription: MQTTSubscriptionClient
    private var mqttPublisher: MQTTPublisherClient
    private var device: Device
    private var threadId: String
    
    private var subscriptionHandle: MQTTSubscriptionListener? = nil
    
    public var onResponseReceived: (GetSmsMessagesResponsePayload) -> Void = { payload in }
    public var onErrorReceived: (Error) -> Void = { err in }
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient, _ device: Device, _ threadId: String) {
        self.mqttSubscription = mqttSubcription
        self.mqttPublisher = mqttPublisher
        self.device = device
        self.threadId = threadId
        
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
    }
    
    func subscribeToSmsResults(onError: @escaping (Error?) -> Void) {
        let topic = "\(device.id)/sms/messages/query-results"
        self.mqttSubscription.subscribe(topic) { err in
            if let err = err {
                onError(err)
                return
            }
            
            let subscriber = MQTTSubscriptionListener(topic)
            subscriber.setHandler { (msg, err) in
                print("Got sms messages")
                if let err = err {
                    self.onErrorReceived(err)
                    return
                }
                
                guard let msg = msg else {
                    return
                }
                
                guard let json = msg.data(using: .utf8) else {
                    return
                }
                            
                guard let payload = try? self.jsonDecoder.decode(GetSmsMessagesResponsePayload.self, from: json) else {
                    return
                }
                
                guard payload.threadId == self.threadId else {
                    return
                }
                
                self.onResponseReceived(payload)
            }
            
            
            if let existingHandle = self.subscriptionHandle {
                self.mqttSubscription.removeSubscriptionListener(existingHandle)
            }
            
            self.mqttSubscription.addSubscriptionListener(subscriber)
            self.subscriptionHandle = subscriber
            
            onError(nil)
        }
    }
    
    func fetchSmsMessages(_ limit: Int, _ start: Int) {
        let topic = "\(device.id)/sms/messages/query-requests"
        let publishPayload = GetSmsMessagesRequestPayload(
            threadId: threadId, limit: limit, start: start
        )
        
        let jsonData = try! jsonEncoder.encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        self.mqttPublisher.publish(topic, jsonString)
    }
    
    func unsubscribeToSmsResults(handler: @escaping (Error?) -> Void) {
        if let handle = subscriptionHandle {
            self.mqttSubscription.removeSubscriptionListener(handle)
        }
        
        let topic = "\(device.id)/sms/messages/query-results"
        self.mqttSubscription.unsubscribe(topic) { err in
            handler(err)
        }
    }
}

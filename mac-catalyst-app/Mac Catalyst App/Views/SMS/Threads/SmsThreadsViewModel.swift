//
//  SmsThreadsViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class SmsThreadsViewModel: ObservableObject {
    @Published var error: Error? = nil
    @Published var threads = [SmsThread]()
    
    public var mqttSubscription: MQTTSubscriptionClient
    public var mqttPublisher: MQTTPublisherClient
    private var jsonDecoder = JSONDecoder()
    
    // Stateful objects
    private var subscriptionHandle: MQTTSubscriptionListener? = nil
    private var publishedPayload: GetSmsThreadsRequestPayload? = nil
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient) {
        self.mqttSubscription = mqttSubcription
        self.mqttPublisher = mqttPublisher
        
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
    }
    
    func subscribeToSmsThreads(_ device: Device, handler: @escaping () -> Void) {
        let topic = "\(device.id)/sms/threads/query-results"
        self.mqttSubscription.subscribe(topic) { err in
            if let err = err {
                self.error = err
                return
            }
            
            let subscriptionHandle = MQTTSubscriptionListener(topic)
            subscriptionHandle.setHandler { (msg, err) in
                print("Got sms threads results")
                
                guard err == nil else {
                    self.error = err
                    return
                }
                
                guard let msg = msg else {
                    return
                }
                
                guard let json = msg.data(using: .utf8) else {
                    return
                }
                
                guard let payload = try? self.jsonDecoder.decode(GetSmsThreadsResponsePayload.self, from: json) else {
                    return
                }
                
                guard let publishedPayload = self.publishedPayload else {
                    return
                }
                
                guard payload.limit == publishedPayload.limit && payload.start == publishedPayload.start else {
                    return
                }
                
                self.threads = payload.threads
            }
            
            if let existingHandle = self.subscriptionHandle {
                self.mqttSubscription.removeSubscriptionListener(existingHandle)
            }
            
            self.mqttSubscription.addSubscriptionListener(subscriptionHandle)
            self.subscriptionHandle = subscriptionHandle
            handler()
        }
    }
    
    func fetchThreads(_ device: Device, _ limit: Int, _ start: Int) {
        print("fetchThreads()")
        
        let publishTopic = "\(device.id)/sms/threads/query-requests"
        let publishPayload = GetSmsThreadsRequestPayload(limit: limit, start: start)
        let jsonData = try! JSONEncoder().encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        self.publishedPayload = publishPayload
        self.mqttPublisher.publish(publishTopic, jsonString)
    }
    
    func unsubscribeToSmsThreads(_ device: Device) {
        if let handle = subscriptionHandle {
            self.mqttSubscription.removeSubscriptionListener(handle)
        }
        
        let topic = "\(device.id)/sms/threads/query-results"
        self.mqttSubscription.unsubscribe(topic){ err in
            if let err = err {
                self.error = err
            }
        }
    }
}

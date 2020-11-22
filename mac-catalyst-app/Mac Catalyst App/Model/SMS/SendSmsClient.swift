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

class SendSmsClient {
    private var jsonEncoder = JSONEncoder()
    private var jsonDecoder = JSONDecoder()
    
    private var mqttSubcription: MQTTSubscriptionClient
    private var mqttPublisher: MQTTPublisherClient
    private var device: Device
    private var subscriber: MQTTSubscriptionListener? = nil
    
    public var onResultsReceived: (SendSmsResultsPayload) -> Void = { payload in }
    public var onErrorReceived: (Error) -> Void = { err in }
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient, _ device: Device) {
        self.mqttSubcription = mqttSubcription
        self.mqttPublisher = mqttPublisher
        self.device = device
        
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
    }
    
    func subscribeToSentSmsResults(handler: @escaping (Error?) -> Void) {
        let topic = "\(device.id)/sms/send-message-results"
        self.mqttSubcription.subscribe(topic, handler)
    }
    
    func setSendSmsResultsHandler(messageHandler: @escaping (SendSmsResultsPayload) -> Void) {
        if let existingHandle = self.subscriber {
            self.mqttSubcription.removeSubscriptionListener(existingHandle)
        }
        
        let subscriber = self.createSubscriber()
        self.subscriber = subscriber
        self.mqttSubcription.addSubscriptionListener(subscriber)
    }
    
    private func createSubscriber() -> MQTTSubscriptionListener {
        let subscriber = MQTTSubscriptionListener("\(device.id)/sms/send-message-results")
        subscriber.setHandler { (msg, err) in
            print("Got sent sms results:", msg!)
            
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
            
            guard let payload = try? self.jsonDecoder.decode(SendSmsResultsPayload.self, from: json) else {
                return
            }
            
            self.onResultsReceived(payload)
        }
        return subscriber
    }
    
    func sendSms(_ phoneNumber: String, _ message: String) {
        print("Sending message \(message) to \(phoneNumber)")
        
        let publishTopic = "\(device.id)/sms/send-message-requests"
        let publishPayload = SendSmsRequestPayload(phone_number: phoneNumber, message: message)
        
        let jsonData = try! jsonEncoder.encode(publishPayload)
        let jsonString = String(data: jsonData, encoding: .utf8)!
        
        self.mqttPublisher.publish(publishTopic, jsonString)
    }
    
    func removeSendSmsResultsHandler() {
        if let existingHandle = self.subscriber {
            self.mqttSubcription.removeSubscriptionListener(existingHandle)
            self.subscriber = nil
        }
    }
    
    func unsubscribeFromSentSmsResults(_ handler: @escaping (Error?) -> Void) {
        if let handle = self.subscriber {
            self.mqttSubcription.removeSubscriptionListener(handle)
        }
        
        let topic = "\(device.id)/sms/send-message-results"
        self.mqttSubcription.unsubscribe(topic, handler)
    }
}

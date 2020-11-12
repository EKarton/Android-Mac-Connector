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
        do {
            let publishTopic = "\(device.id)/send-sms-request"
            let publishPayload = SendSmsRequestPayload(phone_number: phoneNumber, message: message)
            
            let jsonData = try jsonEncoder.encode(publishPayload)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            
            let subscriberTopic = "\(device.id)/send-sms-results"
            let subscriber = MQTTSubscriber(subscriberTopic)
            
            subscriber.setHandler { msg in
                print("Got sms sent results:", msg)
                guard let json = msg.data(using: .utf8) else {
                    return
                }
                
                do {
                    let payload = try self.jsonDecoder.decode(SendSmsResultsPayload.self, from: json)
                    guard payload.messageId == publishPayload.messageId else {
                        return
                    }

                    self.mqttSubcription.unsubscribe(subscriber) { _ in
                        handler(nil)
                    }
                } catch {
                    self.mqttSubcription.unsubscribe(subscriber) { _ in
                        handler(error)
                    }
                }
            }
            
            self.mqttSubcription.subscribe(subscriber) { error in
                guard (error == nil) else {
                    print("Got error: \(error.debugDescription)")
                    handler(error)
                    return
                }
                
                self.mqttPublisher.publish(publishTopic, jsonString)
            }
            
        } catch {
            print("Encountered error when sending msg: \(error)")
            handler(error)
        }
    }
}

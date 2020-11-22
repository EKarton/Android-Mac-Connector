//
//  IncomingSmsClient.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-22.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI


/// This class is responsible for subscribing to and unsubscribing from incoming SMS that
/// is from a particular device and phone number
class IncomingSmsClient {
    private let mqttSubscription: MQTTSubscriptionClient
    private let mqttPublisher: MQTTPublisherClient
    private let device: Device
    private let phoneNumber: String
    
    private var subscriber: MQTTSubscriptionListener? = nil
    
    public var onMessageReceived: (ReceivedSmsMessage) -> Void = { msg in }
    public var onErrorReceived: (Error) -> Void = { err in }
    
    init(_ mqttSubscription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient, _ device: Device, _ phoneNumber: String) {
        self.mqttSubscription = mqttSubscription
        self.mqttPublisher = mqttPublisher
        self.device = device
        self.phoneNumber = phoneNumber
    }
    
    func subscribeToIncomingSms() {
        if let existingSubscriber = subscriber {
            self.mqttSubscription.removeSubscriptionListener(existingSubscriber)
        }
        
        let subscriber = MQTTSubscriptionListener("\(device.id)/sms/new-messages")
        subscriber.setHandler { msg, err in
            print("Received incoming sms")
            
            if let err = err {
                self.onErrorReceived(err)
                return
            }
            
            guard let msg = msg else {
                return
            }
            
            guard let receivedMessage = ReceivedSmsMessage.fromJson(msg) else {
                return
            }
            
            guard receivedMessage.phoneNumber == self.phoneNumber else {
                return
            }
            
            self.onMessageReceived(receivedMessage)
        }
        
        self.mqttSubscription.addSubscriptionListener(subscriber)
        self.subscriber = subscriber
    }
        
    func unsubscribeFromIncomingSms() {
        if let subscriber = self.subscriber {
            self.mqttSubscription.removeSubscriptionListener(subscriber)
            self.subscriber = nil
        }
    }
}

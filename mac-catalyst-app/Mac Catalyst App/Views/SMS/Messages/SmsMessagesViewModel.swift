//
//  SmsMessagesViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsMessagesInFlight: Hashable {
    var id = UUID().uuidString
    var index: Int
    var body: String
}

class SmsMessageViewModelFactory: ObservableObject {
    private var mqttSubscription: MQTTSubscriptionClient
    private var mqttPublisher: MQTTPublisherClient
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient) {
        self.mqttSubscription = mqttSubcription
        self.mqttPublisher = mqttPublisher
    }
    
    func createViewModel(_ device: Device, _ threadId: String, _ phoneNumber: String) -> SmsMessageViewModel {
        return SmsMessageViewModel(mqttSubscription, mqttPublisher, device, threadId, phoneNumber)
    }
}

/// This is a view model for the SmsMessagesView
/// It handles sending messages, refreshing messages, and receiving incoming messages
///
/// Usage:
/// 1. First, start subscribing to the required channels (ex: subscribeToSmsMessages, subscribeToIncomingSms, etc)
/// 2. Then, start calling publishing methods (ex: fetchMessages(), sendSms(), etc)
/// 3. Lastly (but most importantly), unsubscribe from the channels you have subscribed to from (1)
///
class SmsMessageViewModel: ObservableObject {
    @Published var error: Error? = nil
    @Published var messages = [SmsMessage]()
    @Published var messagesInFlight = [SmsMessagesInFlight]()
    
    private var mqttSubcription: MQTTSubscriptionClient
    private var device: Device
    private var phoneNumber: String
    
    private var smsSenderService: SendSmsClient
    private var getSmsMessagesClient: GetSmsMessagesClient
    private var incomingSmsClient: IncomingSmsClient
    
    // Are used to call refreshMessages() recursively with exponential backoff
    // when there are messages in flight
    private let cap = 64000.0
    private let base = 500.0
    private let maxAttempts = 10
    private var curAttempts = 0
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient, _ device: Device, _ threadId: String, _ phoneNumber: String) {
        self.mqttSubcription = mqttSubcription
        self.device = device
        self.phoneNumber = phoneNumber
        self.smsSenderService = SendSmsClient(mqttSubcription, mqttPublisher, device)
        self.getSmsMessagesClient = GetSmsMessagesClient(mqttSubcription, mqttPublisher, device, threadId)
        self.incomingSmsClient = IncomingSmsClient(mqttSubcription, mqttPublisher, device, phoneNumber)
        
        self.getSmsMessagesClient.onResponseReceived = { payload in
            var i = 0
            while i < self.messagesInFlight.count {
                let numNewMessages = payload.messages.count - self.messagesInFlight[i].index
                var j = 0
                while j < numNewMessages {
                    if payload.messages[j].body == self.messagesInFlight[i].body && payload.messages[j].isCurrentUser {
                        self.messagesInFlight.remove(at: i)
                        i -= 1
                        break
                    }
                    j += 1
                }
                i += 1
            }
            
            if self.messagesInFlight.count > 0 {
                let temp = min(self.cap, self.base * pow(2, Double(self.curAttempts)))
                let sleepInMs = temp / 2 + Double.random(in: 0...(temp / 2))
                self.curAttempts += 1
                
                print("Did not get messages in flight. Retrying for \(sleepInMs) ms")
                
                DispatchQueue.main.asyncAfter(deadline: .now() + sleepInMs / 1000.0) {
                    self.fetchMessages()
                }
            }
            
            self.messages = payload.messages
        }
        
        self.getSmsMessagesClient.onErrorReceived = { err in
            print("Error when fetching messages: \(err.localizedDescription)")
            self.error = err
        }
        
        self.smsSenderService.onResultsReceived = { results in
            print("Sms sent successfully? \(results.status) reason: \(String(describing: results.reason))")
        }
        
        self.smsSenderService.onErrorReceived = { err in
            print("Error when sending messages \(err.localizedDescription)")
            self.error = err
        }
        
        self.incomingSmsClient.onMessageReceived = { msg in
            self.fetchMessages()
        }
        
        self.incomingSmsClient.onErrorReceived = { err in
            print("Error when getting incoming messages \(err.localizedDescription)")
            self.error = err
        }
    }
    
    func subscribeToSmsMessages(handler: @escaping () -> Void) {
        self.getSmsMessagesClient.subscribeToSmsResults() { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func fetchMessages() {
        self.getSmsMessagesClient.fetchSmsMessages(100000, 0)
    }
    
    func unsubscribeToSmsMessages(handler: @escaping () -> Void) {
        self.getSmsMessagesClient.unsubscribeToSmsResults() { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func subscribeToSentSmsResults(handler: @escaping () -> Void) {
        smsSenderService.subscribeToSentSmsResults() { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func sendMessage(_ phoneNumber: String, _ message: String, _ handler: @escaping (Error?) -> Void) {
        self.smsSenderService.sendSms(phoneNumber, message)
        
        self.fetchMessages()
        self.curAttempts = 0
        
        let msgInFlight = SmsMessagesInFlight(
            index: self.messages.count,
            body: message
        )
        self.messagesInFlight.insert(msgInFlight, at: 0)
    }
    
    func unsubscribeFromSentSmsResults(handler: @escaping () -> Void) {
        smsSenderService.unsubscribeFromSentSmsResults() { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func listenToIncomingSms() {
        self.incomingSmsClient.subscribeToIncomingSms()
    }
    
    func removeListeningFromIncomingSms() {
        self.incomingSmsClient.unsubscribeFromIncomingSms()
    }
}

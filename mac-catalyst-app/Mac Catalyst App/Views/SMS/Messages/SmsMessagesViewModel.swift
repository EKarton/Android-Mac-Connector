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
    private var mqttPublisher: MQTTPublisherClient
    
    private var device: Device
    private var threadId: String
    private var phoneNumber: String
        
    private let getSmsMessagesPublisher: GetSmsMessagesPublisher
    private let sendSmsPublisher: SendSmsPublisher
    
    private let getSmsMessagesListener: GetSmsMessagesListener
    private let sentSmsResultsListener: SentSmsResultsListener
    private let incomingSmsListener: ReceivedSmsListener
    
    // Are used to call refreshMessages() recursively with exponential backoff
    // when there are messages in flight
    private let cap = 64000.0
    private let base = 500.0
    private let maxAttempts = 10
    private var curAttempts = 0
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient, _ device: Device, _ threadId: String, _ phoneNumber: String) {
        self.mqttSubcription = mqttSubcription
        self.mqttPublisher = mqttPublisher
        
        self.device = device
        self.threadId = threadId
        self.phoneNumber = phoneNumber
        
        self.getSmsMessagesPublisher = GetSmsMessagesPublisher(mqttPublisher, device.id, threadId)
        self.sendSmsPublisher = SendSmsPublisher(mqttPublisher, device.id, phoneNumber)
                
        self.getSmsMessagesListener = GetSmsMessagesListener(device.id, threadId)
        self.sentSmsResultsListener = SentSmsResultsListener(device.id)
        self.incomingSmsListener = ReceivedSmsListener(device.id, phoneNumber)

        self.setupGetSmsMessagesListener()
        self.setupSentSmsResultsListener()
        self.setupIncomingSmsListener()
        
        self.mqttSubcription.addSubscriptionListener(self.getSmsMessagesListener)
        self.mqttSubcription.addSubscriptionListener(self.sentSmsResultsListener)
        self.mqttSubcription.addSubscriptionListener(self.incomingSmsListener)
    }
    
    deinit {
        self.mqttSubcription.removeSubscriptionListener(self.getSmsMessagesListener)
        self.mqttSubcription.removeSubscriptionListener(self.sentSmsResultsListener)
        self.mqttSubcription.removeSubscriptionListener(self.incomingSmsListener)
    }
    
    private func setupGetSmsMessagesListener() {
        self.getSmsMessagesListener.onResponseReceived = { payload in
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
        
        self.getSmsMessagesListener.onErrorReceived = { err in
            print("Error when fetching messages: \(err.localizedDescription)")
            self.error = err
        }
    }
    
    private func setupSentSmsResultsListener() {
        self.sentSmsResultsListener.onResultsReceived = { results in
            print("Sms sent successfully? \(results.status) reason: \(String(describing: results.reason))")
        }
        
        self.sentSmsResultsListener.onErrorReceived = { err in
            print("Error when sending messages \(err.localizedDescription)")
            self.error = err
        }
    }
    
    private func setupIncomingSmsListener() {
        self.incomingSmsListener.onMessageReceived = { msg in
            self.fetchMessages()
        }
        self.incomingSmsListener.onErrorReceived = { err in
            self.error = err
        }
    }
    
    func subscribeToSmsMessages(handler: @escaping () -> Void) {
        let topic = "\(device.id)/sms/messages/query-results"
        self.mqttSubcription.subscribe(topic) { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func fetchMessages() {
        self.getSmsMessagesPublisher.requestSmsMessages()
    }
    
    func unsubscribeToSmsMessages(handler: @escaping () -> Void) {
        let topic = "\(device.id)/sms/messages/query-results"
        self.mqttSubcription.unsubscribe(topic) { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func subscribeToSentSmsResults(handler: @escaping () -> Void) {
        let topic = "\(device.id)/sms/send-message-results"
        self.mqttSubcription.subscribe(topic) { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func sendMessage(_ message: String, _ handler: @escaping (Error?) -> Void) {
        print("Sending message \(message) to \(phoneNumber)")
        sendSmsPublisher.sendMessage(message)
        
        self.fetchMessages()
        self.curAttempts = 0
        
        let msgInFlight = SmsMessagesInFlight(
            index: self.messages.count,
            body: message
        )
        self.messagesInFlight.insert(msgInFlight, at: 0)
    }
    
    func unsubscribeFromSentSmsResults(handler: @escaping () -> Void) {
        let topic = "\(device.id)/sms/send-message-results"
        self.mqttSubcription.unsubscribe(topic) { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
}

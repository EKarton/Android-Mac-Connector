//
//  MQTTOnlyOnceClient.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-12.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import CocoaMQTT

enum MQTTClientErrors: Error {
    case SubscriptionFailed(topic: String)
}

class MQTTSubscriber: Hashable {
    var id = UUID()
    var topic: String
    var handler: ((String) -> Void)? = nil
    
    init(_ topic: String) {
        self.topic = topic
    }
    
    func setHandler(_ handler: @escaping (String) -> Void) {
        self.handler = handler
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(self.id)
    }
    
    static func == (lhs: MQTTSubscriber, rhs: MQTTSubscriber) -> Bool {
        return lhs.id == rhs.id
    }
}

// This client is responsible for subscribing to topics only once
// For instance, if we run this code:
//    1. val client = MQTTOnlyOnceClient()
//    2. client.subscribe("topic") { ... }
//    3. client.subscribe("topic") { ... }
//    4. client.subscribe("topic") { ... }
//    5. client.subscribe("topic") { ... }
// it will only call client.mqtt.subscribe("topic") once and wait until a subscribeAck

// Similar case for unsubscribing from a topic. If we have this code:
//    1. val client = MQTTOnlyOnceClient()
//    2. client.subscribe("topic") { ... }
//    3. client.subscribe("topic") { ... }
//    4. client.subscribe("topic") { ... }
//    5. client.subscribe("topic") { ... }
// it will only call client.mqtt.unsubscribe("topic") once and wait until a unsubscribeAck

class MQTTSubscriptionClient {
    
    // Is a list of subscriptions that was called by this.mqttClient.subscribe() and received a subscribeAck
    private var currentSubscriptions = Set<String>()
    
    // Is a list of subscriptions that was called by this.mqttClient.subscribe() but did not receive a subscribeAck yet
    private var pendingSubscriptions = Set<String>()
    
    // Contains a map of topics to event handlers when calling this.subscribe()
    private var topicToOnSubscribeCallbacks = Dictionary<String, Set<MQTTEventHandler>>()
    
    // Is a list of subscriptions that were called by this.mqttClient.unsubscribe() but did not receive an unsubscribeAck yet
    private var pendingUnsubscriptions = Set<String>()
    
    // Contains a map of topics to event handlers when calling this.unsubscribe()
    private var topicToUnsubscribeCallbacks = Dictionary<String, Set<MQTTEventHandler>>()
    
    // Contains a map of topics to subscribers
    private var topicToSubscribers = Dictionary<String, Set<MQTTSubscriber>>()
    
    private var client: MQTTClient
    
    init(_ client: MQTTClient) {
        self.client = client
        
        self.client.mqttDidSubscribeTopicsListener = MQTTDidSubscribeAckListener(didSubscribeTopicsHandler)
        self.client.mqttDidUnsubscribeTopicsListener = MQTTDidUnsubscribeAckListener(didUnsubscribeTopicsHandler)
        self.client.mqttDidReceiveMessageListener = MQTTDidReceiveMessageListener(didReceiveMessageHandler)
    }
    
    func subscribe(_ subscriber: MQTTSubscriber, _ handler: @escaping (Error?) -> Void) {
        let topic = subscriber.topic
        
        if topicToSubscribers[topic] == nil {
            topicToSubscribers[topic] = Set<MQTTSubscriber>()
        }
        topicToSubscribers[topic]?.insert(subscriber)
        
        if currentSubscriptions.contains(topic) {
            handler(nil)
            return
        }
        
        if topicToOnSubscribeCallbacks[topic] == nil {
            topicToOnSubscribeCallbacks[topic] = Set<MQTTEventHandler>()
        }
        topicToOnSubscribeCallbacks[topic]?.insert(MQTTEventHandler(handler))
        
        if pendingSubscriptions.contains(topic) {
            return
        }
        
        pendingSubscriptions.insert(topic)
        client.mqtt.subscribe(topic, qos: .qos2)
    }
    
    func unsubscribe(_ subscriber: MQTTSubscriber, _ handler: @escaping (Error?) -> Void) {
        let topic = subscriber.topic
        
        if !currentSubscriptions.contains(topic) {
            handler(nil)
            return
        }
        
        if topicToUnsubscribeCallbacks[topic] == nil {
            topicToUnsubscribeCallbacks[topic] = Set<MQTTEventHandler>()
        }
        topicToUnsubscribeCallbacks[topic]?.insert(MQTTEventHandler(handler))
        
        if pendingUnsubscriptions.contains(topic) {
            return
        }
        
        pendingUnsubscriptions.insert(topic)
        client.mqtt.unsubscribe(topic)
    }
    
    private func didSubscribeTopicsHandler(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {
        print("didSubscribeTopics: \(success), \(failed)")
        
        for (key, _) in success {
            let topic = key as! String
            currentSubscriptions.insert(topic)
            pendingSubscriptions.remove(topic)
            
            topicToOnSubscribeCallbacks[topic]?.forEach { eventHandler in
                eventHandler.handler(nil)
            }
            
            topicToOnSubscribeCallbacks.removeValue(forKey: topic)
        }
        
        for failedTopic in failed {
            pendingSubscriptions.remove(failedTopic)
            
            topicToOnSubscribeCallbacks[failedTopic]?.forEach { eventHandler in
                eventHandler.handler(MQTTClientErrors.SubscriptionFailed(topic: failedTopic))
            }
            
            topicToOnSubscribeCallbacks.removeValue(forKey: failedTopic)
        }
    }
    
    private func didUnsubscribeTopicsHandler(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {
        print("didUnsubscribeTopics: \(topics)")
        
        for topic in topics {
            currentSubscriptions.remove(topic)
            pendingUnsubscriptions.remove(topic)
            
            topicToUnsubscribeCallbacks[topic]?.forEach { eventHandler in
                eventHandler.handler(nil)
            }
            topicToUnsubscribeCallbacks.removeValue(forKey: topic)
            
            topicToSubscribers.removeValue(forKey: topic)
        }
    }
    
    private func didReceiveMessageHandler(_ mqtt: CocoaMQTT, didReceiveMessage message: CocoaMQTTMessage, id: UInt16) {
        guard let payload = String(bytes: message.payload, encoding: .utf8) else {
            return
        }
        
        print("didReceiveMessage: \(payload)")
        
        if self.topicToSubscribers[message.topic] == nil {
            fatalError("No subscribers!")
        }
        
        if self.topicToSubscribers[message.topic]?.count == 0 {
            fatalError("No subscribers!")
        }

        self.topicToSubscribers[message.topic]?.forEach { sub in
            sub.handler?(payload)
        }
    }
}

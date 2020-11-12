//
//  MQTTClient.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import CocoaMQTT

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

class MQTTEventHandler: Hashable {
    var id = UUID()
    var handler: (Error?) -> Void
    
    init(_ handler: @escaping (Error?) -> Void) {
        self.handler = handler
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(self.id)
    }
    
    static func == (lhs: MQTTEventHandler, rhs: MQTTEventHandler) -> Bool {
        return lhs.id == rhs.id
    }
}

enum MQTTClientErrors: Error {
    case SubscriptionFailed(topic: String)
}

class MQTTClient: CocoaMQTTDelegate, ObservableObject {
    private var topicToSubscribers = Dictionary<String, Set<MQTTSubscriber>>()
    private var topicToOnSubscribeHandlers = Dictionary<String, Set<MQTTEventHandler>>()
    private var topicToOnUnsubscribeHandlers = Dictionary<String, Set<MQTTEventHandler>>()
    
    private var mqtt: CocoaMQTT
    
    init(_ host: String, _ port: UInt16, _ clientId: String, _ username: String, _ password: String) {
        let websocket = CocoaMQTTWebSocket()
        self.mqtt = CocoaMQTT(clientID: clientId, host: host, port: port, socket: websocket)

        self.mqtt.username = username
        self.mqtt.password = password
        self.mqtt.willMessage = CocoaMQTTMessage(topic: "/will", string: "dieout")
        self.mqtt.keepAlive = 60
        self.mqtt.delegate = self
    }
    
    func setUsername(_ username: String) {
        self.mqtt.username = username
    }
    
    func setPassword(_ password: String) {
        self.mqtt.password = password
    }
    
    func connect() -> Bool {
        return self.mqtt.connect()
    }
    
    func disconnect() {
        self.mqtt.disconnect()
    }
    
    func subscribe(_ subscriber: MQTTSubscriber, _ handler: @escaping (Error?) -> Void) {
        if self.topicToSubscribers[subscriber.topic] == nil {
            print("Subscribing to \(subscriber.topic)")
            
            self.mqtt.subscribe(subscriber.topic, qos: .qos2)
            self.topicToOnSubscribeHandlers[subscriber.topic] = Set<MQTTEventHandler>()
            self.topicToSubscribers[subscriber.topic] = Set<MQTTSubscriber>()
        }
        
        self.topicToSubscribers[subscriber.topic]?.insert(subscriber)
        self.topicToOnSubscribeHandlers[subscriber.topic]?.insert(MQTTEventHandler(handler))
    }
    
    func unsubscribe(_ subscriber: MQTTSubscriber, _ handler: @escaping (Error?) -> Void) {
        guard let subscribers = self.topicToSubscribers[subscriber.topic] else {
            fatalError("Topic \(subscriber.topic) was never subscribed to before!")
        }
                
        if subscribers.count == 1 {
            self.mqtt.unsubscribe(subscriber.topic)
            self.topicToSubscribers.removeValue(forKey: subscriber.topic)
            
        } else {
            self.topicToSubscribers[subscriber.topic]?.remove(subscriber)
        }
        
        if self.topicToOnUnsubscribeHandlers[subscriber.topic] == nil {
            self.topicToOnUnsubscribeHandlers[subscriber.topic] = Set<MQTTEventHandler>()
        }
        self.topicToOnUnsubscribeHandlers[subscriber.topic]?.insert(MQTTEventHandler(handler))
    }
    
    func publish(_ topic: String, _ message: String) {
        print("Publishing \(message) to \(topic)")
        self.mqtt.publish(topic, withString: message, qos: .qos2, retained: true)
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didConnectAck ack: CocoaMQTTConnAck) {
        print("didConnectAck: \(ack)")
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didPublishMessage message: CocoaMQTTMessage, id: UInt16) {
        print("didPublishMessage: \(message), \(id)")
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didPublishAck id: UInt16) {
        print("didPublishAck: \(id)")
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didReceiveMessage message: CocoaMQTTMessage, id: UInt16) {
        guard let payload = String(bytes: message.payload, encoding: .utf8) else {
            print("Not a valid UTF-8 sequence")
            return
        }
        
        print("didReceiveMessage: \(payload)")

        
        guard let subscribers = topicToSubscribers[message.topic] else {
            fatalError("No subscribers!")
        }
                
        subscribers.forEach { subscriber in
            subscriber.handler?(payload)
        }
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {
        print("didSubscribeTopics: \(success), \(failed)")
        
        for (topic, _) in success {
            if let subscribers = self.topicToOnSubscribeHandlers[topic as! String] {
                subscribers.forEach { subscriber in
                    subscriber.handler(nil)
                }
            }
            
            self.topicToOnSubscribeHandlers[topic as! String]?.removeAll()
            self.topicToOnSubscribeHandlers.removeValue(forKey: topic as! String)
        }
        
        for failedTopic in failed {
            guard let subscribers = self.topicToOnSubscribeHandlers[failedTopic] else {
                return
            }

            subscribers.forEach { subscriber in
                subscriber.handler(MQTTClientErrors.SubscriptionFailed(topic: failedTopic))
            }
            
            self.topicToOnSubscribeHandlers[failedTopic]?.removeAll()
            self.topicToOnSubscribeHandlers.removeValue(forKey: failedTopic)
        }
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {
        print("didUnsubscribeTopics: \(topics)")
        
        for topic in topics {
            if let eventListeners = self.topicToOnUnsubscribeHandlers[topic] {
                eventListeners.forEach { eventListener in
                    eventListener.handler(nil)
                }
            }
            
            self.topicToOnUnsubscribeHandlers[topic]?.removeAll()
            self.topicToOnUnsubscribeHandlers.removeValue(forKey: topic)
        }
    }
    
    internal func mqttDidPing(_ mqtt: CocoaMQTT) {
        print("mqttDidPing")
    }
    
    internal func mqttDidReceivePong(_ mqtt: CocoaMQTT) {
        print("mqttDidReceivePong")
    }
    
    internal func mqttDidDisconnect(_ mqtt: CocoaMQTT, withError err: Error?) {
        print("mqttDidDisconnect: \(String(describing: err?.localizedDescription))")
    }
}

class MQTTAuthObserverClient: MQTTClient, SessionChangedListener {
    func handler(_ oldSession: Session, _ newSession: Session) {
        print("MQTTAuthObserverClient(): Old: \(oldSession) vs \(newSession)")
        super.disconnect()
        super.setPassword(newSession.accessToken)
        
        if (newSession.isSignedIn) {
            super.connect()
        }
    }
}

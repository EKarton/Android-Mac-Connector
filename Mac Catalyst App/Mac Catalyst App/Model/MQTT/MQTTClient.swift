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

class MQTTClient: CocoaMQTTDelegate, ObservableObject {
    private var topicToSubscribers = Dictionary<String, Set<MQTTSubscriber>>()
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
    
    func subscribe(_ subscriber: MQTTSubscriber) {
        if self.topicToSubscribers[subscriber.topic] != nil {
            self.mqtt.subscribe(subscriber.topic)
            self.topicToSubscribers[subscriber.topic] = Set<MQTTSubscriber>()
        }
        
        self.topicToSubscribers[subscriber.topic]?.insert(subscriber)
    }
    
    func unsubscribe(_ subscriber: MQTTSubscriber) {
        guard var subscribers = self.topicToSubscribers[subscriber.topic] else {
            fatalError("Topic \(subscriber.topic) was never subscribed to before!")
        }
        
        if subscribers.count == 1 {
            self.mqtt.unsubscribe(subscriber.topic)
        }
        subscribers.remove(subscriber)
    }
    
    func publish(_ topic: String, _ message: String) {
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
        print("didReceiveMessage: \(message)")
        
        guard let subscribers = topicToSubscribers[message.topic] else {
            return
        }
                
        subscribers.forEach { subscriber in
            subscriber.handler?(message.description)
        }
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {
        print("didSubscribeTopics: \(success), \(failed)")
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {
        print("didUnsubscribeTopics: \(topics)")
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

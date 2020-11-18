//
//  MQTTClient.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import CocoaMQTT

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

class MQTTDelegateEventHandler<T>: Hashable {
    var id = UUID()
    var handler: T?
    
    init(_ handler: T) {
        self.handler = handler
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(self.id)
    }
    
    static func == (lhs: MQTTDelegateEventHandler, rhs: MQTTDelegateEventHandler) -> Bool {
        return lhs.id == rhs.id
    }
}

// MARK: The event listeners
class MQTTDidConnectAckListener: MQTTDelegateEventHandler<(CocoaMQTT, CocoaMQTTConnAck) -> Void> {}
class MQTTDidDisconnectistener: MQTTDelegateEventHandler<(CocoaMQTT, Error?) -> Void> {}

class MQTTDidPublishMessageListener: MQTTDelegateEventHandler<(CocoaMQTT, CocoaMQTTMessage, UInt16) -> Void> {}
class MQTTDidPublishAckListener: MQTTDelegateEventHandler<(CocoaMQTT, UInt16) -> Void> {}

class MQTTDidSubscribeAckListener: MQTTDelegateEventHandler<(CocoaMQTT, NSDictionary, [String]) -> Void> {}
class MQTTDidUnsubscribeAckListener: MQTTDelegateEventHandler<(CocoaMQTT, [String]) -> Void> {}
class MQTTDidReceiveMessageListener: MQTTDelegateEventHandler<(CocoaMQTT, CocoaMQTTMessage, UInt16) -> Void> {}

class MQTTDidPingListener: MQTTDelegateEventHandler<(CocoaMQTT) -> Void> {}
class MQTTDidReceivePongListener: MQTTDelegateEventHandler<(CocoaMQTT) -> Void> {}

// MARK: The main class
class MQTTClient: CocoaMQTTDelegate {
    internal var mqtt: CocoaMQTT
    
    public var mqttDidConnectAckListener: MQTTDidConnectAckListener? = nil
    
    public var mqttDidPublishMessageListener: MQTTDidPublishMessageListener? = nil
    public var mqttDidPublishAckListener: MQTTDidPublishAckListener? = nil
    
    public var mqttDidSubscribeTopicsListener: MQTTDidSubscribeAckListener? = nil
    public var mqttDidUnsubscribeTopicsListener: MQTTDidUnsubscribeAckListener? = nil
    public var mqttDidReceiveMessageListener: MQTTDidReceiveMessageListener? = nil
    
    public var mqttDidPingListener: MQTTDidPingListener? = nil
    public var mqttDidReceivePongListener: MQTTDidReceivePongListener? = nil
    
    public var mqttDidDisconnectListener: MQTTDidDisconnectistener? = nil
    
    init(_ host: String, _ port: UInt16, _ clientId: String, _ username: String, _ password: String) {
        let websocket = CocoaMQTTWebSocket()
        self.mqtt = CocoaMQTT(clientID: clientId, host: host, port: port, socket: websocket)

        self.mqtt.username = username
        self.mqtt.password = password
        self.mqtt.willMessage = CocoaMQTTMessage(topic: "/will", string: "dieout")
        self.mqtt.keepAlive = 60
        self.mqtt.autoReconnect = true
        self.mqtt.delegate = self
    }
    
    func setClientId(_ clientId: String) {
        self.mqtt.clientID = clientId
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
    
    internal func mqtt(_ mqtt: CocoaMQTT, didConnectAck ack: CocoaMQTTConnAck) {
        print("didConnectAck")
        self.mqttDidConnectAckListener?.handler?(mqtt, ack)
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didPublishMessage message: CocoaMQTTMessage, id: UInt16) {
        print("didPublishMessage")
        self.mqttDidPublishMessageListener?.handler?(mqtt, message, id)
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didPublishAck id: UInt16) {
        print("didPublishAck")
        self.mqttDidPublishAckListener?.handler?(mqtt, id)
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {
        print("didSubscribeTopics: \(success), \(failed)")
        self.mqttDidSubscribeTopicsListener?.handler?(mqtt, success, failed)
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {
        print("didUnsubscribeTopics: \(topics)")
        self.mqttDidUnsubscribeTopicsListener?.handler?(mqtt, topics)
    }
    
    internal func mqtt(_ mqtt: CocoaMQTT, didReceiveMessage message: CocoaMQTTMessage, id: UInt16) {
        print("didReceiveMessage")
        self.mqttDidReceiveMessageListener?.handler?(mqtt, message, id)
    }
    
    internal func mqttDidPing(_ mqtt: CocoaMQTT) {
        print("mqttDidPing")
        self.mqttDidPingListener?.handler?(mqtt)
    }

    internal func mqttDidReceivePong(_ mqtt: CocoaMQTT) {
        print("mqttDidReceivePong")
        self.mqttDidReceivePongListener?.handler?(mqtt)
    }
    
    internal func mqttDidDisconnect(_ mqtt: CocoaMQTT, withError err: Error?) {
        print("mqttDidDisconnect: \(err?.localizedDescription ?? "")")
        self.mqttDidDisconnectListener?.handler?(mqtt, err)
    }
}

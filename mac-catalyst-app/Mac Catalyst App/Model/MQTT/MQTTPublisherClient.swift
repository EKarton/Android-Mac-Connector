//
//  MQTTPublisherClient.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-12.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

protocol MQTTPublisherClient {
    func publish(_ topic: String, _ message: String)
}

class MockMQTTPublisherClient: MQTTPublisherClient {
    func publish(_ topic: String, _ message: String) {}
}

class MQTTPublisherClientImpl: MQTTPublisherClient {
    private var client: MQTTClient
    
    init(_ client: MQTTClient) {
        self.client = client
    }
    
    func publish(_ topic: String, _ message: String) {
        print("Publishing \(message) to \(topic)")
        client.mqtt.publish(topic, withString: message, qos: .qos2, retained: true)
    }
}

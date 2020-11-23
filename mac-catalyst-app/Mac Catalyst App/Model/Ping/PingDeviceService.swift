//
//  PingDeviceService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class PingDeviceService: ObservableObject {
    private var mqttPublisher: MQTTPublisherClient
    
    init(_ mqttPublisher: MQTTPublisherClient) {
        self.mqttPublisher = mqttPublisher
    }
    
    func pingDevice(_ device: Device) {
        let publishTopic = "\(device.id)/ping/requests"
        self.mqttPublisher.publish(publishTopic, "")
    }
}

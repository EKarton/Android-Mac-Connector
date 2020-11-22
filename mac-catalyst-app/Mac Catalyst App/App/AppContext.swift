//
//  AppContext.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

class AppContext {
    
    // Authentication
    let sessionStore: SessionStore
    
    // Devices
    let deviceWebService: DeviceWebService
    let deviceRegistrationService: DeviceRegistrationService
    
    // MQTT
    let mqttClient: MQTTClient
    let mqttSubscriber: MQTTSubscriptionClient
    let mqttPublisher: MQTTPublisherClient
        
    // Ping-pong
    let pingDeviceService: PingDeviceService
    let receivedPingService: IncomingPingHandler
    
    init() {
        self.sessionStore = SessionStore()
        
        self.deviceWebService = DeviceWebServiceImpl()
        self.deviceRegistrationService = DeviceRegistrationService(sessionStore, deviceWebService)
        
        self.mqttClient = MQTTClient("192.168.0.102", 3000, "client", "username", "password")
        self.mqttSubscriber = MQTTSubscriptionClient(self.mqttClient)
        self.mqttPublisher = MQTTPublisherClient(self.mqttClient)
                
        self.pingDeviceService = PingDeviceService(mqttPublisher)
        self.receivedPingService = IncomingPingHandler()
    }
}

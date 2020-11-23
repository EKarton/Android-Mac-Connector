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
    let deviceRegistrationStore: DeviceRegistrationStore
    let devicesStore: DevicesStore
    
    // MQTT
    let mqttClient: MQTTClient
    let mqttSubscriber: MQTTSubscriptionClient
    let mqttPublisher: MQTTPublisherClient
    let mqttService: MQTTService
        
    // Ping-pong
    let pingDeviceService: PingDeviceService
    let receivedPingService: IncomingPingHandler
    
    init() {
        self.sessionStore = SessionStore()
        
        self.deviceWebService = DeviceWebServiceImpl()
        self.deviceRegistrationService = DeviceRegistrationService(sessionStore, deviceWebService)
        self.deviceRegistrationStore = DeviceRegistrationStore(sessionStore, deviceRegistrationService)
        self.devicesStore = DevicesStore(deviceWebService, sessionStore)
        
        self.receivedPingService = IncomingPingHandler()
        
        self.mqttClient = MQTTClient("192.168.0.102", 3000, "client", "username", "password")
        self.mqttSubscriber = MQTTSubscriptionClientImpl(self.mqttClient)
        self.mqttPublisher = MQTTPublisherClientImpl(self.mqttClient)
        
        self.pingDeviceService = PingDeviceService(mqttPublisher)
        
        self.mqttService = MQTTService(mqttClient, mqttSubscriber, mqttPublisher, sessionStore, deviceWebService, deviceRegistrationService, deviceRegistrationStore, receivedPingService)
    }
}

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
    
    // MQTT
    let mqttClient: MQTTClient
    let mqttSubscriber: MQTTSubscriptionClient
    let mqttPublisher: MQTTPublisherClient
    
    // SMS
    let smsSenderService: SmsSenderService
    let getSmsThreadsService: GetSmsThreadsService
    let getSmsMessageService: GetSmsMessageService
    let receivedSmsMessageService: IncomingSmsHandler
    
    // Ping-pong
    let pingDeviceService: PingDeviceService
    let receivedPingService: IncomingPingHandler
    
    init() {
        self.sessionStore = SessionStore()
        
        self.deviceWebService = DeviceWebServiceImpl()
        
        self.mqttClient = MQTTClient("192.168.0.102", 3000, "client", "username", "password")
        self.mqttSubscriber = MQTTSubscriptionClient(self.mqttClient)
        self.mqttPublisher = MQTTPublisherClient(self.mqttClient)
        
        self.smsSenderService = SmsSenderService(mqttSubscriber, mqttPublisher)
        self.getSmsThreadsService = GetSmsThreadsService(mqttSubscriber, mqttPublisher)
        self.getSmsMessageService = GetSmsMessageService(mqttSubscriber, mqttPublisher)
        self.receivedSmsMessageService = IncomingSmsHandler()
        
        self.pingDeviceService = PingDeviceService(mqttPublisher)
        self.receivedPingService = IncomingPingHandler()
    }
}

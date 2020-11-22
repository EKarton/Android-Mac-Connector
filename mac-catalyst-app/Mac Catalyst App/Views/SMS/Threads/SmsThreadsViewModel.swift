//
//  SmsThreadsViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class SmsThreadsViewModelFactory: ObservableObject {
    private var mqttSubscription: MQTTSubscriptionClient
    private var mqttPublisher: MQTTPublisherClient
    
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient) {
        self.mqttSubscription = mqttSubcription
        self.mqttPublisher = mqttPublisher
    }
    
    func createViewModel(_ device: Device) -> SmsThreadsViewModel {
        return SmsThreadsViewModel(mqttSubscription, mqttPublisher, device)
    }
}

class SmsThreadsViewModel: ObservableObject {
    @Published var error: Error? = nil
    @Published var threads = [SmsThread]()
    
    private var mqttSubscription: MQTTSubscriptionClient
    
    private let getSmsThreadsPublisher: GetSmsThreadsPublisher
    private let getSmsThreadsListener: GetSmsThreadsListener
        
    init(_ mqttSubcription: MQTTSubscriptionClient, _ mqttPublisher: MQTTPublisherClient, _ device: Device) {
        self.mqttSubscription = mqttSubcription
        
        self.getSmsThreadsPublisher = GetSmsThreadsPublisher(mqttPublisher, device.id)
        
        self.getSmsThreadsListener = GetSmsThreadsListener(device.id)
        self.getSmsThreadsListener.onMessageReceived = { payload in
            self.threads = payload.threads
        }
        self.getSmsThreadsListener.onErrorHandler = { err in
            self.error = err
        }
        
        mqttSubcription.addSubscriptionListener(getSmsThreadsListener)
    }
    
    func subscribeToSmsThreads(_ device: Device, handler: @escaping () -> Void) {
        self.mqttSubscription.subscribe("\(device.id)/sms/threads/query-results") { err in
            if let err = err {
                self.error = err
            }
            handler()
        }
    }
    
    func fetchThreads(_ device: Device, _ limit: Int, _ start: Int) {
        self.getSmsThreadsPublisher.publish(limit, start)
    }
    
    func unsubscribeToSmsThreads(_ device: Device) {
        self.mqttSubscription.unsubscribe("\(device.id)/sms/threads/query-results"){ err in
            if let err = err {
                self.error = err
            }
        }
    }
}

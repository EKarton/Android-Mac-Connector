//
//  MQTTClientAuthObserver.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-12.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

class MQTTAuthObserverClient: SessionChangedListener {
    
    private var client: MQTTClient
    
    init(_ client: MQTTClient) {
        self.client = client
    }
    
    func handler(_ oldSession: Session, _ newSession: Session) {
        print("MQTTAuthObserverClient(): Old: \(oldSession) vs \(newSession)")
        client.disconnect()
        client.setPassword(newSession.accessToken)
        
        if (newSession.isSignedIn) {
            client.connect()
        }
    }
}
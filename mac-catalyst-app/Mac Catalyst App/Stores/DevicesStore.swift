//
//  DeviceViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-17.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class DevicesStore: ObservableObject, SessionStoreObserver {
    
    enum Errors: Error {
        case NotAuthenticated
    }
    
    @Published var devices: [Device] = [Device]()
    
    private var webService: DeviceWebService
    private var sessionStore: SessionStore
    
    init(_ webService: DeviceWebService, _ sessionStore: SessionStore) {
        self.webService = webService
        self.sessionStore = sessionStore
        
        self.sessionStore.addObserver(self)
    }
    
    func fetchDevices(handler: @escaping (Error?) -> Void) {
        guard let authToken = self.sessionStore.currentSession?.accessToken else {
            handler(Errors.NotAuthenticated)
            return
        }
        
        self.webService.getDevices(authToken) { (devices, err) in
            if let err = err {
                handler(err)
                return
            }
            
            DispatchQueue.main.async {
                self.devices = devices
                handler(nil)
            }
        }
    }
    
    internal func onSessionChanged(_ oldSession: Session?, _ newSession: Session?) {
        if newSession == nil {
            self.fetchDevices() { err in }
        }
    }
}

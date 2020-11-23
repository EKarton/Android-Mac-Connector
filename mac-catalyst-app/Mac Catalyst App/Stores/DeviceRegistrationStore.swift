//
//  DeviceRegistrationStore.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-23.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import Foundation
import SwiftUI

protocol DeviceRegistrationStoreObserver: class {
    func onDeviceRegistrationChanged(_ oldDeviceId: String?, _ newDeviceId: String?)
}

class DeviceRegistrationStore: ObservableObject, SessionStoreObserver {
    @Published var deviceId: String? = nil
    @Published var isRegistered: Bool = false
    
    private var sessionStore: SessionStore
    private var deviceRegistrationService: DeviceRegistrationService
    private lazy var observers = [DeviceRegistrationStoreObserver]()
    
    init(_ sessionStore: SessionStore, _ deviceRegistrationService: DeviceRegistrationService) {
        self.sessionStore = sessionStore
        self.deviceRegistrationService = deviceRegistrationService
        
        self.sessionStore.addObserver(self)
    }
    
    func addObserver(_ observer: DeviceRegistrationStoreObserver) {
        observers.append(observer)
    }
    
    func removeObserver(_ observer: DeviceRegistrationStoreObserver) {
        if let idx = observers.firstIndex(where: { $0 === observer }) {
            observers.remove(at: idx)
        }
    }
    
    func checkIfCurrentDeviceIsRegistered(_ handler: @escaping (Error?) -> Void) {
        self.deviceRegistrationService.getDeviceId() { deviceId, err in
            if let err = err as? DeviceRegistrationServiceError, case .CannotGetDeviceIdError = err {
                DispatchQueue.main.async {
                    self.updateState(false, nil)
                    handler(nil)
                }
                
            } else if err != nil {
                handler(err)
                
            } else {
                DispatchQueue.main.async {
                    self.updateState(true, deviceId)
                    handler(nil)
                }
            }
        }
    }
    
    func registerDevice(handler: @escaping (Error?) -> Void) {
        self.deviceRegistrationService.registerDevice() { err in
            if let err = err {
                handler(err)
                return
            }
            
            self.deviceRegistrationService.getDeviceId() { deviceId, err in
                if let err = err {
                    handler(err)
                    return
                }
                
                DispatchQueue.main.async {
                    self.updateState(true, deviceId)
                    handler(nil)
                }
            }
        }
    }
    
    func unregisterDevice(handler: @escaping (Error?) -> Void) {
        self.deviceRegistrationService.unregisterDevice { err in
            if let err = err {
                handler(err)
                return
            }
            
            DispatchQueue.main.async {
                self.updateState(false, nil)
                handler(nil)
            }
        }
    }
    
    private func updateState(_ isRegistered: Bool, _ newDeviceId: String?) {
        if self.isRegistered == isRegistered && self.deviceId == newDeviceId {
            return
        }
        
        let oldDeviceId = self.deviceId
        
        self.isRegistered = isRegistered
        self.deviceId = newDeviceId
        
        self.notifyObservers(oldDeviceId, isRegistered ? newDeviceId : nil)
    }
    
    private func notifyObservers(_ oldDeviceId: String?, _ newDeviceId: String?) {
        self.observers.forEach { observer in
            observer.onDeviceRegistrationChanged(oldDeviceId, newDeviceId)
        }
    }
    
    internal func onSessionChanged(_ oldSession: Session?, _ newSession: Session?) {
        print("DeviceRegistrationStore: session changed")
        if newSession == nil {
            self.checkIfCurrentDeviceIsRegistered() { err in
                if let err = err {
                    print("DeviceRegistrationStore: Error when checking if device is registered: \(err.localizedDescription)")
                }
            }
        }
    }
}

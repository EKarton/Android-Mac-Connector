//
//  DeviceViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-17.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

//
// This class is used for views
//
class DeviceViewModel: ObservableObject {
    @Published var deviceId: String? = nil
    @Published var isRegistered: Bool = false
    @Published var devices: [Device] = [Device]()
    
    private var webService: DeviceWebService
    private var deviceRegistrationService: DeviceRegistrationService
    
    init(_ webService: DeviceWebService, _ deviceRegistrationService: DeviceRegistrationService) {
        self.webService = webService
        self.deviceRegistrationService = deviceRegistrationService
    }
    
    func fetchDevices(_ authToken: String, handler: @escaping (Error?) -> Void) {
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
    
    func checkIfCurrentDeviceIsRegistered(_ authToken: String, _ handler: @escaping (Error?) -> Void) {
        self.deviceRegistrationService.getDeviceId() { deviceId, err in
            DispatchQueue.main.async {
                self.isRegistered = err != nil
                self.deviceId = self.isRegistered ? deviceId : nil
                handler(err)
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
                    self.isRegistered = true
                    self.deviceId = deviceId
                    handler(nil)
                }
            }
        }
    }
    
    func unregisterDevice(_ authToken: String, handler: @escaping (Error?) -> Void) {
        self.deviceRegistrationService.unregisterDevice { err in
            DispatchQueue.main.async {
                self.isRegistered = false
                self.deviceId = nil
                handler(nil)
            }
        }
    }
}

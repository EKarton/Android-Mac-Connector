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
    
    init(_ webService: DeviceWebService) {
        self.webService = webService
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
        print("Checking if current device is registered")
        if let deviceId = UserDefaults.standard.string(forKey: "device_id") {
            print("Device is already registered via cache: \(deviceId)")
            self.deviceId = deviceId
            self.isRegistered = true
            handler(nil)
            return
        }
        
        let hardwareId = UIDevice.current.identifierForVendor!.uuidString
        self.webService.isDeviceRegistered(authToken, "macbook", hardwareId) { (isRegistered, deviceId, err) in            
            if let err = err {
                handler(err)
                return
            }
            
            DispatchQueue.main.async {
                self.isRegistered = isRegistered
                self.deviceId = isRegistered ? deviceId : nil
            }
            
            if isRegistered {
                UserDefaults.standard.set(deviceId, forKey: "device_id")
            }
            handler(nil)
        }
    }
    
    func registerDevice(_ authToken: String, handler: @escaping (Error?) -> Void) {
        let newDevice = RegisterDeviceRequest(
            deviceType: "macbook",
            hardwareId: UIDevice.current.identifierForVendor!.uuidString,
            capabilities: ["ping_device"]
        )
        
        self.webService.registerDevice(authToken, newDevice) { deviceId, err in
            if let err = err {
                handler(err)
                return
            }
            
            // Add it to the cache
            UserDefaults.standard.set(deviceId, forKey: "device_id")
            
            DispatchQueue.main.async {
                self.isRegistered = true
                self.deviceId = deviceId
                handler(nil)
            }
        }
    }
    
    func unregisterDevice(_ authToken: String, handler: @escaping (Error?) -> Void) {
        if let deviceId = self.deviceId {
            self.webService.removeDevice(authToken, deviceId) { err in
                if let err = err {
                    handler(err)
                    return
                }
                
                // Remove it from the cache
                UserDefaults.standard.removeObject(forKey: "device_id")
                
                DispatchQueue.main.async {
                    self.isRegistered = false
                    self.deviceId = nil
                    handler(nil)
                }
            }
            
        } else {
            handler(nil)
        }
    }
}

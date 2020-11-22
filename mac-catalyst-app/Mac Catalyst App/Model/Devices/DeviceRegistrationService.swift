//
//  DeviceRegistrationService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-21.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

enum DeviceRegistrationServiceError: Error {
    case NotLoggedInError
    case CannotGetDeviceIdError
}

/// This class is responsible for registering, unregistering, and obtaining the device ID with caching
class DeviceRegistrationService {
    private var sessionStore: SessionStore
    private var deviceWebService: DeviceWebService
    
    init(_ sessionStore: SessionStore, _ deviceWebService: DeviceWebService) {
        self.sessionStore = sessionStore
        self.deviceWebService = deviceWebService
    }
    
    /// Registers the device
    /// - Parameter handler: async callback for when registering the device is successful or not
    func registerDevice(handler: @escaping (Error?) -> Void) {
        let newDevice = RegisterDeviceRequest(
            deviceType: getDeviceType(),
            hardwareId: getHardwareId(),
            capabilities: ["ping_device"]
        )
        
        self.sessionStore.getAuthToken { authToken in
            guard let authToken = authToken else {
                handler(DeviceRegistrationServiceError.NotLoggedInError)
                return
            }
            
            self.deviceWebService.registerDevice(authToken, newDevice) { deviceId, err in
                if let err = err {
                    handler(err)
                    return
                }
                
                // Add it to the cache
                self.saveDeviceIdToCache(deviceId)
                handler(nil)
            }
        }
    }
    
    /// Unregisters a device
    /// - Parameter handler: async callback to when unregistering the device was successful or not
    func unregisterDevice(handler: @escaping (Error?) -> Void) {
        self.sessionStore.getAuthToken { authToken in
            guard let authToken = authToken else {
                handler(DeviceRegistrationServiceError.NotLoggedInError)
                return
            }
            
            self.getDeviceId { deviceId, err in
                if let err = err {
                    handler(err)
                    return
                }
                
                self.deviceWebService.removeDevice(authToken, deviceId) { err in
                    if let err = err {
                        handler(err)
                        return
                    }
                    
                    self.removeDeviceIdInCache()
                }
            }
        }
    }
    
    
    /// Gets the device id by first obtaining it from the cache. If it is not in the cache,
    /// then it will fetch it from the server
    ///
    /// If it cannot find the device id, it will pass an error to the handler and set the string to ""
    ///
    /// - Parameter handler: callback for when it finds the device id
    func getDeviceId(handler: @escaping (String, Error?) -> Void) {
        if let deviceId = self.getDeviceIdFromCache() {
            handler(deviceId, nil)
            return
        }
        
        let hardwareId = self.getHardwareId()
        let deviceType = self.getDeviceType()
        
        self.sessionStore.getAuthToken { authToken in
            guard let authToken = authToken else {
                handler("", DeviceRegistrationServiceError.NotLoggedInError)
                return
            }
        
            self.deviceWebService.isDeviceRegistered(authToken, deviceType, hardwareId) { (isRegistered, deviceId, err) in
                if let err = err {
                    handler("", err)
                    return
                }
                                
                if isRegistered {
                    self.saveDeviceIdToCache(deviceId)
                }
                handler(deviceId, nil)
            }
        }
    }
    
    private func getHardwareId() -> String {
        return UIDevice.current.identifierForVendor!.uuidString
    }
    
    private func getDeviceType() -> String {
        return "macbook"
    }
    
    private func saveDeviceIdToCache(_ deviceId: String) {
        UserDefaults.standard.set(deviceId, forKey: "device_id")
    }
    
    private func getDeviceIdFromCache() -> String? {
        return UserDefaults.standard.string(forKey: "device_id")
    }
    
    private func removeDeviceIdInCache() {
        UserDefaults.standard.removeObject(forKey: "device_id")
    }
}

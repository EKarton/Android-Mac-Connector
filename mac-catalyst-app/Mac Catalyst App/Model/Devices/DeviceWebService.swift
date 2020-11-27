//
//  DeviceWebService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-17.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

enum DeviceServiceError: Error {
    case UnexpectedStatusCode(statusCode: Int)
    case UnexpectedError(errorMsg: String)
    case UnexpectedError(error: Error)
}

struct Devices: Decodable {
    var devices: [Device]
}

struct IsDeviceRegistered2xxResponse: Decodable {
    var isRegistered: Bool
    var deviceId: String?
}

struct GetDevices2xxResponse: Decodable {
    var devices: [Device]
}

struct RegisterDeviceRequest: Encodable {
    var deviceType: String
    var hardwareId: String
    var name: String
    var capabilities: [String]
}

struct RegisterDevice2xxResponse: Decodable {
    var deviceId: String
}

struct DeleteDevice2xxResponse: Decodable {
    var status: String
}

protocol DeviceWebService {
    func isDeviceRegistered(
        _ authToken: String,
        _ deviceType: String,
        _ hardwareId: String,
        _ handler: @escaping (Bool, String, DeviceServiceError?) -> Void
    )
    
    func getDevices(
        _ accessToken: String,
        _ handler: @escaping ([Device], DeviceServiceError?) -> Void
    )
    
    func registerDevice(
        _ authToken: String,
        _ newDevice: RegisterDeviceRequest,
        _ handler: @escaping (String, DeviceServiceError?) -> Void
    )
    
    func removeDevice(
        _ authToken: String,
        _ deviceId: String,
        _ handler: @escaping (DeviceServiceError?) -> Void
    )
}

class MockedDeviceWebService: DeviceWebService {
    func isDeviceRegistered(
        _ authToken: String,
        _ deviceType: String,
        _ hardwareId: String,
        _ handler: @escaping (Bool, String, DeviceServiceError?) -> Void
    ) {
        handler(false, "", nil)
    }
    
    func getDevices(
        _ accessToken: String,
        _ handler: @escaping ([Device], DeviceServiceError?) -> Void
    ) {
        let devices = [
            Device(id: "1", type: "android_phone", name: "Galaxy S9", capabilities: ["read_sms", "send_sms", "receive_sms"], phoneNumber: "123-456-7890"),
            Device(id: "2", type: "android_phone", name: "Nexus 2", capabilities: ["read_sms", "send_sms", "receive_sms"], phoneNumber: "098-765-4321")
        ]
        
        handler(devices, nil)
    }
    
    func registerDevice(
        _ authToken: String,
        _ newDevice: RegisterDeviceRequest,
        _ handler: @escaping (String, DeviceServiceError?) -> Void
    ) {
        handler("DeviceId1234567890", nil)
    }
    
    func removeDevice(
        _ authToken: String,
        _ deviceId: String,
        _ handler: @escaping (DeviceServiceError?) -> Void
    ) {
        handler(nil)
    }
}

//
// This class is a stateless service used to interface with the rest api
//
class DeviceWebServiceImpl: DeviceWebService {
    private let isDeviceRegisteredUrl = "http://192.168.0.102:3000/api/v1/devices/registered"
    private let getDevicesUrl = "http://192.168.0.102:3000/api/v1/devices"
    private let registerDeviceUrl = "http://192.168.0.102:3000/api/v1/devices/register"
    private let removeDeviceUrl = "http://192.168.0.102:3000/api/v1/devices/%@"
    
    private var jsonEncoder = JSONEncoder()
    private var jsonDecoder = JSONDecoder()
    
    init() {
        self.jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
        self.jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
    }
    
    func isDeviceRegistered(
        _ authToken: String,
        _ deviceType: String,
        _ hardwareId: String,
        _ handler: @escaping (Bool, String, DeviceServiceError?) -> Void
    ) {
        guard var urlComponents = URLComponents(string: isDeviceRegisteredUrl) else {
            handler(false, "", .UnexpectedError(errorMsg: "Invalid url"))
            return
        }
        
        // Add query params to the url
        urlComponents.queryItems = [
            URLQueryItem(name: "device_type", value: deviceType),
            URLQueryItem(name: "hardware_id", value: hardwareId)
        ]
        
        guard let url = urlComponents.url else {
            handler(false, "", .UnexpectedError(errorMsg: "Invalid url"))
            return
        }
        
        var request = URLRequest(url: url)
        request.setValue("Bearer " + authToken, forHTTPHeaderField: "Authorization")
        
        let task = URLSession.shared.dataTask(with: request, completionHandler: { (data, response, error) in
            let data = self.handleResponse(IsDeviceRegistered2xxResponse.self, data, response, error)
            handler(data.json?.isRegistered ?? false, data.json?.deviceId ?? "", data.err)
        })
        
        task.resume()
    }
    
    func getDevices(
        _ accessToken: String,
        _ handler: @escaping ([Device],
        DeviceServiceError?) -> Void
    ) {
        guard let url = URL(string: getDevicesUrl) else {
            handler([Device](), .UnexpectedError(errorMsg: "Invalid url"))
            return
        }
        
        var request = URLRequest(url: url)
        request.setValue("Bearer " + accessToken, forHTTPHeaderField: "Authorization")
                        
        let task = URLSession.shared.dataTask(with: request, completionHandler: { (data, response, error) in
            let data = self.handleResponse(GetDevices2xxResponse.self, data, response, error)
            handler(data.json?.devices ?? [Device](), data.err)
        })
        
        task.resume()
    }
    
    func registerDevice(
        _ authToken: String,
        _ newDevice: RegisterDeviceRequest,
        _ handler: @escaping (String, DeviceServiceError?) -> Void
    ) {
        print("Registering device \(newDevice.deviceType) with hardware \(newDevice.hardwareId)")
        
        guard let url = URL(string: registerDeviceUrl) else {
            handler("", .UnexpectedError(errorMsg: "Invalid url"))
            return
        }
        
        guard let jsonData = try? self.jsonEncoder.encode(newDevice) else {
            handler("", .UnexpectedError(errorMsg: "Failed to convert struct to json string"))
            return
        }
        
        print(jsonData)
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = jsonData
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer " + authToken, forHTTPHeaderField: "Authorization")
                        
        URLSession.shared.dataTask(with: request, completionHandler: { (data, response, error) in
            let data = self.handleResponse(RegisterDevice2xxResponse.self, data, response, error)
            handler(data.json?.deviceId ?? "", data.err)
            
        }).resume()
    }
    
    func removeDevice(
        _ authToken: String,
        _ deviceId: String,
        _ handler: @escaping (DeviceServiceError?) -> Void
    ) {
        print("Removing device \(deviceId)")
        
        let apiPath = String(format: removeDeviceUrl, deviceId)
        print(apiPath)
        guard let url = URL(string: apiPath) else {
            handler(.UnexpectedError(errorMsg: "Invalid url"))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.setValue("Bearer " + authToken, forHTTPHeaderField: "Authorization")
        
        let task = URLSession.shared.dataTask(with: request, completionHandler: { (data, response, error) in
            let data = self.handleResponse(DeleteDevice2xxResponse.self, data, response, error)
            if let err = data.err {
                handler(err)
                return
            }
            
            if data.json?.status != "success" {
                handler(.UnexpectedError(errorMsg: "Failed to delete device"))
                return
            }
            
            handler(nil)
        })
        
        task.resume()
    }
    
    private func handleResponse<T>(
        _ type: T.Type,
        _ data: Data?,
        _ response: URLResponse?,
        _ err: Error?
    ) -> (json: T?, err: DeviceServiceError?) where T : Decodable {
        
        if let err = err {
            return (nil, .UnexpectedError(error: err))
        }
        
        guard let httpResponse = response as? HTTPURLResponse else {
            return (nil, .UnexpectedError(errorMsg: "Cannot get http response"))
        }
        
        guard (200...299).contains(httpResponse.statusCode) else {
            return (nil, .UnexpectedStatusCode(statusCode: httpResponse.statusCode))
        }
        
        guard let data = data else {
            return (nil, .UnexpectedError(errorMsg: "Cannot get data"))
        }
        
        guard let json = try? self.jsonDecoder.decode(T.self, from: data) else {
            return (nil, .UnexpectedError(errorMsg: "Cannot parse JSON property"))
        }
        
        return (json, nil)
    }
}

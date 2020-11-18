//
//  DeviceService.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
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

class DeviceService: ObservableObject {
    func getDevices(_ accessToken: String, _ handler: @escaping ([Device], Error?) -> Void) {
        print("Getting devices")
        guard let url = URL(string: "http://192.168.0.102:3000/api/v1/devices") else {
            print("Invalid url")
            return
        }
        
        var request = URLRequest(url: url)
        request.setValue("Bearer " + accessToken, forHTTPHeaderField: "Authorization")
                        
        let task = URLSession.shared.dataTask(with: request, completionHandler: { (data, response, error) in
            if let error = error {
                print("Error getting device: \(error)")
                handler(Array<Device>(), error)
                return
            }
            
            guard let httpResponse = response as? HTTPURLResponse else {
                handler(Array<Device>(), DeviceServiceError.UnexpectedError(errorMsg: "Cannot get http response"))
                return
            }
            
            guard (200...299).contains(httpResponse.statusCode) else {
                print("Status code is not 2xx: \(httpResponse.statusCode)")
                handler(Array<Device>(), DeviceServiceError.UnexpectedStatusCode(statusCode: httpResponse.statusCode))
                return
            }
            
            guard let data = data else {
                print("Cannot get data")
                handler(Array<Device>(), DeviceServiceError.UnexpectedError(errorMsg: "Cannot get data"))
                return
            }
            
            guard let devices = try? JSONDecoder().decode(Devices.self, from: data) else {
                print("Cannot parse JSON")
                handler(Array<Device>(), DeviceServiceError.UnexpectedError(errorMsg: "Cannot parse JSON property"))
                return
            }
            
            handler(devices.devices, nil)
        })
        
        task.resume()
    }
}

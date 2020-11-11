//
//  Device.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

struct Device: Identifiable, Decodable {
    var id: String
    var type: String
    var name: String
    var capabilities: [String]
    var phoneNumber: String?
    
    var hasSmsCapability: Bool {
        if (capabilities.contains("read_sms")) {
            return true
        }

        if (capabilities.contains("receive_sms")) {
            return true
        }

        if (capabilities.contains("send_sms")) {
            return true
        }
        return false
    }
}

#if DEBUG
let devicesList = [
    Device(id: "1", type: "Android", name: "Galaxy s9+", capabilities: ["read_sms", "send_sms", "receive_sms"], phoneNumber: "647-607-6358"),
    Device(id: "2", type: "Android", name: "OnePlus 4", capabilities: ["read_sms", "send_sms", "receive_sms"], phoneNumber: "647-607-6358")
]
#endif

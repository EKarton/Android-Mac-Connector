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
    
    var hasPingDeviceCapability: Bool {
        if (capabilities.contains("ping_device")) {
            return true
        }
        return false
    }
    
    var hasReadSmsCapability: Bool {
        return capabilities.contains("read_sms")
    }
    
    var hasSendSmsCapability: Bool {
        return capabilities.contains("send_sms")
    }
    
    var hasReceiveSmsCapability: Bool {
        return capabilities.contains("receive_sms")
    }
    
    var hasSmsCapability: Bool {
        return hasReadSmsCapability || hasSendSmsCapability || hasReceiveSmsCapability
    }
}

#if DEBUG
let devicesList = [
    Device(id: "1", type: "Android", name: "Galaxy s9+", capabilities: ["read_sms", "send_sms", "receive_sms"], phoneNumber: "647-607-6358"),
    Device(id: "2", type: "Android", name: "OnePlus 4", capabilities: ["read_sms", "send_sms", "receive_sms"], phoneNumber: "647-607-6358")
]
#endif
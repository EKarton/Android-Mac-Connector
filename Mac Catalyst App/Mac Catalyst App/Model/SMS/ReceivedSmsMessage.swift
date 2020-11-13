//
//  ReceivedSmsMessage.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct ReceivedSmsMessage: Codable {
    var phoneNumber: String
    var body: String
    var timestamp: Int
    
    static func fromJson(_ json: String) -> ReceivedSmsMessage? {
        let jsonDecoder = JSONDecoder()
        jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
        
        guard let jsonData = json.data(using: .utf8) else {
            return nil
        }
                    
        guard let newStruct = try? jsonDecoder.decode(ReceivedSmsMessage.self, from: jsonData) else {
            return nil
        }
        
        return newStruct
    }
    
    static func toJson(msg: ReceivedSmsMessage) -> String? {
        let jsonEncoder = JSONEncoder()
        jsonEncoder.keyEncodingStrategy = .convertToSnakeCase
        
        guard let jsonData = try? jsonEncoder.encode(msg) else {
            return nil
        }
        
        return String(data: jsonData, encoding: .utf8)
    }
}

#if DEBUG
let receivedSmsMessages = [
    ReceivedSmsMessage(phoneNumber: "647-607-6358", body: "Hi there", timestamp: 10),
    ReceivedSmsMessage(phoneNumber: "647-607-6358", body: "Hi there again", timestamp: 11)
]
#endif

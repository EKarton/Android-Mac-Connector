//
//  SmsThread.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsThread: Codable {
    var threadId: String
    var phoneNumber: String
    var contactName: String?
    var numUnreadMessages: Int
    var numMessages: Int
    var lastMessageSent: String
    var timeLastMessageSent: Int
    
    var formattedTimeLastMessageSent: String {
        let epocTime = TimeInterval(timeLastMessageSent)
        let myDate = Date(timeIntervalSince1970: epocTime)
        
        // Check if it is within 24 hours, and if so return in format 9:00 am
        if (myDate.timeIntervalSinceNow > -86400) {
            let timeFormatter = DateFormatter()
            timeFormatter.locale = Locale(identifier: "en_US")
            timeFormatter.dateFormat = "h:ss a"

            return timeFormatter.string(from: myDate)
        }
        
        // Check if it is within the current year, and if so return the date
        guard let msgYear = Calendar.current.dateComponents([.year], from: myDate).year else {
            return "?"
        }
        
        guard let curYear = Calendar.current.dateComponents([.year], from: Date()).year else {
            return "?"
        }
    
        if msgYear == curYear {
            let dateFormatter = DateFormatter()
            dateFormatter.locale = Locale(identifier: "en_US")
            dateFormatter.dateFormat = "MMM d"
            
            return dateFormatter.string(from: myDate)
        }
        
        // If not, return the year
        return String(msgYear)
    }
}

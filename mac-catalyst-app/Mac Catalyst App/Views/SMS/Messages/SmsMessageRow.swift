//
//  SmsMessageRow.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-11.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsMessageRow: View {
    var isCurrentUser: Bool
    var message: String
    
    var body: some View {
        HStack {
            if isCurrentUser {
                Spacer()
                SmsMessageBubble(isCurrentUser: isCurrentUser, message: message)
                
            } else {
                SmsMessageBubble(isCurrentUser: isCurrentUser, message: message)
                Spacer()
            }
        }
    }
}

struct SmsMessageRow_Previews: PreviewProvider {
    static var previews: some View {
        SmsMessageRow(isCurrentUser: true, message: "Hi")
    }
}
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
    var isInFlight: Bool
    
    var body: some View {
        HStack {
            if isCurrentUser {
                Spacer()
                VStack(alignment: .trailing, spacing: 10) {
                    SmsMessageBubble(isCurrentUser: isCurrentUser, message: message)
                    if isInFlight {
                        Text("Sending")
                    }
                }
                
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    SmsMessageBubble(isCurrentUser: isCurrentUser, message: message)
                    if isInFlight {
                        Text("Sending")
                    }
                }
                Spacer()
            }
        }
    }
}

struct SmsMessageRow_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            SmsMessageRow(isCurrentUser: false, message: "Hi", isInFlight: false)
                .previewLayout(.fixed(width: 400, height: 100))
            
            SmsMessageRow(isCurrentUser: true, message: "Hi", isInFlight: false)
                .previewLayout(.fixed(width: 400, height: 100))
        
            SmsMessageRow(isCurrentUser: true, message: "Hi", isInFlight: true)
                .previewLayout(.fixed(width: 400, height: 100))
        }
    }
}

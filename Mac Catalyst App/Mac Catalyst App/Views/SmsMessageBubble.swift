//
//  SmsMessageBubble.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsMessageBubble: View {
    var isCurrentUser: Bool
    var message: String
    
    var body: some View {
        Text(message)
            .padding(10)
            .foregroundColor(foregroundColor)
            .background(backgroundColor)
            .cornerRadius(10)
    }
    
    var foregroundColor: Color {
        if (isCurrentUser) {
            return Color.white
        }
        return Color.black
    }
    
    var backgroundColor: Color {
        return isCurrentUser ? Color.blue : Color(UIColor(red: 240/255, green: 240/255, blue: 240/255, alpha: 1.0))
    }
}

#if DEBUG
struct SmsMessageBubble_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            SmsMessageBubble(isCurrentUser: false, message: "Hi there!").previewLayout(.fixed(width: 300, height: 100))
            
            SmsMessageBubble(isCurrentUser: true, message: "Hey! What's up?").previewLayout(.fixed(width: 300, height: 100))
        }
    }
}
#endif

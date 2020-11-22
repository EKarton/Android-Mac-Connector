//
//  SmsThreadsRow.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsThreadsRow: View {
    var image: Image
    var name: String
    var lastMessage: String
    var timeLastMessageSent: String
    
    var body: some View {
        VStack {
            HStack {
                image.resizable()
                    .frame(width: 40, height: 40)
                VStack(alignment: .leading) {
                    Text(self.name)
                        .padding(.bottom, 8)
                        .font(.headline)
                        .truncationMode(.tail)
                        .lineLimit(1)
                    HStack {
                        Text(self.lastMessage)
                            .font(.subheadline)
                            .truncationMode(.tail)
                            .lineLimit(1)
                        Spacer()
                        Text(self.timeLastMessageSent).font(.caption)
                    }
                }
                Spacer()
            }
            .padding(.all, 4)
        }
    }
}

#if DEBUG
struct SmsThreadRowPreviews: PreviewProvider {
    static var previews: some View {
        Group {
            SmsThreadsRow(
                image: Image(systemName: "person.fill"),
                name: "Bob Smith",
                lastMessage: "How are you?",
                timeLastMessageSent: "Now"
            ).previewLayout(.fixed(width: 300, height: 100))
            
            SmsThreadsRow(
                image: Image(systemName: "person.fill"),
                name: "ThisIsAVeryLongContactNameForThisThread",
                lastMessage: "How are you?",
                timeLastMessageSent: "Thursday"
            ).previewLayout(.fixed(width: 300, height: 100))
            
            SmsThreadsRow(
                image: Image(systemName: "person.fill"),
                name: "Bob Smith",
                lastMessage: "ThisIsAVeryLongMessageBody",
                timeLastMessageSent: "9:45 am"
            ).previewLayout(.fixed(width: 300, height: 100))
        }
    }
}
#endif

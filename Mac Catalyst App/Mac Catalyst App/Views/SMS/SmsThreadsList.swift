//
//  SmsThreadsList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsThreadsList: View {
    @EnvironmentObject var smsReader: SmsReaderService
    
    var device: Device
    
    @State private var smsThreads = [SmsThread]()
    @State private var showingNewSmsMessageSheet = false
    
    var body: some View {
        VStack {
            List(smsThreads, id: \.threadId) { (thread: SmsThread) in
                NavigationLink(
                    destination: SmsMessagesView(
                        device: self.device,
                        threadId: thread.threadId,
                        contactName: thread.contactName ?? thread.phoneNumber
                    )
                ) {
                    SmsThreadsRow(
                        image: Image(systemName: "cloud.heavyrain.fill"),
                        name: thread.contactName ?? thread.phoneNumber,
                        lastMessage: thread.lastMessageSent,
                        timeLastMessageSent: self.formatReadableTime(thread.timeLastMessageSent)
                    )
                }
            }
        }
        .onAppear(perform: self.onAppearHandler)
        .sheet(isPresented: $showingNewSmsMessageSheet) {
            NewSmsMessageView()
        }
        .navigationBarTitle("SMS via \(device.name)", displayMode: .inline)
        .navigationBarItems(trailing:
            Button(action: {
                self.showingNewSmsMessageSheet = true
            }) {
                Image(systemName: "square.and.pencil")
            }
        )
    }
    
    private func onAppearHandler() {
        self.smsReader.fetchSmsThreads(self.device, 10000, 0) { (smsThreads, error) in
            guard error == nil else {
                print("Error encountered: \(error.debugDescription)")
                return
            }
            
            self.smsThreads = smsThreads
        }
    }
    
    private func formatReadableTime(_ unixTime: Int) -> String {
        let epocTime = TimeInterval(unixTime)
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

#if DEBUG
struct SmsView_Previews: PreviewProvider {
    static var previews: some View {
        SmsThreadsList(device: devicesList[0])
    }
}
#endif

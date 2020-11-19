//
//  SmsThreadsList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsThreadsView: View {
    @EnvironmentObject var viewModel: SmsThreadsViewModel
    
    var device: Device
    @State private var showingNewSmsMessageSheet = false
    
    var body: some View {
        VStack {
            List(self.viewModel.threads, id: \.threadId) { (thread: SmsThread) in
                NavigationLink(destination: SmsMessagesView(
                        device: self.device,
                        threadId: thread.threadId,
                        contactName: thread.contactName ?? thread.phoneNumber,
                        phoneNumber: thread.phoneNumber
                )) {
                    SmsThreadsRow(
                        image: Image(systemName: "cloud.heavyrain.fill"),
                        name: thread.contactName ?? thread.phoneNumber,
                        lastMessage: thread.lastMessageSent,
                        timeLastMessageSent: thread.formattedTimeLastMessageSent
                    )
                }
            }
            .id(UUID())
        }
        .onAppear(perform: self.onAppear)
        .sheet(isPresented: $showingNewSmsMessageSheet) {
            NewSmsMessageView()
        }
        .navigationBarTitle("SMS via \(device.name)", displayMode: .inline)
        .navigationBarItems(trailing:
            Button(action: self.onCreateNewMessageButtonClicked) {
                Image(systemName: "square.and.pencil")
            }
        )
    }
    
    private func onAppear() {
        self.viewModel.fetchThreads(self.device, 100000, 0) { err in
            if let err = err {
                print("Error when refreshing threads: \(err.localizedDescription)")
            }
        }
    }
    
    private func onCreateNewMessageButtonClicked() {
        self.showingNewSmsMessageSheet = true
    }
}

#if DEBUG
struct SmsView_Previews: PreviewProvider {
    static var previews: some View {
        SmsThreadsView(device: devicesList[0])
    }
}
#endif

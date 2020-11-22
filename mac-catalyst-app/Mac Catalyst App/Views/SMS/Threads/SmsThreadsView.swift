//
//  SmsThreadsList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsThreadsView: View {
    @EnvironmentObject var viewModelFactory: SmsMessageViewModelFactory
    @State private var showingNewSmsMessageSheet = false
    
    @ObservedObject var viewModel: SmsThreadsViewModel
    var device: Device
            
    var body: some View {
        VStack {
            List(self.viewModel.threads, id: \.threadId) { (thread: SmsThread) in
                NavigationLink(destination: NavigationLazyView(SmsMessagesView(
                        device: self.device,
                        threadId: thread.threadId,
                        contactName: thread.contactName ?? thread.phoneNumber,
                        phoneNumber: thread.phoneNumber,
                        viewModelFactory: self.viewModelFactory
                ))) {
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
        .onDisappear(perform: self.onDisappear)
        .navigationBarTitle("SMS via \(device.name)", displayMode: .inline)
        .navigationBarItems(trailing:
            HStack {
                Button(action: self.onRefreshButtonClicked) {
                    Image(systemName: "arrow.clockwise")
                }
                Button(action: self.onCreateNewMessageButtonClicked) {
                    Image(systemName: "square.and.pencil")
                }
            }
        )
        .sheet(isPresented: $showingNewSmsMessageSheet) {
            NewSmsMessageView()
        }
    }
    
    private func onAppear() {
        self.viewModel.subscribeToSmsThreads(device) {
            self.viewModel.fetchThreads(self.device, 10000, 0)
        }
    }
    
    private func onDisappear() {
        self.viewModel.unsubscribeToSmsThreads(device)
    }
    
    private func onRefreshButtonClicked() {
        self.viewModel.fetchThreads(self.device, 10000, 0)
    }
    
    private func onCreateNewMessageButtonClicked() {
        self.showingNewSmsMessageSheet = true
    }
}

#if DEBUG
struct SmsView_Previews: PreviewProvider {
    static var previews: some View {
        Text("Hello world")
//        SmsThreadsView(device: devicesList[0], )
    }
}
#endif

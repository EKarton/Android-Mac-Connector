//
//  SmsMessagesView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsMessagesView: View {
    var device: Device
    var threadId: String
    var contactName: String
    var phoneNumber: String
    
    @ObservedObject private var viewModel: SmsMessageViewModel
    @State private var messageToSend: String = ""
        
    init(device: Device, threadId: String, contactName: String, phoneNumber: String, viewModelFactory: SmsMessageViewModelFactory) {
        self.device = device
        self.threadId = threadId
        self.contactName = contactName
        self.phoneNumber = phoneNumber
        self.viewModel = viewModelFactory.createViewModel(device, threadId, phoneNumber)
        
        // Remove separator lines from List
        UITableView.appearance().separatorStyle = .none
        UITableView.appearance().tableFooterView = UIView()
    }
    
    var body: some View {
        VStack {
            VStack {
                List {
                    // Note: the contents here are in reversed order
                    ForEach(self.viewModel.messagesInFlight, id: \.self) { (msgInFlight: SmsMessagesInFlight) in
                        SmsMessageRow(
                            isCurrentUser: true,
                            message: msgInFlight.body,
                            isInFlight: true
                        )
                    }
                    .scaleEffect(x: 1, y: -1, anchor: .center)
                    
                    ForEach(self.viewModel.messages, id: \.messageId) { (message: SmsMessage) in
                        SmsMessageRow(
                            isCurrentUser: message.isCurrentUser,
                            message: message.body,
                            isInFlight: false
                        )
                    }
                    .scaleEffect(x: 1, y: -1, anchor: .center)
                }
                .scaleEffect(x: 1, y: -1, anchor: .center)
                .padding(.leading, 20)
                .padding(.trailing, 20)
                .padding(.top, 10)
                .id(UUID())
            }
            
            VStack {
                HStack {
                    TextField(
                        "Insert message",
                        text: self.$messageToSend,
                        onCommit: self.onSendSmsButtonClickHandler
                    )
                    Button(action: self.onSendSmsButtonClickHandler) {
                        Image(systemName: "paperplane.fill")
                    }
                }
            }
            .padding()
        }
        .navigationBarTitle(Text(self.contactName), displayMode: .inline)
        .navigationBarItems(trailing:
            Button(action: self.refreshMessages) {
                Image(systemName: "arrow.clockwise")
            }
        )
        .onAppear(perform: self.onAppear)
        .onDisappear(perform: self.onDisappear)
    }
    
    func onAppear() {
        self.viewModel.subscribeToSmsMessages() {
            self.viewModel.fetchMessages()
        }
        self.viewModel.subscribeToSentSmsResults {}
    }
    
    func onDisappear() {
        self.viewModel.unsubscribeToSmsMessages() {}
        self.viewModel.unsubscribeFromSentSmsResults {}
    }
    
    func refreshMessages() {
        self.viewModel.fetchMessages()
    }
    
    func onSendSmsButtonClickHandler() {
        viewModel.sendMessage(messageToSend) { err in
            if let err = err {
                print("Error when sending message: \(err)")
                return
            }
        }
        self.messageToSend = ""
    }
}


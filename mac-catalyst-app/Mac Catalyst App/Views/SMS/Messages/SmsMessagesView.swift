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
    
    @EnvironmentObject var viewModel: SmsMessageViewModel
    @State private var messageToSend: String = ""
        
    init(device: Device, threadId: String, contactName: String, phoneNumber: String) {
        self.device = device
        self.threadId = threadId
        self.contactName = contactName
        self.phoneNumber = phoneNumber
        
        // Remove separator lines from List
        UITableView.appearance().separatorStyle = .none
        UITableView.appearance().tableFooterView = UIView()
    }
    
    var body: some View {
        VStack {
            VStack {
                List {
                    // Note: the contents here are in reversed order
                    ForEach(self.viewModel.messagesInFlight, id: \.self) { (sendingMsg: String) in
                        SmsMessageRow(
                            isCurrentUser: true,
                            message: sendingMsg
                        )
                    }
                    .scaleEffect(x: 1, y: -1, anchor: .center)
                    
                    ForEach(self.viewModel.messages, id: \.messageId) { (message: SmsMessage) in
                        SmsMessageRow(
                            isCurrentUser: message.isCurrentUser,
                            message: message.body
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
        .onAppear(perform: self.refreshMessages)
    }
    
    func onSendSmsButtonClickHandler() {
        viewModel.sendMessage(device, phoneNumber, messageToSend) { err in
            if let err = err {
                print("Error when sending message: \(err)")
                return
            }
        }
        self.messageToSend = ""
    }
    
    func refreshMessages() {
        viewModel.fetchMessages(device, threadId, 10000, 0) { err in
            if let err = err {
                print("Error while fetching messages: \(err)")
            }
        }
    }
}

#if DEBUG
struct SmsMessagesView_Previews: PreviewProvider {
    static var previews: some View {
        SmsMessagesView(device: devicesList[0], threadId: "1", contactName: "Bob Smith", phoneNumber: "123-456-7890")
    }
}
#endif

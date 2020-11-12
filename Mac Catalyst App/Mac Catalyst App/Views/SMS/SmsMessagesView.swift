//
//  SmsMessagesView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsMessagesView: View {
    @EnvironmentObject var smsMessageService: GetSmsMessageService
    @EnvironmentObject var smsSender: SmsSenderService
    
    var device: Device
    var threadId: String
    var contactName: String
    var phoneNumber: String
    
    @State private var messages = [SmsMessage]()
    @State private var sendingMessages = [String]()
    @State private var messageToSend: String = ""
    @State private var isLoadingMessages = false
    
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
                    ForEach(self.sendingMessages, id: \.self) { (sendingMsg: String) in
                        SmsMessageRow(
                            isCurrentUser: true,
                            message: sendingMsg
                        )
                    }
                    .scaleEffect(x: 1, y: -1, anchor: .center)
                    
                    ForEach(self.messages, id: \.messageId) { (message: SmsMessage) in
                        SmsMessageRow(
                            isCurrentUser: message.phoneNumber == self.device.phoneNumber,
                            message: message.body
                        )
                    }
                    .scaleEffect(x: 1, y: -1, anchor: .center)
                }
                .scaleEffect(x: 1, y: -1, anchor: .center)
                .padding(.leading, 20)
                .padding(.trailing, 20)
                .padding(.top, 10)
            }
            
            VStack {
                HStack {
                    TextField("Insert message",
                              text: self.$messageToSend,
                        onCommit: { self.onSendSmsButtonClickHandler() }
                    )
                    Button(action: { self.onSendSmsButtonClickHandler() }) {
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
        self.smsSender.sendSms(self.device, self.phoneNumber, self.messageToSend) { (error: Error?) in
            if let error = error {
                print("Error when sending message: \(error)")
                return
            }
            
            print("Successfully sent sms message")
            self.refreshMessages()
        }
        self.sendingMessages.append(self.messageToSend)
        self.messageToSend = ""
    }
    
    func refreshMessages() {
        if self.isLoadingMessages {
            return
        }
        
        self.isLoadingMessages = true
        
        self.smsMessageService.fetchSmsMessages(self.device, self.threadId, 10000, 0) { (smsMessages: [SmsMessage], error: Error?) in
            
            self.isLoadingMessages = false
            
            if let error = error {
                print("Encountered error when refreshing messages: \(error.localizedDescription)")
                return
            }
            
            var i = 0
            while (i < self.sendingMessages.count){
                for msg in smsMessages {
                    if self.sendingMessages.contains(msg.body) {
                        self.sendingMessages.remove(at: i)
                        i -= 1
                    }
                }
                i += 1
            }
            
            self.messages = smsMessages
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

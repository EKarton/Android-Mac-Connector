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
    
    @State var messages = [SmsMessage]()
    @State private var messageToSend: String = ""
    @State var isLoadingMessages = false
    
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
                    ForEach(self.messages, id: \.messageId) { (message: SmsMessage) in
                        HStack {
                            if (message.phoneNumber == self.device.phoneNumber) {
                                Spacer()
                                SmsMessageBubble(isCurrentUser: true, message: message.body)
                                
                            } else {
                                SmsMessageBubble(isCurrentUser: false, message: message.body)
                                Spacer()
                            }
                        }
                        
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

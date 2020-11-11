//
//  SmsMessagesView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SmsMessagesView: View {
    @EnvironmentObject var smsReader: SmsReaderService
    @EnvironmentObject var smsSender: SmsSenderService
    
    var device: Device
    var threadId: String
    var contactName: String
    
    @State var messages = [SmsMessage]()
    @State private var messageToSend: String = ""
    
    var body: some View {
        VStack {
            Spacer()
            VStack {
                ForEach(self.messages, id: \.messageId) { (message: SmsMessage) in
                    HStack {
                        if (message.address == self.device.phoneNumber) {
                            Spacer()
                            SmsMessageBubble(isCurrentUser: true, message: message.body)
                            
                        } else {
                            SmsMessageBubble(isCurrentUser: false, message: message.body)
                            Spacer()
                        }
                    }
                }
                .padding(.leading, 20)
                .padding(.trailing, 20)
                .padding(.top, 10)
            }
            
            VStack {
                HStack {
                    TextField("Insert message",
                        text: $messageToSend,
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
        .onAppear {
            self.smsReader.fetchSmsMessages(self.device, self.threadId) { (smsMessages: [SmsMessage]) in
                self.messages = smsMessages
            }
        }
    }
    
    func onSendSmsButtonClickHandler() {
        self.smsSender.sendSms(self.device, "444-232-2323", self.messageToSend) { (error: Error?) in
            self.messageToSend = ""
            self.refreshMessages()
        }
    }
    
    func refreshMessages() {
        self.smsReader.fetchSmsMessages(self.device, self.threadId) { (smsMessages: [SmsMessage]) in
            self.messages = smsMessages
        }
    }
}

#if DEBUG
struct SmsMessagesView_Previews: PreviewProvider {
    static var previews: some View {
        SmsMessagesView(device: devicesList[0], threadId: "1", contactName: "Bob Smith")
    }
}
#endif

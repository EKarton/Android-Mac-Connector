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
    @State private var messageToSend: String = ""
    @State private var sendingMessages = [String]()
    @State private var isLoadingMessages = false
    
    private var refreshMessagesSemaphore = DispatchSemaphore(value: 1)
    
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
                            message: message.body + " | " + message.phoneNumber
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
        .navigationBarTitle(Text(self.contactName + "|" + self.threadId), displayMode: .inline)
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
        print("Waiting for semaphore to finish")
        refreshMessagesSemaphore.wait()
        
        print("Semaphore finished! Now refreshing msgs recursively")
        refreshMessagesRecursively(1)
    }
    
    private let cap = 64000.0
    private let base = 500.0
    private let maxAttempts = 10
    
    func refreshMessagesRecursively(_ attempt: Int) {
        if attempt >= maxAttempts {
            self.refreshMessagesSemaphore.signal()
            return
        }
        print("refreshMessagesRecursively: \(attempt)")
        
        self.smsMessageService.fetchSmsMessages(self.device, self.threadId, 10000, 0) { (msgs: [SmsMessage], err: Error?) in
            if let err = err {
                print("Error encountered when refreshing messages: \(err)")
                self.refreshMessagesSemaphore.signal()
                return
            }
            
            self.messages = msgs
            
            var i = 0
            while (i < self.sendingMessages.count){
                for msg in msgs {
                    if self.sendingMessages.contains(msg.body) {
                        self.sendingMessages.remove(at: i)
                        i -= 1
                    }
                }
                i += 1
            }
            
            if self.sendingMessages.count == 0 {
                self.refreshMessagesSemaphore.signal()
                return
            }
            
            let temp = min(self.cap, self.base * pow(2, Double(attempt)))
            let sleepInMs = temp / 2 + Double.random(in: 0...(temp / 2))
            
            print("Did not get msgs that were sent. Retrying for \(sleepInMs) ms")
            
            DispatchQueue.main.asyncAfter(deadline: .now() + sleepInMs / 1000.0) {
                self.refreshMessagesRecursively(attempt + 1)
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

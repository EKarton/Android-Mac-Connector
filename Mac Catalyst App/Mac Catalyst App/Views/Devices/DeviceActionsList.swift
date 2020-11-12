//
//  DeviceActionsList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct DeviceActionsList: View {
    @EnvironmentObject var subscriber: MQTTSubscriptionClient
    
    var device: Device
    
    var body: some View {
        List {
            if device.hasSmsCapability {
                NavigationLink(destination: SmsThreadsList(device: device)) {
                    Text("Send / Read SMS")
                }
            }
        }
        .navigationBarTitle("\(self.device.name)", displayMode: .inline)
        .onAppear {
            self.subscriber.subscribe("\(self.device.id)/sms/messages/query-results") { err in
                
            }
            
            self.subscriber.subscribe("\(self.device.id)/sms/threads/query-results") { err in
                
            }
            
            self.subscriber.subscribe("\(self.device.id)/send-sms-results") { err in
                
            }
        }
    }
}

#if DEBUG
struct DeviceActionListPreview: PreviewProvider {
    static var previews: some View {
        DeviceActionsList(device: devicesList[0])
    }
}
#endif

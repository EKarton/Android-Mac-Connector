//
//  DeviceActionsListRow.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-27.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

enum DeviceActionsListRowType {
    case ReadSendSms
    case PingDevice
}

struct DeviceActionsListRow: View {
    var type: DeviceActionsListRowType
    var buttonText: String {
        get {
            switch(type){
                case .PingDevice: return "Ping Device"
                case .ReadSendSms: return "Send / Read SMS"
            }
        }
    }
    var imageSystemName: String {
        get {
            switch(type){
                case .PingDevice: return "bell"
                case .ReadSendSms: return "text.bubble"
            }
        }
    }
    
    var body: some View {
        HStack(spacing: 25) {
            Image(systemName: imageSystemName)
                .resizable()
                .scaledToFit()
                .frame(width: 25, alignment: .center)
                .padding(.leading, 16)
            Text(buttonText)
        }
    }
}

struct DeviceActionsListRow_Previews: PreviewProvider {
    static var previews: some View {
        DeviceActionsListRow(type: .ReadSendSms)
    }
}

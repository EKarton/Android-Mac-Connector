//
//  DeviceRow.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-27.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct DevicesListRow: View {
    let device: Device
    var imageUrl: String {
        get {
            switch (device.type) {
                case "macbook":
                    return "macbook"
                case "android_phone":
                    return "android_phone"
                default:
                    return "device"
            }
        }
    }
    
    var body: some View {
        HStack {
            Image(imageUrl)
                .resizable()
                .scaledToFit()
                .frame(height: 40)
            
            Text(device.name)
        }
    }
}

struct DeviceListRow_Preview: PreviewProvider {
    static var previews: some View {
        DevicesListRow(device: devicesList[0])
    }
}

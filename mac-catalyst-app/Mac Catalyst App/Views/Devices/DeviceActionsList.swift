//
//  DeviceActionsList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct DeviceActionsList: View {
    @EnvironmentObject var viewModelFactory: SmsThreadsViewModelFactory
    @EnvironmentObject var pingDeviceService: PingDeviceService
    var device: Device
    
    var body: some View {
        List {
            if device.hasPingDeviceCapability {
                Button(action: self.pingDevice) {
                    Text("Ping device")
                }
            }
            
            if device.hasSmsCapability {
                NavigationLink(destination: NavigationLazyView(
                    SmsThreadsView(
                        viewModel: self.viewModelFactory.createViewModel(self.device),
                        device: self.device
                    )
                )) {
                    Text("Send / Read SMS")
                }
            }
        }
        .navigationBarTitle("\(self.device.name) | \(self.device.id)", displayMode: .inline)
    }
    
    private func pingDevice() {
        self.pingDeviceService.pingDevice(device)
    }
}

#if DEBUG
struct DeviceActionListPreview: PreviewProvider {
    static var previews: some View {
        Text("Hello world")
//        DeviceActionsList(device: devicesList[0])
    }
}
#endif

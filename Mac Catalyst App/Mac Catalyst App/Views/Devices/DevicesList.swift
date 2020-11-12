//
//  DevicesList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct DevicesListView: View {
    @EnvironmentObject var deviceService: DeviceService
    @EnvironmentObject var sessionStore: SessionStore
    
    @State private var devicesList = [Device]()
    
    var body: some View {
        VStack {
            List(self.devicesList, id: \.id) { device in
                NavigationLink(destination: DeviceActionsList(device: device)) {
                    HStack {
                        Text(device.name)
                    }
                }
            }
            .navigationBarTitle(Text("Devices"), displayMode: .large)
        }
        .onAppear(perform: self.onAppearHandler)
    }
    
    private func onAppearHandler() {
        self.deviceService.getDevices(sessionStore.currentSession.accessToken) { (devices: [Device], error: Error?) in
            self.devicesList = devices
            print("Error: \(String(describing: error?.localizedDescription))")
        }
    }
}

#if DEBUG
struct DevicesListViewPreview: PreviewProvider {
    static var previews: some View {
        DevicesListView()
    }
}
#endif

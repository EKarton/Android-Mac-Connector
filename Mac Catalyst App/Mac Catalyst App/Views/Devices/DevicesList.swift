//
//  DevicesList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct DevicesListView: View {
    var showSettingsAction: () -> Void
    
    @EnvironmentObject var deviceService: DeviceService
    @EnvironmentObject var sessionStore: SessionStore
    
    @State private var devicesList = [Device]()
    
    var body: some View {
        NavigationView {
            List(self.devicesList, id: \.id) { device in
                NavigationLink(destination: DeviceActionsList(device: device)) {
                    HStack {
                        Text(device.name)
                    }
                }
            }
            .navigationBarTitle(Text("Devices"), displayMode: .large)
            .navigationBarItems(trailing:
                Button(action: {
                    print("Settings button clicked")
                    self.showSettingsAction()
                }) {
                    Image(systemName: "gear")
                }
            )
        }
        .navigationViewStyle(DefaultNavigationViewStyle())
        .onAppear(perform: self.onAppearHandler)
    }
    
    private func onAppearHandler() {
        self.deviceService.getDevices(sessionStore.currentSession.accessToken) { (devices: [Device], error: Error?) in
            guard error == nil else {
                print("Encountered error when fetching devices: \(error.debugDescription)")
                return
            }
            
            self.devicesList = devices
        }
    }
}

#if DEBUG
struct DevicesListViewPreview: PreviewProvider {
    static var previews: some View {
        DevicesListView(showSettingsAction: {
            print("Show settings button clicked")
        })
    }
}
#endif

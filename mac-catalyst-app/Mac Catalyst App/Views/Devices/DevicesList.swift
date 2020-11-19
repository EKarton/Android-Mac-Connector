//
//  DevicesList.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct DevicesListView: View {
    @EnvironmentObject var contentViewModel: ContentViewModel
    @EnvironmentObject var deviceViewModel: DeviceViewModel
    @EnvironmentObject var sessionStore: SessionStore
    
    @State private var devicesList = [Device]()
    
    var body: some View {
        NavigationView {
            List(self.deviceViewModel.devices, id: \.id) { device in
                NavigationLink(destination: DeviceActionsList(device: device)) {
                    HStack {
                        Text(device.name)
                    }
                }
            }
            .navigationBarTitle(Text("Devices"), displayMode: .large)
            .navigationBarItems(trailing:
                Button(action: self.onSettingsButtonClicked) {
                    Image(systemName: "gear")
                }
            )
        }
        .navigationViewStyle(DefaultNavigationViewStyle())
        .onAppear(perform: self.onAppearHandler)
    }
    
    private func onAppearHandler() {
        self.deviceViewModel.fetchDevices(sessionStore.currentSession.accessToken) { err in
            if let err = err {
                print("Encountered error when fetching devices: \(err.localizedDescription)")
            }
        }
    }
    
    private func onSettingsButtonClicked() {
        print("Settings button clicked")
        self.contentViewModel.showSettingsDialog()
    }
}

#if DEBUG
struct DevicesListViewPreview: PreviewProvider {
    static var previews: some View {
        DevicesListView()
    }
}
#endif

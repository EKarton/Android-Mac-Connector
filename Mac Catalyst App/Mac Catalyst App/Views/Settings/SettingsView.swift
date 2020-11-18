//
//  SettingsView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-16.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var contentViewModel: ContentViewModel
    @EnvironmentObject var sessionStore: SessionStore
    @EnvironmentObject var deviceViewModel: DeviceViewModel
        
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Device registration")) {
                    Button(action: self.onAddRemoveDeviceButtonClicked) {
                        Text(deviceViewModel.isRegistered ?
                            "Remove this device from your account" :
                            "Add this device to your account"
                        )
                    }
                }
                
                Section(header: Text("Account settings (bla@gmail.com)")) {
                    Button(action: self.onSignOutButtonClicked) {
                        Text("Sign out")
                    }
                }
            }
            .navigationBarTitle(Text("Settings"), displayMode: .inline)
            .navigationBarItems(trailing:
                Button(action: self.onCloseButtonClicked) {
                    Text("Close")
                }
            )
        }
        .onAppear(perform: onAppear)
    }
    
    private func onAppear() {
        let authToken = sessionStore.currentSession.accessToken
        self.deviceViewModel.checkIfCurrentDeviceIsRegistered(authToken) { err in
            if let err = err {
                print("Error when trying to see if device is registered or not: \(err)")
                return
            }
        }
    }
    
    private func onAddRemoveDeviceButtonClicked() {
        if self.deviceViewModel.isRegistered {
            unregisterDevice()
            
        } else {
            registerDevice()
        }
    }
    
    private func unregisterDevice() {
        let authToken = sessionStore.currentSession.accessToken
        deviceViewModel.unregisterDevice(authToken) { err in
            if let err = err {
                print("Error when unregistering device: \(err.localizedDescription)")
                return
            }
            self.refreshDevices()
        }
    }
    
    private func registerDevice() {
        let authToken = sessionStore.currentSession.accessToken
        deviceViewModel.registerDevice(authToken) { err in
            if let err = err {
                print("Error when registering device: \(err.localizedDescription)")
                return
            }
            self.refreshDevices()
        }
    }
    
    private func refreshDevices() {
        let authToken = sessionStore.currentSession.accessToken
        self.deviceViewModel.fetchDevices(authToken) { err in
            if let err = err {
                print("Error when fetching devices: \(err.localizedDescription)")
                return
            }
            self.contentViewModel.hideSettingsDialog()
        }
    }
    
    private func onSignOutButtonClicked() {
        sessionStore.signOut()
        self.contentViewModel.hideSettingsDialog()
    }
    
    private func onCloseButtonClicked() {
        self.contentViewModel.hideSettingsDialog()
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            SettingsView()
            SettingsView()
        }
    }
}

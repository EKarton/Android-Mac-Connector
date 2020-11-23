//
//  SettingsView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-16.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var appState: AppStateStore
    @EnvironmentObject var sessionStore: SessionStore
    @EnvironmentObject var deviceRegistrationStore: DeviceRegistrationStore
    @EnvironmentObject var deviceStore: DevicesStore
    
    @State private var isDeviceRegistrationLoading = false
        
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Device registration")) {
                    Button(action: self.onAddRemoveDeviceButtonClicked) {
                        Text(deviceRegistrationStore.isRegistered ?
                            "Remove this device from your account" :
                            "Add this device to your account"
                        )
                    }.disabled(self.isDeviceRegistrationLoading)
                }
                
                Section(header: Text("Account settings \(sessionStore.currentSession?.email ?? "")")) {
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
        self.deviceRegistrationStore.checkIfCurrentDeviceIsRegistered() { err in
            if let err = err {
                print("Error when trying to see if device is registered or not: \(err)")
                return
            }
        }
    }
    
    private func onAddRemoveDeviceButtonClicked() {
        self.isDeviceRegistrationLoading = true
        if self.deviceRegistrationStore.isRegistered {
            unregisterDevice()
            
        } else {
            registerDevice()
        }
    }
    
    private func unregisterDevice() {
        deviceRegistrationStore.unregisterDevice() { err in
            if let err = err {
                print("Error when unregistering device: \(err.localizedDescription)")
                self.isDeviceRegistrationLoading = false
                return
            }
            
            print("Successfully unregistered device")
            
            self.deviceStore.fetchDevices() { err in
                if let err = err {
                    print("Error when fetching devices: \(err.localizedDescription)")
                    self.isDeviceRegistrationLoading = false
                    return
                }
                
                self.isDeviceRegistrationLoading = false
                self.appState.showSettingsDialog = false
            }
        }
    }
    
    private func registerDevice() {
        self.appState.curState = .DeviceRegistration
        self.appState.showSettingsDialog = false
    }
    
    private func onSignOutButtonClicked() {
        sessionStore.signOut()
        self.appState.curState = .Auth
        self.appState.showSettingsDialog = false
    }
    
    private func onCloseButtonClicked() {
        self.appState.showSettingsDialog = false
    }
}

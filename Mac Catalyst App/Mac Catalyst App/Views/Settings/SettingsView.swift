//
//  SettingsView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-16.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SettingsView: View {
    @Binding var isPresent: Bool
    
    @EnvironmentObject var sessionStore: SessionStore
    
    var isRegistered: Bool = true
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Device registration")) {
                    Button(action: self.onRegisterDeviceAction) {
                        Text(isRegistered ?
                            "Remove this device from your account" :
                            "Add this device to your account"
                        )
                    }
                }
                
                Section(header: Text("Account settings (bla@gmail.com)")) {
                    Button(action: self.onSignOutAction) {
                        Text("Sign out")
                    }
                }
            }
            .navigationBarTitle(Text("Settings"), displayMode: .inline)
            .navigationBarItems(trailing:
                Button(action: self.onCloseAction) {
                    Text("Close")
                }
            )
        }
    }
    
    private func onRegisterDeviceAction() {
        isPresent = false
    }
    
    private func onSignOutAction() {
        sessionStore.signOut()
        isPresent = false
    }
    
    private func onCloseAction() {
        isPresent = false
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            SettingsView(isPresent: .constant(true))
            SettingsView(isPresent: .constant(true))
        }
    }
}

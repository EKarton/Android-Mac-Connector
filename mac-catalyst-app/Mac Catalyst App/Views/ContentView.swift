//
//  ContentView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct NavigationLazyView<Content: View>: View {
    let build: () -> Content
    init(_ build: @autoclosure @escaping () -> Content) {
        self.build = build
    }
    var body: Content {
        build()
    }
}

struct ContentView: View {
    @EnvironmentObject var appState: AppStateStore
    @EnvironmentObject var sessionStore: SessionStore
    @EnvironmentObject var devicesStore: DevicesStore
    @EnvironmentObject var deviceRegistrationStore: DeviceRegistrationStore
            
    var body: some View {
        HStack {
            if self.sessionStore.currentSession == nil {
                SignInView()
                
            } else if self.appState.curState == .DeviceRegistration {
                AddDeviceView()
                
            } else if self.appState.curState == .DevicesList {
                DevicesListView()
            }
        }
        .sheet(isPresented: self.$appState.showSettingsDialog) {
            SettingsView()
                .environmentObject(self.sessionStore)
                .environmentObject(self.devicesStore)
                .environmentObject(self.deviceRegistrationStore)
        }
        .onAppear {
            self.deviceRegistrationStore.checkIfCurrentDeviceIsRegistered() { err in
                if let err = err {
                    print("Error: \(err.localizedDescription)")
                    return
                }
                                    
                if !self.sessionStore.isSignedIn() {
                    self.appState.curState = .Auth
                    
                } else if !self.deviceRegistrationStore.isRegistered {
                    self.appState.curState = .DeviceRegistration
                    
                } else {
                    self.appState.curState = .DevicesList
                }
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        Text("Hello world")
//        ContentView()
    }
}


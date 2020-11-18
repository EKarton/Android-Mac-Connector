//
//  ContentView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var contentViewModel: ContentViewModel
    @EnvironmentObject var sessionStore: SessionStore
    @EnvironmentObject var deviceViewModel: DeviceViewModel
            
    var body: some View {
        return VStack {
            if (!self.sessionStore.currentSession.isSignedIn) {
                SignInView()
                
            } else if (self.contentViewModel.isAddDevicePagePresent) {
                AddDeviceView()
                
            } else {
                DevicesListView()
            }
        }
        .sheet(isPresented: self.$contentViewModel.isSettingsDialogPresent) {
            SettingsView()
                .environmentObject(self.contentViewModel)
                .environmentObject(self.sessionStore)
                .environmentObject(self.deviceViewModel)
        }
        .onAppear {
            self.sessionStore.bindListeners()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}


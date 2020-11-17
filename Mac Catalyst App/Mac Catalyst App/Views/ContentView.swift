//
//  ContentView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var sessionStore: SessionStore
    @State private var showSettingsModal = false
            
    var body: some View {
        let showSheet = Binding<Bool> (
            get: {
                return self.showSettingsModal
            },
            set: { (val: Bool) in }
        )
        
        return VStack {
            if (!self.sessionStore.currentSession.isSignedIn) {
                SignInView()
            } else {
                DevicesListView(showSettingsAction: {
                    self.showSettingsModal = true
                })
            }
        }
        .sheet(isPresented: showSheet) {
            if (self.showSettingsModal) {
                SettingsView(isPresent: self.$showSettingsModal)
                    .environmentObject(self.sessionStore)
                
            } else {
                Text("Error: view should not be here")
            }
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


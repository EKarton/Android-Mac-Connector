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
        
    var body: some View {
        VStack {
            NavigationView {
                if (!self.sessionStore.currentSession.isSignedIn) {
                    SignInView()
                } else {
                    DevicesListView()
                }
            }
            .navigationViewStyle(StackNavigationViewStyle())
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


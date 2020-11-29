//
//  SignInView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SignInView: View {
    @EnvironmentObject var appState: AppStateStore
    @EnvironmentObject var sessionStore: SessionStore
    
    
    @EnvironmentObject var deviceRegistrationStore: DeviceRegistrationStore
    
    @Environment(\.presentationMode) var presentation
    @State private var email = "";
    @State private var password = ""
    @State private var error = ""
    
    var body: some View {
        NavigationView {
            VStack {
                Text("Welcome back!")
                    .fontWeight(.semibold)
                    .font(.title)
                    .padding(.bottom, 20)
                
                Text("Please sign in")
                    .fontWeight(.medium)
                    .font(.subheadline)
                    .padding(.bottom, 30)
                
                TextField("Email", text: $email)
                    .padding(.bottom, 30)
                SecureField("Password", text: $password)
                    .padding(.bottom, 100)
                
                if (self.error != "") {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .padding(.bottom, 40)
                }
                
                Spacer()
                            
                Button(action: handleSignin) {
                    Text("Sign in")
                        .frame(minWidth: 0, maxWidth: .infinity)
                        .font(.system(size: 14, weight: .regular))
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(Color.white)
                        .cornerRadius(10)
                        .padding(.bottom, 40)
                }
                            
                NavigationLink(destination: SignUpView()) {
                    Text("Create an account")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.blue)
                        .padding(.bottom, 40)
                }
            }
            .padding(20)
            .frame(maxWidth: 500)
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
    
    func handleSignin() {
        print("Trying to sign in")
        self.sessionStore.signIn(email: self.email, password: self.password) { (error: Error?) in
            if let error = error {
                self.error = "An error occured: \(error.localizedDescription)"
                return
            }
            
            print("Successfully signed in")
            
            self.deviceRegistrationStore.checkIfCurrentDeviceIsRegistered() { err in
                if let err = err {
                    print("Failed to check if it is registered or not: \(err.localizedDescription)")
                    self.error = "An error occured: \(err.localizedDescription)"
                    return
                }
                
                print("Is registered? \(self.deviceRegistrationStore.isRegistered)")
                
                if self.deviceRegistrationStore.isRegistered {
                    self.appState.curState = .DevicesList
                } else {
                    self.appState.curState = .DeviceRegistration
                }
                
                self.presentation.wrappedValue.dismiss()
            }
        }
    }
}

struct SignInView_Previews: PreviewProvider {
    static var previews: some View {
        Text("Hello world")
//        SignInView()
    }
}

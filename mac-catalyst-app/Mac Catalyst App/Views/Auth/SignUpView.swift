//
//  SignUpView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct SignUpView: View {
    @Environment(\.presentationMode) var presentation
    @EnvironmentObject var sessionStore: SessionStore
    
    @State private var email = ""
    @State private var password1 = ""
    @State private var password2 = ""
    @State private var error = ""
    
    var body: some View {
        VStack {
            Text("Create an Account")
                .fontWeight(.semibold)
                .font(.title)
                .padding(.bottom, 30)
            
            TextField("Email", text: $email)
                .padding(.bottom, 20)
            SecureField("Password", text: $password1)
                .padding(.bottom, 20)
            SecureField("Password", text: $password2)
                .padding(.bottom, 40)
            
            if (self.error != "") {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding(.bottom, 40)
            }
            
            Spacer()
            
            Button(action: self.handleSignUp) {
                Text("Sign up")
                    .frame(minWidth: 0, maxWidth: .infinity)
                    .font(.system(size: 14, weight: .regular))
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(10)
                    .padding(.bottom, 60)
            }
        }
        .padding(20)
        .frame(maxWidth: 500)
    }
    
    func handleSignUp() {
        if (self.email.count == 0) {
            self.error = "Email should not be blank"
            return
        }
        
        if (self.password1 != self.password2) {
            self.error = "Passwords do not match!"
            return
        }
        
        if (self.password1.count == 0) {
            self.error = "Password should not be blank!"
            return
        }
        
        self.sessionStore.signUp(email: email, password: password1) { err in
            if let err = err {
                self.error = err.localizedDescription
                return
            }
            
            self.presentation.wrappedValue.dismiss()
        }
    }
}

struct SignUpView_Previews: PreviewProvider {
    static var previews: some View {
        Text("Hello world")
//        SignUpView()
    }
}

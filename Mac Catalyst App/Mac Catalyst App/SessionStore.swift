//
//  SessionStore.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import FirebaseAuth

enum SessionStoreErrors: Error {
    case CannotGetUserError
}

class SessionStore: ObservableObject {
    @Published var isSignedIn = false
    @Published var accessToken = ""
    
    private var handle: AuthStateDidChangeListenerHandle?
    
    init() {}
    
    func unbindListeners() {
        if let curHandle = self.handle {
            Auth.auth().removeStateDidChangeListener(curHandle)
            self.handle = nil
        }
    }
    
    func bindListeners(forceRebind: Bool = false) {
        if (self.handle != nil && forceRebind) {
            self.unbindListeners()
        }
        
        handle = Auth.auth().addStateDidChangeListener { (auth: Auth, user: User?) in
            print("Updated user state: \(user)")
            self.isSignedIn = user != nil

            if let curUser = user {
                self.updateAccessToken(curUser)
            }
        }
    }
    
    func signUp(email: String, password: String, handler: @escaping (Error?) -> Void) {
        Auth.auth().createUser(withEmail: email, password: password) { (result: AuthDataResult?, error: Error?) in
            if let user = result?.user {
                self.updateAccessToken(user)
            }
            
            self.isSignedIn = error == nil
            handler(error)
        }
    }
    
    func signIn(email: String, password: String, handler: @escaping (Error?) -> Void) {
        Auth.auth().signIn(withEmail: email, password: password) { (result: AuthDataResult?, error: Error?) in
            if let user = result?.user {
                self.updateAccessToken(user)
            }
            
            self.isSignedIn = error == nil
            handler(error)
        }
    }
    
    private func updateAccessToken(_ user: User) {
        user.getIDToken { (accessToken, error) in
            if error != nil {
                return
            }
            
            if let accessToken = accessToken {
                self.accessToken = accessToken
            }
        }
    }
    
    func signOut() {
        do {
            try Auth.auth().signOut()
        } catch {
            print("Error signing out")
        }
    }
}


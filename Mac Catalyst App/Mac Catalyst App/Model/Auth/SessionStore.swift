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

protocol SessionChangedListener: class {
    func handler(_ oldSession: Session, _ newSession: Session)
}

struct Session {
    var isSignedIn: Bool
    var accessToken: String
}

class SessionStore: ObservableObject {
    @Published var currentSession = Session(isSignedIn: false, accessToken: "")
    
    private var listeners = [SessionChangedListener]()
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
            print("Updated user state: \(String(describing: user))")
            if let curUser = user {
                self.updateSession(curUser)
            }
        }
    }
    
    func addSessionChangedListener(_ listener: SessionChangedListener) {
        self.listeners.append(listener)
    }
    
    func removeSessionChangedListener(_ listener: SessionChangedListener) {
        if let idx = self.listeners.firstIndex(where: { $0 === listener }) {
            self.listeners.remove(at: idx)
        }
    }
    
    func signUp(email: String, password: String, handler: @escaping (Error?) -> Void) {
        Auth.auth().createUser(withEmail: email, password: password) { (result: AuthDataResult?, error: Error?) in
            if let user = result?.user {
                self.updateSession(user)
            }
            handler(error)
        }
    }
    
    func signIn(email: String, password: String, handler: @escaping (Error?) -> Void) {
        Auth.auth().signIn(withEmail: email, password: password) { (result: AuthDataResult?, error: Error?) in
            if let user = result?.user {
                self.updateSession(user)
            }
            
            handler(error)
        }
    }
    
    private func updateSession(_ user: User) {
        user.getIDToken { (accessToken, error) in
            var newSession = Session(isSignedIn: false, accessToken: "")
            
            if let accessToken = accessToken {
                newSession = Session(isSignedIn: true, accessToken: accessToken)
            }
            
            self.listeners.forEach { listener in
                listener.handler(self.currentSession, newSession)
            }
            self.currentSession = newSession
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


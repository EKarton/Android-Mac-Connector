//
//  SessionStore.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-10.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI
import FirebaseAuth

protocol SessionStoreObserver: class {
    func onSessionChanged(_ oldSession: Session?, _ newSession: Session?)
}

struct Session {
    var email: String?
    var accessToken: String
}

class SessionStore: ObservableObject {
    @Published var currentSession: Session? = nil
    
    private var observers = [SessionStoreObserver]()
    private var handle: AuthStateDidChangeListenerHandle?
    
    func listen() {
        self.unbind()
        handle = Auth.auth().addStateDidChangeListener { (auth: Auth, user: User?) in
            self.updateSession(user) { _ in }
        }
    }
    
    func unbind() {
        if let curHandle = self.handle {
            Auth.auth().removeStateDidChangeListener(curHandle)
            self.handle = nil
        }
    }
    
    func addObserver(_ listener: SessionStoreObserver) {
        self.observers.append(listener)
    }
    
    func removeObserver(_ listener: SessionStoreObserver) {
        if let idx = self.observers.firstIndex(where: { $0 === listener }) {
            self.observers.remove(at: idx)
        }
    }
    
    func signUp(email: String, password: String, handler: @escaping (Error?) -> Void) {
        Auth.auth().createUser(withEmail: email, password: password) { (result: AuthDataResult?, error: Error?) in
            if let user = result?.user {
                self.updateSession(user) { err in
                   handler(err)
               }
            }
            handler(error)
        }
    }
    
    func signIn(email: String, password: String, handler: @escaping (Error?) -> Void) {
        Auth.auth().signIn(withEmail: email, password: password) { (result: AuthDataResult?, error: Error?) in
            if let user = result?.user {
                self.updateSession(user) { err in
                    handler(err)
                }
            } else {
                handler(error)
            }
        }
    }
    
    func isSignedIn() -> Bool {
        return Auth.auth().currentUser != nil
    }
    
    func signOut() {
        do {
            try Auth.auth().signOut()
            updateSession(nil){ _ in }
        } catch {
            print("Error signing out")
        }
    }
    
    private func updateSession(_ user: User?, _ handler: @escaping (Error?) -> Void) {
        let oldSession = self.currentSession
        var newSession: Session? = nil
        
        if let user = user {
            user.getIDToken { (accessToken, error) in
                if let accessToken = accessToken {
                    newSession = Session(email: user.email, accessToken: accessToken)
                    
                } else {
                    newSession = Session(email: user.email, accessToken: "")
                }
                
                self.currentSession = newSession
                self.notifyObservers(oldSession, newSession)
                handler(error)
            }
            
        } else {
            self.currentSession = newSession
            self.notifyObservers(oldSession, newSession)
            handler(nil)
        }
    }
    
    private func notifyObservers(_ oldSession: Session?, _ newSession: Session?) {
        self.observers.forEach { observer in
            observer.onSessionChanged(oldSession, newSession)
        }
    }
}


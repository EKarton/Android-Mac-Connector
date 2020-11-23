//
//  AppStateViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-23.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class AppStateStore: ObservableObject {
    enum State: String {
        case Auth = "auth"
        case DeviceRegistration = "reg"
        case DevicesList = "list"
    }

    @Published var curState: State? = .DevicesList
    @Published var showSettingsDialog = false
    
    func goToAuthentication() {
        self.curState = .Auth
    }
    
    func goToDeviceRegistration() {
        self.curState = .DeviceRegistration
    }
    
    func gotToDevicesList() {
        self.curState = .DevicesList
    }
}

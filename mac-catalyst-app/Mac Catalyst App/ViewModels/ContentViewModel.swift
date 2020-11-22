//
//  ContentViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-17.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class ContentViewModel: ObservableObject {
    @Published var isAddDevicePagePresent = false
    @Published var isDevicesPagePresent = false
    @Published var isSettingsDialogPresent = false
    
    func goToAddDevicePage() {
        isAddDevicePagePresent = true
        isDevicesPagePresent = false
        isSettingsDialogPresent = false
    }
    
    func goToDevicesPage() {
        isAddDevicePagePresent = false
        isDevicesPagePresent = true
        isSettingsDialogPresent = false
    }
    
    func hideSettingsDialog() {
        isSettingsDialogPresent = false
    }
    
    func showSettingsDialog() {
        isSettingsDialogPresent = true
    }
}

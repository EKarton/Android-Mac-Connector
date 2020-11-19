//
//  AddDeviceView.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-17.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

struct AddDeviceView: View {
    @EnvironmentObject var contentViewModel: ContentViewModel
    @EnvironmentObject var sessionStore: SessionStore
    @EnvironmentObject var deviceViewModel: DeviceViewModel
    @State private var error = ""
    
    var body: some View {
        VStack {
            Text("Add device?")
                .fontWeight(.semibold)
                .font(.title)
                .padding(.bottom, 20)
                .padding(.top, 30)
            
            Text("Adding this device to your account will allow your device to receive SMS notifications, app notifications, and files from other devices")
                .fontWeight(.medium)
                .font(.subheadline)
                .padding(.bottom, 30)
            
            Text("Note: if you add this device to your account, you will be asked a few permissions")
                .fontWeight(.medium)
                .font(.subheadline)
                .padding(.bottom, 30)
            
            if (self.error != "") {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding(.bottom, 40)
            }
            
            Spacer()
                        
            Button(action: onRegisterDeviceButtonClick) {
                Text("Add device")
                    .frame(minWidth: 0, maxWidth: .infinity)
                    .font(.system(size: 14, weight: .regular))
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(10)
                    .padding(.bottom, 40)
            }
            
            Button(action: onCancelButtonClick) {
                Text("Cancel")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.blue)
                    .padding(.bottom, 40)
            }
        }
        .padding(20)
        .frame(maxWidth: 500)
    }
    
    private func onRegisterDeviceButtonClick() {
        print("Register device button clicked")
        let authToken = sessionStore.currentSession.accessToken
        
        deviceViewModel.registerDevice(authToken) { err1 in
            if let err1 = err1 {
                self.error = err1.localizedDescription
                return
            }
            
            self.deviceViewModel.fetchDevices(authToken) { err2 in
                if let err2 = err2 {
                    self.error = err2.localizedDescription
                    return
                }
                
                self.contentViewModel.goToDevicesPage()
            }
        }
    }
    
    private func onCancelButtonClick() {
        print("Cancel button clicked")
        self.contentViewModel.goToDevicesPage()
    }
}

struct AddDeviceView_Previews: PreviewProvider {
    static var previews: some View {
        AddDeviceView()
            .environmentObject(DeviceViewModel(MockedDeviceWebService()))
    }
}

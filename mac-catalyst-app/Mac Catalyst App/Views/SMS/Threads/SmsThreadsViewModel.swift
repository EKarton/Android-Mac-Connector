//
//  SmsThreadsViewModel.swift
//  Mac Catalyst App
//
//  Created by Emilio Kartono on 2020-11-18.
//  Copyright © 2020 Emilio Kartono. All rights reserved.
//

import SwiftUI

class SmsThreadsViewModel: ObservableObject {
    @Published var threads = [SmsThread]()
    
    private var service: GetSmsThreadsService
    
    init(_ service: GetSmsThreadsService) {
        self.service = service
    }
    
    func fetchThreads(_ device: Device, _ limit: Int, _ start: Int, _ handler: @escaping (Error?) -> Void) {
        print("fetchThreads()")
        self.service.fetchSmsThreads(device, limit, start) { (threads, err) in
            if let err = err {
                handler(err)
                return
            }
            
            self.threads = threads
            handler(nil)
        }
    }
}

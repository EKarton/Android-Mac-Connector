import { messaging } from "firebase-admin";

export interface DeviceNotifier {
  notifyDevice(token: String)
}

export class AndroidDeviceNotifier implements DeviceNotifier {
  private readonly fcmMessaging: messaging.Messaging

  constructor(fcmMessaging: messaging.Messaging) {
    this.fcmMessaging = fcmMessaging
  }

  async notifyDevice(fcmToken: string) {
    let message: messaging.MessagingPayload = {
      data: {},
    };
    let options: messaging.MessagingOptions = {
      priority: "high"
    }
    await this.fcmMessaging.sendToDevice(fcmToken, message, options)
  }
}

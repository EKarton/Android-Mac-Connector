import { HttpError } from "../rest_api/middlewares";

export interface DeviceService {
  doesDeviceExist(userId: string, deviceType: string, hardwareId: string): Promise<boolean>
  registerDevice(userId: string, deviceType: string, hardwareId: string, capabilities: String[]): Promise<string>
  getDevices(userId: string): Promise<Device[]>
	updateDeviceCapabilities(deviceId: string, capabilities: string[])
  getDeviceCapabilities(deviceId: string): Promise<string[]>
  updatePushNotificationToken(deviceId: string, newToken: string)
	getPushNotificationToken(deviceId: string): Promise<string>
}

export interface Device {
  id: string,
  type: string,
  name: string,
  capabilities: string[],
}

export class FirebaseDeviceService implements DeviceService {
  private readonly firestoreClient: FirebaseFirestore.Firestore;

  constructor(firestoreClient: FirebaseFirestore.Firestore) {
    this.firestoreClient = firestoreClient
  }

  async doesDeviceExist(userId: string, deviceType: string, hardwareId: string): Promise<boolean> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const query = devicesCollection
      .where("user_id", "==", userId)
      .where("device_type", "==", deviceType)
      .where("hardware_id", "==", hardwareId)

    const results = await query.get()
    if (results.empty) {
      return false
    }

    return true
  }
  
  async registerDevice(userId: string, deviceType: string, hardwareId: string, capabilities: String[]): Promise<string> {
    if (await this.doesDeviceExist(userId, deviceType, hardwareId)) {
      throw new HttpError(409, "DeviceAlreadyExists", "Device already exist")
    }

    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.add({
      "user_id":                 userId,
      "device_type":             deviceType,
      "hardware_id":             hardwareId,
      "push_notification_token": "",
      "capabilities":            capabilities,
    })

    return doc.id
  }

  async updateDeviceCapabilities(deviceId: string, capabilities: string[]) {
    if (!(await this.doesDeviceIdExist(deviceId))) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId}does not exist`)
    }

    const devicesCollection = this.firestoreClient.collection("devices")
    await devicesCollection.doc(deviceId).update("capabilities", capabilities)
  }

  async doesDeviceIdExist(deviceId: string): Promise<boolean> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.doc(deviceId).get()

    return doc.exists
  }

  async getDevices(userId: string): Promise<Device[]> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const query = devicesCollection.where("user_id", "==", userId)

    const results = await query.get()

    return results.docs.map(doc => {
      return {
        id: doc.id,
        type: <string> doc.data()["device_type"],
        name: "My Phone",
        capabilities: <string[]> doc.data()["capabilities"],
      }
    })
  }

  async getDeviceCapabilities(deviceId: string): Promise<string[]> {
    const doc = await this.firestoreClient.doc(deviceId).get()    
    if (doc.exists) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId}does not exist`)
    }

    return <string[]> doc.data()["capabilities"]
  }

  async updatePushNotificationToken(deviceId: string, newToken: string) {
    if (!(await this.doesDeviceIdExist(deviceId))) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId}does not exist`)
    }

    const devicesCollection = this.firestoreClient.collection("devices")
    await devicesCollection.doc(deviceId).update("push_notification_token", newToken)
  }

  async getPushNotificationToken(deviceId: string): Promise<string> {
    const doc = await this.firestoreClient.doc(deviceId).get()    
    if (doc.exists) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId}does not exist`)
    }

    return <string> doc.data()["push_notification_token"]
  }
}

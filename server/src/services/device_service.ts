import { HttpError } from "../rest_api/middlewares";

export interface DeviceService {
  doesDeviceExist(userId: string, deviceType: string, hardwareId: string): Promise<string>
  registerDevice(userId: string, deviceType: string, hardwareId: string, name: string, capabilities: String[]): Promise<string>
  removeDevice(deviceId: string)
  getDevices(userId: string): Promise<Device[]>

  getDevice(deviceId: String): Promise<Device>
  updateDevice(deviceId: String, updatedInfo: UpdatedDevice)

  doesDeviceIdExist(deviceId: string): Promise<boolean>
  getDeviceType(deviceId: string): Promise<string>
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

export interface UpdatedDevice {
  new_type?: string,
  new_name?: string,
  new_capabilities?: string[]
}

export class FirebaseDeviceService implements DeviceService {
  private readonly firestoreClient: FirebaseFirestore.Firestore;

  constructor(firestoreClient: FirebaseFirestore.Firestore) {
    this.firestoreClient = firestoreClient
  }

  async getDevice(deviceId: string): Promise<Device> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.doc(deviceId).get()
    if (!doc.exists) {
      throw new HttpError(404, "DeviceNotExist", "Device does not exist")
    }

    const data = doc.data()

    return {
      id: doc.id,
      type: <string> data["device_type"],
      name: <string> data["name"],
      capabilities: <string[]> data["capabilities"]
    }
  }

  async updateDevice(deviceId: string, updatedInfo: UpdatedDevice) {
    const devicesCollection = this.firestoreClient.collection("devices")
    const result = await devicesCollection.doc(deviceId)
    const doesDocExist = (await result.get()).exists

    if (!doesDocExist) {
      throw new HttpError(404, "DeviceNotExist", "Device does not exist")
    }

    const newData = {}
    if (updatedInfo.new_name) {
      newData["name"] = updatedInfo.new_name
    }

    if (updatedInfo.new_type) {
      newData["device_type"] = updatedInfo.new_type
    }

    if (updatedInfo.new_capabilities) {
      newData["capabilities"] = updatedInfo.new_capabilities
    }

    await result.update(newData)
  }

  async doesDeviceExist(userId: string, deviceType: string, hardwareId: string): Promise<string> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const query = devicesCollection
      .where("user_id", "==", userId)
      .where("device_type", "==", deviceType)
      .where("hardware_id", "==", hardwareId)

    const results = await query.get()
    if (results.empty) {
      return ""
    }

    if (results.docs.length > 1) {
      return ""
    }

    return results.docs[0].id
  }
  
  async registerDevice(userId: string, deviceType: string, hardwareId: string, name: string, capabilities: String[]): Promise<string> {
    if (await this.doesDeviceExist(userId, deviceType, hardwareId)) {
      throw new HttpError(409, "DeviceAlreadyExists", "Device already exist")
    }

    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.add({
      "user_id":                 userId,
      "device_type":             deviceType,
      "hardware_id":             hardwareId,
      "name":                    name,
      "push_notification_token": "",
      "capabilities":            capabilities,
    })

    return doc.id
  }

  async removeDevice(deviceId: string) {
    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.doc(deviceId).get()

    if (!doc.exists) {
      throw new HttpError(404, "DeviceNotExist", "Device does not exist")
    }
  
    await devicesCollection.doc(deviceId).delete()
  }

  async updateDeviceCapabilities(deviceId: string, capabilities: string[]) {
    if (!(await this.doesDeviceIdExist(deviceId))) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId}does not exist`)
    }

    const devicesCollection = this.firestoreClient.collection("devices")
    await devicesCollection.doc(deviceId).update("capabilities", capabilities)
  }

  async doesDeviceIdExist(deviceId: string): Promise<boolean> {
    if (!deviceId) {
      return false
    }

    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.doc(deviceId).get()

    return doc.exists
  }

  async getDevices(userId: string): Promise<Device[]> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const query = devicesCollection.where("user_id", "==", userId)

    const results = await query.get()

    return results.docs.map(doc => {
      const data = doc.data()
      return {
        id: doc.id,
        type: <string> data["device_type"],
        name: <string> data["name"],
        capabilities: <string[]> data["capabilities"],
      }
    })
  }

  async getDeviceCapabilities(deviceId: string): Promise<string[]> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.doc(deviceId).get()    
    if (!doc.exists) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId} does not exist`)
    }

    return <string[]> doc.data()["capabilities"]
  }

  async getDeviceType(deviceId: string): Promise<string> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.doc(deviceId).get()    
    if (!doc.exists) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId} does not exist`)
    }

    return <string> doc.data()["device_type"]
  }

  async updatePushNotificationToken(deviceId: string, newToken: string) {
    if (!(await this.doesDeviceIdExist(deviceId))) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId} does not exist`)
    }

    const devicesCollection = this.firestoreClient.collection("devices")
    await devicesCollection.doc(deviceId).update("push_notification_token", newToken)
  }

  async getPushNotificationToken(deviceId: string): Promise<string> {
    const devicesCollection = this.firestoreClient.collection("devices")
    const doc = await devicesCollection.doc(deviceId).get()    
    if (!doc.exists) {
      throw new HttpError(404, "DeviceNotFound", `Device with id ${deviceId} does not exist`)
    }

    return <string> doc.data()["push_notification_token"]
  }
}

export interface Authorizer {
  /**
   * Checks if the client is authorized to publish to a topic
   * @param topic the topic
   * @param clientId the client id
   */
  isPublishAuthorized(topic: string, clientId: string): Promise<boolean>

  /**
   * Checks if the client is authorized to subscribe to a topic
   * @param topic the topic
   * @param clientId the client id
   */
  isSubscriptionAuthorized(topic: string, clientId: string): Promise<boolean>
}

export class GrantAllAuthorizer implements Authorizer {
  /**
   * Checks if the client is authorized to publish to a topic
   * It will return true
   * 
   * @param topic the topic
   * @param clientId the client id
   */
  isPublishAuthorized(topic: string, clientId: string): Promise<boolean> {
    return Promise.resolve(true)
  }

  /**
   * Checks if the client is authorized to subscribe to a topic
   * It will return true
   * 
   * @param topic the topic
   * @param clientId the client id
   */
  isSubscriptionAuthorized(topic: string, clientId: string): Promise<boolean> {
    return Promise.resolve(true)
  }
}

/**
 * An authorizer that will grant devices that belong to the same user
 */
export class FirebaseUserBasedAuthorizer implements Authorizer {
  private firestoreClient: FirebaseFirestore.Firestore;

  constructor(firestoreClient: FirebaseFirestore.Firestore) {
    this.firestoreClient = firestoreClient
  }

  isPublishAuthorized(topic: string, clientId: string): Promise<boolean> {
    return this.isUserIdOfTopicSameAsClientId(topic, clientId)
  }
  
  isSubscriptionAuthorized(topic: string, clientId: string): Promise<boolean> {
    return this.isUserIdOfTopicSameAsClientId(topic, clientId)
  }

  private async isUserIdOfTopicSameAsClientId(topic: string, clientId: string): Promise<boolean> {
    const resourceDevice = this.getDeviceIdOfTopic(topic)
    const resourceDeviceUserId = await this.getUserIdOfDevice(resourceDevice)
    const clientUserId = await this.getUserIdOfDevice(clientId)

    return resourceDeviceUserId == clientUserId
  }

  private getDeviceIdOfTopic(topic: string): string {
    const topicLevels = topic.split('/')
    if (topicLevels.length < 2) {
      throw new Error(`Invalid topic ${topic}`)
    }

    return topicLevels[0]
  }

  private async getUserIdOfDevice(deviceId: string): Promise<string> {
    const collection = this.firestoreClient.collection('devices')
    const doc = await collection.doc(deviceId).get()

    if (!doc.exists) {
      throw new Error(`Device id ${deviceId} does not exist`)
    }

    return doc.get("user_id")
  }
}

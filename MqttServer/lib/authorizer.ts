export interface Authorizer {
  authorizePublish(topic: string, clientId: string): Promise<boolean>
  authorizeSubscription(topic: string, clientId: string): Promise<boolean>
}

export class FirebaseAuthorizer implements Authorizer {
  private readonly firestoreClient: FirebaseFirestore.Firestore;

  constructor(firestoreClient: FirebaseFirestore.Firestore) {
    this.firestoreClient = firestoreClient
  }

  public authorizePublish(topic: string, clientId: string): Promise<boolean> {
    const topicLevels = topic.split('/')
    if (topicLevels.length < 2) {
      throw new Error('Invalid topic ' + topic)
    }

    const deviceId = topicLevels[0]
    const action = `${topicLevels[1]}:publish`

    // Refer to resource policies to see if the client is authorized to publish
    return this.isActionAuthorized(deviceId, action, clientId)
  }

  public authorizeSubscription(topic: string, clientId: string): Promise<boolean> {
    const topicLevels = topic.split('/')
    if (topicLevels.length < 2) {
      throw new Error('Invalid topic ' + topic)
    }

    const deviceId = topicLevels[0]
    const action = `${topicLevels[1]}:subscribe`

    return this.isActionAuthorized(deviceId, action, clientId)
  }

  /**
   * Checks if an action under a resource is authorized for a principal
   * If no resource policy is found, we assume that it is unauthorized
   * @param resource the resource
   * @param action the action
   * @param principal the principal
   */
  private async isActionAuthorized(resource: string, action: string, principal: string): Promise<boolean> {
    console.log(resource, action, principal)
    const collection = this.firestoreClient.collection('resource-policies')
    const allowQuery = collection
      .where('resource', '==', resource)
      .where('action', '==', action)
      .where('principal', 'array-contains', principal)

    const results = await allowQuery.get()
    if (results.empty) {
      return false
    }

    return true
  }
}
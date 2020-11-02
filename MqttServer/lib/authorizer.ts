export interface Authorizer {
  authorizePublish(topic: string, clientId: string): Promise<boolean>
  authorizeSubscription(topic: string, clientId: string): Promise<boolean>
}

export class FirebaseAuthorizer implements Authorizer {
  private readonly firestoreClient: FirebaseFirestore.Firestore;

  constructor(firestoreClient: FirebaseFirestore.Firestore) {
    this.firestoreClient = firestoreClient
  }

  public async authorizePublish(topic: string, clientId: string): Promise<boolean> {
    const topicLevels = topic.split('/')
    if (topicLevels.length < 2) {
      throw new Error('Invalid topic ' + topic)
    }

    console.log(topicLevels)

    const deviceId = topicLevels[0]
    const action = topicLevels[1]

    // Allow the originator to publish to its own topics
    if (clientId == deviceId) {
      return true
    }

    // Refer to resource policies to see if the client is authorized to publish

    console.log(topic)
    throw new Error("Method not implemented.");
  }
  
  public async authorizeSubscription(topic: string, clientId: string): Promise<boolean> {
    const topicLevels = topic.split('/')
    if (topicLevels.length < 2) {
      throw new Error('Invalid topic ' + topic)
    }

    console.log(topicLevels, clientId)

    const deviceId = topicLevels[0]
    const action = topicLevels[1]

    // Allow the originator to subscribe to its own topics
    if (clientId == deviceId) {
      return true
    }

    throw new Error("Method not implemented.");
  }
}
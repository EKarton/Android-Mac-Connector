export interface ResourcePolicyService {
  addPermission(action: string, principal: string, resource: string)
  deletePermission(action?: string, principal?: string, resource?: string)
  isAuthorized(action: string, principal: string, resource: string): Promise<boolean>
}

export class FirebaseResourcePolicyService implements ResourcePolicyService {
  private readonly firestoreClient: FirebaseFirestore.Firestore;

  constructor(firestoreClient: FirebaseFirestore.Firestore) {
    this.firestoreClient = firestoreClient
  }

  async addPermission(action: string, principal: string, resource: string) {
    // Check if the permission exists
    const resourcePolicyCollection = this.firestoreClient.collection("resource-policies")
    const docs = await this.getPermissions(action, principal, resource)
    
    if (docs.empty) {

      // Add a new entry
      resourcePolicyCollection.add({
        "action": action,
        "principal": [ principal ],
        "resource": resource
      })

    } else {

      // Update the docs with the principal
      docs.forEach((doc) => {
        const partialData = {
          "principal": principal
        }
        const setOptions = {
          merge: true,
          mergeFields: ["principal"]
        }

        resourcePolicyCollection.doc(doc.id).set(partialData, setOptions)
      })
    }
  }

  async deletePermission(action?: string, principal?: string, resource?: string) {
    const collection = this.firestoreClient.collection("resource-policies")
    const docs = await this.getPermissions(action, principal, resource)
    const pendingResults = [];

    docs.forEach(doc => {
      const pendingResult = collection.doc(doc.id).delete()
      pendingResults.push(pendingResult)
    })

    await Promise.all(pendingResults)
  }

  async isAuthorized(action: string, principal: string, resource: string): Promise<boolean> {
    const docs = await this.getPermissions(action, principal, resource)
    
    if (docs.empty) {
      return false
    }
    return true
  }

  private getPermissions(action?: string, principal?: string, resource?: string): Promise<FirebaseFirestore.QuerySnapshot<FirebaseFirestore.DocumentData>> {
    if (!action || !principal || !resource) {
      throw new Error("Specify at least an action, principal, or a resource to do a query!")
    }

    var query: any = this.firestoreClient.collection("resource-policies")

    if (action) {
      query = query.where("action", "==", action)
    }

    if (principal) {
      query = query.where("principal", "array-contains", principal)
    }

    if (resource) {
      query = query.where("resource", "==", resource)
    }

    return query.get()
  }
}
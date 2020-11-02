export interface Authorizer {
  authorizePublish(topic: string, clientId: string): Promise<boolean>
  authorizeSubscription(topic: string, clientId: string): Promise<boolean>
}

export class FirebaseAuthorizer implements Authorizer {
  public authorizePublish(topic: string, clientId: string): Promise<boolean> {
    throw new Error("Method not implemented.");
  }
  public authorizeSubscription(topic: string, clientId: string): Promise<boolean> {
    throw new Error("Method not implemented.");
  } 
}
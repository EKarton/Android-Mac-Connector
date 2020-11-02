export interface Authenticator {
  authenticate(clientId: string, username: string, password: string): Promise<boolean>
}

export class FirebaseAuthenticator implements Authenticator {
  public authenticate(clientId: string, username: string, password: string): Promise<boolean> {
    throw new Error("Method not implemented.");
  }
}

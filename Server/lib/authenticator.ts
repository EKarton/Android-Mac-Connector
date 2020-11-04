import { auth } from "firebase-admin";

export interface Authenticator {
  authenticate(deviceId: string, username: string, password: string): Promise<boolean>
  verifyOauthToken(token: string): Promise<boolean>
}

export class FirebaseAuthenticator implements Authenticator {
  private readonly firebaseAuth: auth.Auth;
  private readonly firestore: FirebaseFirestore.Firestore;

  constructor(firebaseAuth: auth.Auth, firestore: FirebaseFirestore.Firestore) {
    this.firebaseAuth = firebaseAuth
    this.firestore = firestore
  }

  public async authenticate(deviceId: string, _username: string, password: string): Promise<boolean> {
    const result = await this.firebaseAuth.verifyIdToken(password, true)
    const actualUserId = await this.getUserIdFromDeviceId(deviceId)
    return result.uid == actualUserId
  }

  private async getUserIdFromDeviceId(deviceId: string): Promise<string> {
    const doc = await this.firestore.collection('devices').doc(deviceId).get()
    if (!doc.exists) {
      throw new Error('No such document exists')
    } else {
      return doc.get('user_id')
    }
  }

  public async verifyOauthToken(token: string): Promise<boolean> {
    await this.firebaseAuth.verifyIdToken(token, true)
    return true
  }
}

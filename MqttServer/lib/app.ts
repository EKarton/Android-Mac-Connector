import aedes, { Aedes, AedesOptions, AuthenticateError, AuthErrorCode, Client, PublishPacket, Subscription } from 'aedes'
import { createServer, Server } from 'net'
import { Authenticator, FirebaseAuthenticator } from './authenticator'
import { Authorizer, FirebaseAuthorizer } from './authorizer'

import * as admin from 'firebase-admin';

export interface App {
  startServer()
  stopServer()
}

export class MqttServerApp implements App {
  private readonly serverPort = 1883 
  private readonly server: Server = null

  /**
   * Creates the app server
   */
  constructor() {
    const app = admin.initializeApp();
    const authServer = app.auth();
    const firestore = app.firestore();

    const authenticator = new FirebaseAuthenticator(authServer, firestore)
    const authorizer = new FirebaseAuthorizer(firestore)

    const mqttServer = this.createMqttServer(authenticator, authorizer)
    this.server = createServer(mqttServer.handle)
  }

  private createMqttServer(authenticator: Authenticator, authorizer: Authorizer): Aedes {
    const mqttServerOpts: AedesOptions = {
      authenticate: (client: Client, username: string, password: Buffer, done: (error: AuthenticateError | null, success: boolean | null) => void) => {
        authenticator.authenticate(client.id, username, password.toString())
          .then((isAuthenticated: boolean) => {
            done(null, isAuthenticated)
          })
          .catch((err: Error) => {
            const wrappedError: AuthenticateError = {
              returnCode: 4,
              ...err,
            }
            done(wrappedError, false)
          })
      },
      authorizePublish: (client: Client, packet: PublishPacket, callback: (error?: Error | null) => void) => {
        authorizer.authorizePublish(packet.topic, client.id)
          .then((isAuthorized: boolean) => {
            isAuthorized ? callback(null) : callback(new Error('Unauthorized'))
          })
          .catch((err: Error) => {
            console.error(err)
            callback(err)
          })
      },
      authorizeSubscribe: (client: Client, subscription: Subscription, callback: (error: Error | null, subscription?: Subscription | null) => void) => {
        authorizer.authorizeSubscription(subscription.topic, client.id)
          .then((isAuthorized: boolean) => {
            isAuthorized ? callback(null, subscription) : callback(new Error('Unauthorized'), null)
          })
          .catch((err: Error) => {
            console.error(err)
            callback(err, null)
          })
      }
    }
    return aedes(mqttServerOpts)
  }

  /**
   * Starts the server
   */
  public startServer() {
    this.server.listen(this.serverPort, () => {
      console.log('Server started and listening on port ', this.serverPort)
    })
  }

  /**
   * Shuts down the server
   */
  public stopServer() {
    this.server.close((err: Error) => {
      if (err) {
        console.error('Server shutdown encountered error:', err)
        return
      }

      console.log('Server shutdown successful')
    })
  }
}

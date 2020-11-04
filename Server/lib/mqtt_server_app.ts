import aedes, { Aedes, AedesOptions, AuthenticateError, Client, PublishPacket, Subscription } from 'aedes'
import { createServer, Server } from 'net'
import { Authenticator, FirebaseAuthenticator } from './authenticator'
import { Authorizer, FirebaseAuthorizer } from './authorizer'

import * as admin from 'firebase-admin';
import { App } from "./app";

export interface MqttServerAppOptions {
  verifyAuthentication: boolean,
  verifyAuthorization: boolean,
}

export class MqttServerApp implements App {
  private readonly opts?: MqttServerAppOptions = null
  private readonly serverPort = 1883 
  private readonly server: Server = null

  /**
   * Creates the app server
   */
  constructor(firebaseApp: admin.app.App, opts?: MqttServerAppOptions) {
    this.opts = opts
    console.log(opts)

    const authServer = firebaseApp.auth();
    const firestore = firebaseApp.firestore();

    const authenticator = new FirebaseAuthenticator(authServer, firestore)
    const authorizer = new FirebaseAuthorizer(firestore)

    const mqttServer = this.createMqttServer(authenticator, authorizer)
    this.server = createServer(mqttServer.handle)
  }

  private createMqttServer(authenticator: Authenticator, authorizer: Authorizer): Aedes {
    const mqttServerOpts: AedesOptions = {
      authenticate: (client: Client, username: string, password: Buffer, done: (error: AuthenticateError | null, success: boolean | null) => void) => {
        
        if (!(this.opts?.verifyAuthentication)) {
          done(null, true)
          return
        }
        
        if (password.length == 0 || username.length == 0) {
          done(null, false)
          return
        }

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
        if (!(this.opts?.verifyAuthorization)) {
          callback(null)
          return
        }
        authorizer.isPublishAuthorized(packet.topic, client.id)
          .then((isAuthorized: boolean) => {
            isAuthorized ? callback(null) : callback(new Error('Unauthorized'))
          })
          .catch((err: Error) => {
            console.error(err)
            callback(err)
          })
      },
      authorizeSubscribe: (client: Client, subscription: Subscription, callback: (error: Error | null, subscription?: Subscription | null) => void) => {
        if (!(this.opts.verifyAuthorization)) {
          callback(null, subscription)
          return
        }
        
        authorizer.isSubscriptionAuthorized(subscription.topic, client.id)
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

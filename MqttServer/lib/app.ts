import aedes, { AedesOptions, AuthenticateError, Client, PublishPacket, Subscription } from 'aedes'
import { createServer, Server } from 'net'
import { Authenticator } from './authenticator'
import { Authorizer } from './authorizer'

export interface App {
  startServer()
  stopServer()
}

export class MqttServerApp implements App {
  private readonly serverPort = 1883 
  private readonly server: Server = null

  /**
   * Creates the app server
   * @param authenticator the authenticator
   * @param authorizer the authorizer
   */
  constructor(authenticator: Authenticator, authorizer: Authorizer) {
    const mqttServerOpts: AedesOptions = {
      authenticate: (client: Client, username: string, password: Buffer, done: (error: AuthenticateError | null, success: boolean | null) => void) => {
        authenticator.authenticate(client.id, username, password.toString())
          .then((isAuthenticated: boolean) => {
            done(null, isAuthenticated)
          })
          .catch((err: Error) => {
            const wrappedError = {
              ...err,
              returnCode: aedes.AuthErrorCode.SERVER_UNAVAILABLE
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
            callback(err)
          })
      },
      authorizeSubscribe: (client: Client, subscription: Subscription, callback: (error: Error | null, subscription?: Subscription | null) => void) => {
        authorizer.authorizeSubscription(subscription.topic, client.id)
          .then((isAuthorized: boolean) => {
            isAuthorized ? callback(null) : callback(new Error('Unauthorized'))
          })
          .catch((err: Error) => {
            callback(err)
          })
      }
    }
    const mqttServer = aedes(mqttServerOpts)
    this.server = createServer(mqttServer.handle)
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

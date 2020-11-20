import aedes, { Aedes, AedesOptions, AedesPublishPacket, AuthenticateError, Client, PublishPacket, Subscription } from 'aedes'
import { Authenticator } from '../services/authenticator'
import { Authorizer } from '../services/authorizer'

import { DeviceNotifier } from '../services/device_notifier';
import { DeviceService } from '../services/device_service';

export interface MqttAppOptions {
  verifyAuthentication: boolean,
  verifyAuthorization: boolean,
}

export class MqttAppBuilder {
  private opts: MqttAppOptions = null
  private authenticator: Authenticator = null;
  private authorizer: Authorizer = null;
  private androidDeviceNotifier: DeviceNotifier = null;
  private deviceService: DeviceService = null;
  
  constructor() {}

  public withAuthenticator(authenticator: Authenticator): MqttAppBuilder {
    this.authenticator = authenticator
    return this
  }

  public withAuthorizer(authorizer: Authorizer): MqttAppBuilder {
    this.authorizer = authorizer
    return this
  }

  public withAndroidDeviceNotifier(androidDeviceNotifier: DeviceNotifier): MqttAppBuilder {
    this.androidDeviceNotifier = androidDeviceNotifier
    return this
  }

  public withDeviceService(deviceService: DeviceService): MqttAppBuilder {
    this.deviceService = deviceService
    return this
  }

  public withOpts(opts: MqttAppOptions): MqttAppBuilder {
    this.opts = opts
    return this
  }

  public build(): Aedes {
    const mqttAppOptions = this.createOptions()
    const mqttServer = aedes(mqttAppOptions)

    this.attachHooksToMqttApp(mqttServer)

    return mqttServer
  }

  private createOptions(): AedesOptions {
    const mqttServerOpts: AedesOptions = {
      authenticate: (client: Client, username: string, password: Buffer, done: (error: AuthenticateError | null, success: boolean | null) => void) => {
        if (!(this.opts?.verifyAuthentication)) {
          console.log(`${client.id}: Authenticated? true`)
          done(null, true)
          return
        }
        
        if (password.length == 0 || username.length == 0) {
          console.log(`${client.id}: Authenticated? false`)
          done(null, false)
          return
        }

        this.authenticator.authenticate(client.id, username, password.toString())
          .then((isAuthenticated: boolean) => {
            console.log(`${client.id}: Authenticated? ${isAuthenticated}`)
            done(null, isAuthenticated)
          })
          .catch((err: Error) => {
            console.log(`${client.id}: Authenticated? false`)
            const wrappedError: AuthenticateError = {
              returnCode: 4,
              ...err,
            }
            done(wrappedError, false)
          })
      },
      authorizePublish: (client: Client, packet: PublishPacket, callback: (error?: Error | null) => void) => {
        if (!(this.opts?.verifyAuthorization)) {
          console.log(`${client.id}: Authorize publish to ${packet.topic}? true`)
          callback(null)
          return
        }

        this.authorizer.isPublishAuthorized(packet.topic, client.id)
          .then((isAuthorized: boolean) => {
            console.log(`${client.id}: Authorize publish to ${packet.topic}? ${isAuthorized}`)
            isAuthorized ? callback(null) : callback(new Error('Unauthorized'))
          })
          .catch((err: Error) => {
            console.log(`${client.id}: Authorize publish to ${packet.topic}? false`)
            console.error(err)
            callback(err)
          })
      },
      authorizeSubscribe: (client: Client, subscription: Subscription, callback: (error: Error | null, subscription?: Subscription | null) => void) => {
        if (!(this.opts.verifyAuthorization)) {
          console.log(`${client.id}: Authorize subscribe? true`)
          callback(null, subscription)
          return
        }
        
        this.authorizer.isSubscriptionAuthorized(subscription.topic, client.id)
          .then((isAuthorized: boolean) => {
            console.log(`${client.id}: Authorize subscribe? ${isAuthorized}`)
            isAuthorized ? callback(null, subscription) : callback(new Error('Unauthorized'), null)
          })
          .catch((err: Error) => {
            console.log(`${client.id}: Authorize subscribe? false`)
            console.error(err)
            callback(err, null)
          })
      }
    }
    return mqttServerOpts
  }

  private attachHooksToMqttApp(mqttServer: Aedes) {
    mqttServer.on("publish", async (packet: AedesPublishPacket, client: Client) => {
      console.log(`${client ? client.id : "null"}: Publish to ${packet.topic} | ${packet.dup} | ${packet.qos}`)

      let topicParts = packet.topic.split('/')
      if (topicParts.length == 0) {
        console.log(`Cannot get device id from topic ${topicParts}`)
        return
      }

      let deviceId = topicParts[0]

      if (await this.deviceService.doesDeviceIdExist(deviceId)) {
        let token = await this.deviceService.getPushNotificationToken(deviceId)
        let deviceType = await this.deviceService.getDeviceType(deviceId)

        if (deviceType == "android_phone") {
          await this.androidDeviceNotifier.notifyDevice(token)

        } else {
          console.log(`Unsupported push notification for device ${deviceType}`)
        }
      }
    })
  }
}

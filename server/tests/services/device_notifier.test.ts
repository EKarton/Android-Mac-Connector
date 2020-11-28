import { AndroidDeviceNotifier } from "../../src/services/device_notifier"

const sendToDeviceFn = jest.fn()
const FirebaseMessenger = jest.fn().mockImplementation(() => {
  return {
    sendToDevice: sendToDeviceFn,
  }
})

describe("notifyDevice()", () => {
  it("should call sendToDevice() with proper arguments, given valid fcm token", async () => {
    const messenger = new FirebaseMessenger()
    const notifier = new AndroidDeviceNotifier(messenger)
    await notifier.notifyDevice("1234")

    const expectedMessage = {
      data: {},
    };
    const expectedOptions = {
      priority: "high"
    }

    expect(sendToDeviceFn).toHaveBeenCalledWith("1234", expectedMessage, expectedOptions)
  })

  it("should throw an error, given sendToDeviceFn() throws an error", () => {
    sendToDeviceFn.mockImplementation(() => {
      return Promise.reject(new Error("Error message"))
    })

    const messenger = new FirebaseMessenger()
    const notifier = new AndroidDeviceNotifier(messenger)

    expect(notifier.notifyDevice("1234")).rejects.toMatch("Error message")
  })
})
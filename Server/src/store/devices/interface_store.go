package devices

type Device struct {
	UserId                string
	DeviceId              string
	DeviceType            string
	HardwareId            string
	PushNotificationToken string
	Capabilities          []string
}

type DevicesStore interface {
	DoesDeviceExist(userId string, deviceType string, hardwareId string) (bool, error)
	RegisterDevice(userId string, deviceType string, hardwareId string) (string, error)
	UpdateDeviceCapabilities(deviceId string, capabilities []string) error
	GetDeviceCapabilities(deviceId string) ([]string, error)
	UpdatePushNotificationToken(deviceId string, newToken string) error
}

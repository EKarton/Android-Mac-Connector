package devices

type Device struct {
	Capabilities []string
}

type DevicesStore interface {
	GetDevices(userId string) ([]Device, error)
	AddDevice(userId string, device Device) (string, error)
	DeleteDevice(deviceId string) error
}

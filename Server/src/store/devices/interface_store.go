package devices

type Device struct {
	Capabilities []string
}

type DevicesStore interface {
	GetDevice(deviceId string) (Device, error)
	AddDevice(device Device) (string, error)
	DeleteDevice(deviceId string) error
}

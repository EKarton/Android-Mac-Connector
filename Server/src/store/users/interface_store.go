package users

type UsersStore interface {
	AddUser(userId string) error
	DeleteUser(userId string) error
	DoesUserExist(userId string) (bool, error)
	RegisterDeviceToUser(userId string, deviceId string) error
	UnregisterDeviceFromUser(userId string, deviceId string) error
	GetDevicesRegisteredToUser(userId string) ([]string, error)
}

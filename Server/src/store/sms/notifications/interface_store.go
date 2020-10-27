package notifications

type SmsNotification struct {
	Uuid        string
	ContactInfo string
	Data        string
	Timestamp   int
}

type SmsNotificationsStore interface {
	AddSmsNotification(deviceId string, notification SmsNotification) error
	GetNewSmsNotificationsFromUuid(numNotifications int, startingUuid string) ([]SmsNotification, error)
	GetPreviousSmsNotificationsFromUuid(numNotifications int, startingUuid string) ([]SmsNotification, error)
	GetOldestSmsNotification(deviceId string) (SmsNotification, error)
	GetLatestSmsNotification(deviceId string) (SmsNotification, error)
}

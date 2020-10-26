package notifications

type SmsNotification struct {
	Uuid        string
	ContactInfo string
	Data        string
	Timestamp   int
}

type SmsNotificationsStore interface {
	AddSmsNotification() error
	GetNewSmsNotificationsFromUuid(numNotifications int, startingUuid string) ([]SmsNotification, error)
	GetPreviousSmsNotificationsFromUuid(numNotifications int, startingUuid string) ([]SmsNotification, error)
	GetOldestSmsNotification() (SmsNotification, error)
	GetLatestSmsNotification() (SmsNotification, error)
}

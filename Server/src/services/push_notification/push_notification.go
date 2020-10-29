package push_notification

type PushNotificationService interface {
	SendMessage(deviceToken string, data map[string]string) error
}

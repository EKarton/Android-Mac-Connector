package push_notification

import (
	"context"

	"firebase.google.com/go/messaging"

	firebaseMessaging "firebase.google.com/go/messaging"
)

type AndroidPushNotificationService struct {
	client *messaging.Client
}

func CreateAndroidPushNotificationService(client *messaging.Client) *AndroidPushNotificationService {
	return &AndroidPushNotificationService{
		client: client,
	}
}

func (service *AndroidPushNotificationService) SendMessage(deviceToken string, data map[string]string) error {
	var message = firebaseMessaging.Message{
		Data:  data,
		Token: deviceToken,
	}

	_, err := service.client.Send(context.Background(), &message)
	return err
}

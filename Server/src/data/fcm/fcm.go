package fcm

import (
	"context"
	"log"

	firebase "firebase.google.com/go"
	"firebase.google.com/go/messaging"
	firebaseMessaging "firebase.google.com/go/messaging"
)

var cachedApp *firebase.App

func getApp() *firebase.App {
	if cachedApp == nil {
		app, err := firebase.NewApp(context.Background(), nil)
		if err != nil {
			log.Fatalf("Error initializing Firebase app: %v\n", err)
		}

		cachedApp = app
	}
	return cachedApp
}

func getMessagingClient() *messaging.Client {
	client, err := getApp().Messaging(context.Background())
	if err != nil {
		log.Fatalf("Error initializing Firebase app: %v\n", err)
	}

	return client
}

func SendFcmMessage(deviceToken string, data map[string]string, notification *messaging.Notification) error {
	var message = firebaseMessaging.Message{
		Data:         data,
		Notification: notification,
		Token:        deviceToken,
	}

	_, err := getMessagingClient().Send(context.Background(), &message)
	return err
}

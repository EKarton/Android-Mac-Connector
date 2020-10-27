package fcm

import (
	"context"

	"firebase.google.com/go/auth"
	"firebase.google.com/go/messaging"

	firestore "cloud.google.com/go/firestore"
	firebase "firebase.google.com/go"
	firebaseMessaging "firebase.google.com/go/messaging"
)

var cachedApp *firebase.App
var cachedFcmClient *messaging.Client
var cachedAuthClient *auth.Client
var cachedFirestoreClient *firestore.Client

func getApp() *firebase.App {
	if cachedApp == nil {
		if app, err := firebase.NewApp(context.Background(), nil); err != nil {
			panic("Error initializing Firebase app: %v\n" + err.Error())

		} else {
			cachedApp = app
		}
	}

	return cachedApp
}

func getMessagingClient() *messaging.Client {
	if cachedFcmClient == nil {
		if client, err := getApp().Messaging(context.Background()); err != nil {
			panic("Error initializing Firebase Cloud Messaging client: %v\n" + err.Error())

		} else {
			cachedFcmClient = client
		}
	}
	return cachedFcmClient
}

func getAuthClient() *auth.Client {
	if cachedAuthClient == nil {

		if client, err := getApp().Auth(context.Background()); err != nil {
			panic("Error initializing Firebase Authentication client: %v\n" + err.Error())

		} else {
			cachedAuthClient = client
		}
	}
	return cachedAuthClient
}

func GetFirestoreClient() *firestore.Client {
	if cachedFirestoreClient == nil {
		if client, err := getApp().Firestore(context.Background()); err != nil {
			panic("Error initializing Firebase Authentication client: %v\n" + err.Error())

		} else {
			cachedFirestoreClient = client
		}
	}
	return cachedFirestoreClient
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

func VerifyAccessToken(accessToken string) (bool, string, error) {
	token, err := getAuthClient().VerifyIDTokenAndCheckRevoked(context.Background(), accessToken)

	if err != nil {
		return false, "", err
	}
	return true, token.UID, nil
}

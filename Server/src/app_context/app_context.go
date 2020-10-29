package app_context

import (
	authService "Android-Mac-Connector-Server/src/services/auth"
	"Android-Mac-Connector-Server/src/services/push_notification"
	pushNotificationService "Android-Mac-Connector-Server/src/services/push_notification"
	"Android-Mac-Connector-Server/src/services/sms_notifications"
	"Android-Mac-Connector-Server/src/store/devices"
	"Android-Mac-Connector-Server/src/store/jobs"
	"Android-Mac-Connector-Server/src/store/resourcepolicies"
	"context"

	"cloud.google.com/go/firestore"
	firebase "firebase.google.com/go"
	"firebase.google.com/go/auth"
	"firebase.google.com/go/messaging"
)

type ApplicationContext struct {
	Services   *Services
	DataStores *DataStores
}

type Services struct {
	AuthService                        authService.AuthService
	AndroidPushNotificationService     push_notification.PushNotificationService
	SmsNotificationSubscriptionService *sms_notifications.SmsNotificationSubscriptionService
}

type DataStores struct {
	DevicesStores         devices.DevicesStore
	JobQueueService       jobs.JobQueueService
	ResourcePoliciesStore resourcepolicies.ResourcePoliciesStore
}

type firebaseClients struct {
	app             *firebase.App
	messagingClient *messaging.Client
	authClient      *auth.Client
	firestoreClient *firestore.Client
}

func CreateApplicationContext() *ApplicationContext {
	firebaseClients := createFirebaseClients()

	smsNotificationService := sms_notifications.CreateFirebaseSmsNotificationService(firebaseClients.firestoreClient, 10)
	smsNotificationSubService := sms_notifications.CreateSmsNotificationSubscriptionService(smsNotificationService)

	return &ApplicationContext{
		Services: &Services{
			AuthService:                        authService.CreateFirebaseAuthService(firebaseClients.authClient),
			AndroidPushNotificationService:     pushNotificationService.CreateAndroidPushNotificationService(firebaseClients.messagingClient),
			SmsNotificationSubscriptionService: smsNotificationSubService,
		},
		DataStores: &DataStores{
			DevicesStores:         devices.CreateFirestoreDevicesStore(firebaseClients.firestoreClient),
			JobQueueService:       jobs.CreateFirebaseJobQueueService(firebaseClients.firestoreClient),
			ResourcePoliciesStore: resourcepolicies.CreateInMemoryStore(),
		},
	}
}

func createFirebaseClients() *firebaseClients {
	client := firebaseClients{}

	// Create the firebase clients
	app, err := firebase.NewApp(context.Background(), nil)
	if err != nil {
		panic("Error initializing Firebase app: %v\n" + err.Error())
	}
	client.app = app

	messagingClient, err := app.Messaging(context.Background())
	if err != nil {
		panic("Error initializing Firebase Cloud Messaging client: %v\n" + err.Error())
	}
	client.messagingClient = messagingClient

	authClient, err := app.Auth(context.Background())
	if err != nil {
		panic("Error initializing Firebase Authentication client: %v\n" + err.Error())
	}
	client.authClient = authClient

	fireStoreClient, err := app.Firestore(context.Background())
	if err != nil {
		panic("Error initializing Firestore client: %v\n" + err.Error())
	}
	client.firestoreClient = fireStoreClient

	return &client
}

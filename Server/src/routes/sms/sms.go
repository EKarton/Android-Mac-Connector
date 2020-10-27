package sms

import (
	"github.com/gorilla/mux"

	messagesRoute "Android-Mac-Connector-Server/src/routes/sms/messages"
	notificationsRoute "Android-Mac-Connector-Server/src/routes/sms/notifications"
	sendSmsRoute "Android-Mac-Connector-Server/src/routes/sms/send"

	"Android-Mac-Connector-Server/src/store"
)

func InitializeRouter(dataStore *store.Datastore, router *mux.Router) {
	notificationsRouter := router.PathPrefix("/notifications").Subrouter()
	sendSmsRouter := router.PathPrefix("/send").Subrouter()
	messagesRouter := router.PathPrefix("/threads").Subrouter()

	notificationsRoute.InitializeRouter(dataStore, notificationsRouter)
	sendSmsRoute.InitializeRouter(dataStore, sendSmsRouter)
	messagesRoute.InitializeRouter(dataStore, messagesRouter)
}

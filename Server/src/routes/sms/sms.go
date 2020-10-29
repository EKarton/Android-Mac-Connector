package sms

import (
	"github.com/gorilla/mux"

	notificationsRoute "Android-Mac-Connector-Server/src/routes/sms/notifications"

	"Android-Mac-Connector-Server/src/store"
)

func InitializeRouter(dataStore *store.Datastore, router *mux.Router) {
	notificationsRouter := router.PathPrefix("/notifications").Subrouter()

	notificationsRoute.InitializeRouter(dataStore, notificationsRouter)
}

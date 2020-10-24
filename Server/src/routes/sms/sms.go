package sms

import (
	"github.com/gorilla/mux"

	messagesRoute "Android-Mac-Connector-Server/src/routes/sms/messages"
	threadsRoute "Android-Mac-Connector-Server/src/routes/sms/threads"
)

func InitializeRouter(router *mux.Router) {
	var messagesRouter = router.PathPrefix("/messages").Subrouter()
	var threadsRouter = router.PathPrefix("/threads").Subrouter()

	messagesRoute.InitializeRouter(messagesRouter)
	threadsRoute.InitializeRouter(threadsRouter)
}

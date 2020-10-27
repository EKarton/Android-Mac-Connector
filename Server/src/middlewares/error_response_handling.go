package middlewares

import (
	"encoding/json"
	logger "log"
	"net/http"
)

type HttpError interface {
	HttpCode() int
	ErrorCode() string
	Error() string
}

type HttpErrorResponseBody struct {
	ErrorCode string `json:"error_code"`
	Reason    string `json:"reason"`
}

func HandleErrors(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if errorCaught := recover(); errorCaught != nil {
				logger.Println(errorCaught.(HttpError))

				if httpError, isHttpError := errorCaught.(HttpError); isHttpError {

					// Specify the response code
					w.WriteHeader(httpError.HttpCode())

					// Write response body in json
					json.NewEncoder(w).Encode(HttpErrorResponseBody{
						ErrorCode: httpError.ErrorCode(),
						Reason:    httpError.Error(),
					})

				} else if err, isError := errorCaught.(error); isError {

					// Specify response code
					w.WriteHeader(http.StatusInternalServerError)

					// Write response body in json
					json.NewEncoder(w).Encode(HttpErrorResponseBody{
						ErrorCode: "InternalServerError",
						Reason:    err.Error(),
					})

				} else {
					w.WriteHeader(http.StatusInternalServerError)
				}
			}
		}()

		next.ServeHTTP(w, r)
	})
}

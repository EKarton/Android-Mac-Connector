package auth

import (
	"context"

	"firebase.google.com/go/auth"
)

type FirebaseAuthService struct {
	client *auth.Client
}

func CreateFirebaseAuthService(client *auth.Client) *FirebaseAuthService {
	return &FirebaseAuthService{
		client: client,
	}
}

func (service *FirebaseAuthService) VerifyAccessToken(accessToken string) (bool, string, error) {
	token, err := service.client.VerifyIDTokenAndCheckRevoked(context.Background(), accessToken)

	if err != nil {
		return false, "", err
	}
	return true, token.UID, nil
}

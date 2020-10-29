package auth

type AuthService interface {
	VerifyAccessToken(accessToken string) (bool, string, error)
}

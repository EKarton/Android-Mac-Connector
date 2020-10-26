package resourcepolicies

type ResourcePolicy struct {
	Effect    string
	Principal string
	Action    string
	Resource  string
}

type ResourcePoliciesStore interface {
	GrantResourceForPrincipal(principalDevice string, action string, resource string) error
	DenyResourceForPrincipal(principalDevice string, action string, resource string) error
	GetPermissionsForPrincipal(principalDevice string) ([]ResourcePolicy, error)
	IsResourceGrantedForPrincipal(principalDevice string, action string, resource string) (bool, error)
	GetPrincipalsForResource(action string, resource string) ([]string, error)
}

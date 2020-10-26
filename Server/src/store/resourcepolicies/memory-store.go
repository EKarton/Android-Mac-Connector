package resourcepolicies

type InMemoryResourcePoliciesStore struct{}

func CreateInMemoryStore() *InMemoryResourcePoliciesStore {
	store := InMemoryResourcePoliciesStore{}
	return &store
}

func (store *InMemoryResourcePoliciesStore) GrantResourceForPrincipal(principalDevice string, action string, resource string) error {
	return nil
}

func (store *InMemoryResourcePoliciesStore) DenyResourceForPrincipal(principalDevice string, action string, resource string) error {
	return nil
}

func (store *InMemoryResourcePoliciesStore) GetPermissionsForPrincipal(principalDevice string) ([]ResourcePolicy, error) {
	return make([]ResourcePolicy, 0), nil
}

func (store *InMemoryResourcePoliciesStore) IsResourceGrantedForPrincipal(principalDevice string, action string, resource string) (bool, error) {
	return true, nil
}

func (store *InMemoryResourcePoliciesStore) GetPrincipalsForResource(action string, resource string) ([]string, error) {
	return make([]string, 0), nil
}

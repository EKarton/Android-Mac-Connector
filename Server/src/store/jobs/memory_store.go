package jobs

import (
	"log"
	uuid "github.com/google/uuid"
)

type InMemoryJobsStore struct {
	jobIdToJobStatus map[string]string
}

func CreateInMemoryStore() *InMemoryJobsStore {
	store := InMemoryJobsStore {
		jobIdToJobStatus: make(map[string]string)
	}

	return &store
}

func (store *InMemoryJobsStore) AddJob(status string) (string, error) {
	jobId := ""

	if uuid, err := uuid.NewRandom(); err != nil {
		return "", err

	} else {
		jobId = uuid.String()
	}

	store.jobIdToJobStatus[jobId] = status
	return jobId, nil
}
func (store *InMemoryJobsStore) DeleteJob(jobId string) error {
	delete(store.jobIdToJobStatus, jobId)
	return nil
}

func (store *InMemoryJobsStore) UpdateJobStatus(jobId string, newStatus string) error {
	store.jobIdToJobStatus[jobId] = newStatus
	return nil
}

func (store *InMemoryJobsStore) GetJobStatus(jobId string) GetJobStatus(jobId string) error {
	jobStatus := store.jobIdToJobStatus[jobId] 
	return jobStatus, nil
}
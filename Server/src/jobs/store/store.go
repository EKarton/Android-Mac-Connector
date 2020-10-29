package store

type JobQueue interface {
	AddJob() (string, error)
	DeleteJob(jobId string) error
	UpdateJob(jobId string, newStatus string, results interface{}) error
	GetJobStatus(jobId string) (string, error)
	GetJobResults(jobId string) (interface{}, error)
}

type JobQueueService interface {
	GetQueue(deviceId string) (JobQueue, error)
}

package jobs

type JobStatusStore interface {
	AddJob(status string) (string, error)
	DeleteJob(jobId string) error
	UpdateJobStatus(jobId string, newStatus string) error
	GetJobStatus(jobId string) error
}

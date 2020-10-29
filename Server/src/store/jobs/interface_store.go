package jobs

type JobStore interface {
	AddJob(status string, data interface{}) (string, error)
	DeleteJob(jobId string) error
	UpdateJobStatus(jobId string, newStatus string) error
	UpdateJobData(jobId string, newData interface{}) error
	GetJobStatus(jobId string) (string, error)
	GetJobData(jobId string) (interface{}, error)
}

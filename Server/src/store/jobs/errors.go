package jobs

import (
	"fmt"
	"net/http"
)

type JobNotFoundError struct {
	jobId string
}

func CreateJobNotFoundError(jobId string) *JobNotFoundError {
	return &JobNotFoundError{jobId}
}

func (err *JobNotFoundError) HttpCode() int {
	return http.StatusNotFound
}

func (err *JobNotFoundError) ErrorCode() string {
	return "JobNotFound"
}

func (err *JobNotFoundError) Error() string {
	return fmt.Sprintf("Job with ID %s is not found", err.jobId)
}

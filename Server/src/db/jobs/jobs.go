package jobs

import (
	uuid "github.com/google/uuid"
)

var jobIdToJob = make(map[string]interface{})

func AddJob(job interface{}) string {
	uuid, _ := uuid.NewRandom()
	uuidString := uuid.String()

	jobIdToJob[uuidString] = job

	return uuidString
}

func RemoveJob(jobId string) {
	delete(jobIdToJob, jobId)
}

func GetJobStatus(jobId string) interface{} {
	return jobIdToJob[jobId]
}

func UpdateJobStatus(jobId string, newJob interface{}) {
	jobIdToJob[jobId] = newJob
}

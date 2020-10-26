package jobs

import (
	"testing"

	jobsStore "Android-Mac-Connector-Server/src/store/jobs"
)

func TestCreateInMemoryStore(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	if store == nil {
		t.Error("Store is nil")
	}
}

func TestAddJob_ShouldReturnDifferentJobIds_WhenCalledTwice(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	jobId1, err1 := store.AddJob("pending")
	jobId2, err2 := store.AddJob("pending")

	if err1 != nil {
		t.Error("err1 threw error")
	} else if err2 != nil {
		t.Error("err2 threw error")
	} else if jobId1 == jobId2 {
		t.Error("jobId1 " + jobId1 + "should be different from jobId2 " + jobId2)
	}
}

func TestDeleteJob_ShouldReturnNoError_GivenValidJobId(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	jobId, _ := store.AddJob("pending")
	err := store.DeleteJob(jobId)

	if err != nil {
		t.Error("Should not throw an error")
	}
}

func TestDeleteJob_ShouldReturnError_GivenInvalidJobId(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	err := store.DeleteJob("test job id")

	if err == nil {
		t.Error("Should return error")
	}
}

func TestUpdateJobStatus_ShouldReturnStatus_GivenValidJobId(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	jobId, _ := store.AddJob("pending")
	err := store.UpdateJobStatus(jobId, "complete")
	newStatus, _ := store.GetJobStatus(jobId)

	if err != nil {
		t.Error("Should not return error")
	}

	if newStatus != "complete" {
		t.Error("New job status should be complete")
	}
}

func TestUpdateJobStatus_ShouldReturnError_GivenInvalidJobId(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	err := store.UpdateJobStatus("test job id", "complete")

	if err == nil {
		t.Error("Should return error")
	}
}

func TestGetJobStatus_ShouldReturnStatus_GivenValidJobId(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	jobId, _ := store.AddJob("pending")
	status, err := store.GetJobStatus(jobId)

	if err != nil {
		t.Error("Should not return error")
	}

	if status != "pending" {
		t.Error("Job status should be pending")
	}
}

func TestGetJobStatus_ShouldReturnError_GivenInvalidJobId(t *testing.T) {
	store := jobsStore.CreateInMemoryStore()
	_, err := store.GetJobStatus("test job id")

	if err == nil {
		t.Error("Should return error")
	}
}

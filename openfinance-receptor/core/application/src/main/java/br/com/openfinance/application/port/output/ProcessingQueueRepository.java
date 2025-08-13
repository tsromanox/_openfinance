package br.com.openfinance.application.port.output;

import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ProcessingQueueRepository {
    ProcessingJob save(ProcessingJob job);
    List<ProcessingJob> fetchNextBatch(int size);
    
    @Transactional
    void updateStatus(Long jobId, JobStatus status);
    
    void moveToDeadLetter(Long jobId);
}

import java.time.Duration;
import java.time.Instant;
public class AnonymousAccountToken {
    private final String tokenHash;
    private final Instant createdAt;
    private int submissionCount;
    private Instant windowStart;
    private Instant lastUsedAt;

    AnonymousAccountToken(String tokenHash, Instant createdAt){
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.submissionCount = 0;
        this.windowStart = createdAt;
        this.lastUsedAt = createdAt;
    }
    public boolean isRateLimited(int maxSubmissionPerWindow, Duration window, Instant now){
        if(now.isAfter(windowStart.plus(window))){
            return false;
        }
        if(submissionCount >= maxSubmissionPerWindow){
            return true;
        }
        return false;
    }

    public void recordSubmission(Duration window, Instant now){
        if(now.isAfter(windowStart.plus(window))){
            windowStart = now;
            submissionCount=0;
        }
        submissionCount++;
        lastUsedAt = now;
    }

    public String getTokenHash(){
        return tokenHash;
    }
    public Instant getCreatedAt(){
        return createdAt;
    }
    public int getSubmissionCount(){
        return submissionCount;
    }
    public Instant getWindowStart(){
        return windowStart;
    }
    public Instant getLastUsedAt(){
        return lastUsedAt;
    }
    
}

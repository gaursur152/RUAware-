import java.util.UUID;
import java.time.Instant;
public class Report {
    
    private final UUID reportId;
    private final IncidentType incidentType;
    private String incidentSubType;
    private final String description;
    private final LocationZone locationZone;
    private final TimeBucket occurredAtBucket;
    private final Campus campus;
    private final Instant reportSubmittedAt;
    private final String reporterTokenHash;
    private SeverityLevel severityLevel;
    private UrgencyLevel urgencyLevel;
    private ReportStatus status;

    Report(IncidentType incidentType, String incidentSubType, String description, LocationZone locationZone, Campus campus, SeverityLevel severityLevel, UrgencyLevel urgencyLevel, TimeBucket occurredAtBucket, String reporterTokenHash){
        this.incidentType = incidentType;
        this.incidentSubType = incidentSubType;
        this.description = description;
        this.locationZone = locationZone;
        this.campus = campus;
        this.severityLevel = severityLevel;
        this.urgencyLevel = urgencyLevel;
        this.occurredAtBucket = occurredAtBucket;
        this.reporterTokenHash = reporterTokenHash;
        status = ReportStatus.SUBMITTED;
        reportId = UUID.randomUUID();
        reportSubmittedAt = Instant.now();

    }

    public UUID getReportId(){
        return reportId;
    }
    public IncidentType getIncidentType(){
        return incidentType;
    }
    public String getIncidentSubType(){
        return incidentSubType;
    }
    public void setIncidentSubType(String value){
        incidentSubType = value;
        //return incidentSubType;
    }
    public String getDescription(){
        return description;
    }
    public LocationZone getLocationZone(){
        return locationZone;
    }
    public TimeBucket getOccurredAtBucket(){
        return occurredAtBucket;
    }
    public Campus getCampus(){
        return campus;
    }
    public Instant getReportSubmittedAt(){
        return reportSubmittedAt;
    }
    public String getReporterTokenHash(){
        return reporterTokenHash;
    }
    public SeverityLevel getSeverityLevel(){
        return severityLevel;
    }
    public void setSeverityLevel(SeverityLevel level){
        severityLevel = level;
        ///return severityLevel;
    }
    public UrgencyLevel getUrgencyLevel(){
        return urgencyLevel;
    }
    public void setUrgencyLevel(UrgencyLevel level){
        urgencyLevel = level;
        //return urgencyLevel;
    }
    public ReportStatus getStatus(){
        return status;
    }
    public void setStatus(ReportStatus status){
        this.status = status;
        //return this.status;
    }

    
}

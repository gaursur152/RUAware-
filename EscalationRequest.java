import java.util.UUID;
import java.time.Instant;
public class EscalationRequest {
    private final UUID escalationId;
    private final UUID linkedReportId;
    private final EscalationRequestType requestType;
    private final boolean requestFollowUpContact;
    private final ContactMethod contactMethod;
    private final String contactInfo;
    private final Instant consentTimestamp;
    private final Instant createdAt;
    private String preferredContactNotes;
    private EscalationStatus status;
    private String assignedCoordinator;
    EscalationRequest(UUID escalationId, UUID linkedReportId, EscalationRequestType requestType, boolean requestFollowUpContact, ContactMethod contactMethod, String contactInfo,Instant consentTimestamp, Instant createdAt){
        if(requestFollowUpContact == true && (contactInfo == null || contactMethod == null)){
            throw new IllegalArgumentException("Error no contact method found");
        }
            this.escalationId = escalationId;
            this.linkedReportId = linkedReportId;
            this.requestType = requestType;
            this.requestFollowUpContact = requestFollowUpContact;
            this.consentTimestamp = consentTimestamp;
            this.createdAt = createdAt;
            this.contactInfo = contactInfo;
            this.contactMethod = contactMethod;
            status = EscalationStatus.PENDING;
        
    }

    public UUID getEscalationId(){
        return escalationId;
    }
    public UUID getLinkedReportId(){
        return linkedReportId;
    }
    public EscalationRequestType getRequestType(){
        return requestType;
    }
    public boolean isRequestFollowUpContact(){
        return requestFollowUpContact;
    }
    public ContactMethod getContactMethod(){
        return contactMethod;
    }
    public String getContactInfo(){
        return contactInfo;
    }
    public Instant getConsentTimestamp(){
        return consentTimestamp;
    }
    public Instant getCreatedAt(){
        return createdAt;
    }
    public String getPreferredContactNotes(){
        return preferredContactNotes;
    }
    public void setPreferredContactNotes(String value){
        preferredContactNotes = value;
    }
    public EscalationStatus getStatus(){
        return status;
    }
    public void setStatus(EscalationStatus value){
        status = value;
    }
    public String getAssignedCoordinator(){
        return assignedCoordinator;
    }
    public void setAssignedCoordinator(String value){
        assignedCoordinator = value;
    }

}

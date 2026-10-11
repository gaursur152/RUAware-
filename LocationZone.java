
public enum LocationZone {
    COLLEGE_AVE(Campus.NEW_BRUNSWICK),
    COOK_DOUG(Campus.NEW_BRUNSWICK),
    BUSCH(Campus.NEW_BRUNSWICK),
    LIVINGSTON(Campus.NEW_BRUNSWICK),
    NEWARK(Campus.NEWARK),
    CAMDEN(Campus.CAMDEN),
    OFF_CAMPUS(null);



    

    private final Campus location;

    LocationZone(Campus location){
        this.location = location;
    }

    public Campus getCampus(){
        return location;
    }
       
}

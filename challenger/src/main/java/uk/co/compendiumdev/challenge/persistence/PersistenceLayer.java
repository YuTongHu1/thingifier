package uk.co.compendiumdev.challenge.persistence;

import uk.co.compendiumdev.challenge.ChallengerAuthData;
import uk.co.compendiumdev.challenge.ChallengerState;
import uk.co.compendiumdev.challenge.challengers.Challengers;

public class PersistenceLayer {

    private StorageType storeOn;

    // TODO: have a database persistence layer e.g. 'save to disk' option for the todos
    // TODO: single player mode should have this switched on by default
    // TODO: allow configuring what is on and what is off for any storage type using constructor rather than environment variables
    // todo: add all active storage mechanisms in a list and store on all - switch it off by removing from list

    ChallengerPersistenceMechanism file = new ChallengerFileStorage();
    static ChallengerPersistenceMechanism aws;
    boolean allowSaveToS3 = false;
    boolean allowLoadFromS3 = false;

    public void setToCloud() {
        storeOn = PersistenceLayer.StorageType.CLOUD;
    }

    public void switchOffPersistence() {
        storeOn=StorageType.NONE;
    }

    public PersistenceResponse tryToLoadChallenger(final Challengers challengers,
                                                  final String xChallengerGuid) {

        final PersistenceResponse response = loadChallengerStatus(xChallengerGuid);

        if(response.isSuccess()){
            ChallengerAuthData challenger = challengers.createNewChallenger();
            challenger.fromData(response.getAuthData(), challengers.getDefinedChallenges());
            challenger.touch();
            challenger.setState(ChallengerState.LOADED_FROM_PERSISTENCE);// refresh last accessed date
            challengers.put(challenger);
        }

        return response;
    }

    public enum StorageType{LOCAL, CLOUD, NONE};

    public PersistenceLayer(StorageType storeWhere){
        this.storeOn = storeWhere;

        if(this.storeOn==StorageType.CLOUD){

            String allow_save = System.getenv("AWS_ALLOW_SAVE");
            if(allow_save!=null && allow_save.toLowerCase().trim().equals("true")){
                allowSaveToS3=true;
            }

            String allow_load = System.getenv("AWS_ALLOW_LOAD");
            if(allow_load!=null && allow_load.toLowerCase().trim().equals("true")){
                allowLoadFromS3=true;
            }

            String bucketName = System.getenv("AWSBUCKET");
            aws= new AwsS3Storage(allowSaveToS3, allowLoadFromS3, bucketName);
        }
    }

    public PersistenceResponse saveChallengerStatus(ChallengerAuthData data){

        if(storeOn== StorageType.LOCAL){
            return file.saveChallengerStatus(data);
        }

        if(storeOn==StorageType.CLOUD && aws!=null){
            return aws.saveChallengerStatus(data);
        }

        //if(storeOn==StorageType.NONE){
        return new PersistenceResponse().withSuccess(false).withErrorMessage("No Persistence Configured - store in memory only.");
        //}
    }

    public PersistenceResponse loadChallengerStatus(String guid){

        if(storeOn== StorageType.LOCAL){
            return file.loadChallengerStatus(guid);
        }

        if(storeOn==StorageType.CLOUD && aws!=null){
            return aws.loadChallengerStatus(guid);
        }

        //if(storeOn==StorageType.NONE){
        return new PersistenceResponse().withSuccess(false).withErrorMessage("No Persistence Configured - store in memory only.");
        //}
    }

    public boolean willAutoSaveLoadChallengerStatusToPersistenceLayer(){

        if(storeOn==StorageType.LOCAL){
            return true;
        }

        if(storeOn==StorageType.CLOUD && allowSaveToS3){
            return true;
        }

        return false;
    }
}

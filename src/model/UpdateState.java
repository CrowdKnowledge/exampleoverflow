package model;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class UpdateState {

	@PrimaryKey
    @Persistent
	private String id = "state";
	
	@Persistent
	private String lastCreationDate = "";

	public String getId() {
		return id;
	}

	public String getLastCreationDate() {
		return lastCreationDate;
	}

	public void setLastCreationDate(String lastCreationDate) {
		this.lastCreationDate = lastCreationDate;
	}
}

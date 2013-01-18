package model;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Codedata {

	@PrimaryKey
    @Persistent
	private String id = "codedata";
	
	@Persistent
	private double numOfLines = 0;
	private double numOfSnippets = 0;

	public String getId() {
		return id;
	}

	public double getNumOfLines() {
		return numOfLines;
	}

	public void setNumOfLines(double numOfLines) {
		this.numOfLines = numOfLines;
	}
	
	public void addNumOfLines(double numOfLines) {
		this.numOfLines += numOfLines;
	}
	
	public double getNumOfSnippets() {
		return numOfSnippets;
	}

	public void setNumOfSnippets(double numOfSnippets) {
		this.numOfSnippets = numOfSnippets;
	}
	
	public void addNumOfSnippets(double numOfSnippets) {
		this.numOfSnippets += numOfSnippets;
	}
}

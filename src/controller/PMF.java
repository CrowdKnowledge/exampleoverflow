package controller;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public final class PMF {
	
	private static final PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("transactions-optional");
	
	private PMF(){}
	
	public static PersistenceManagerFactory get(){
		return pmf;
	}
}

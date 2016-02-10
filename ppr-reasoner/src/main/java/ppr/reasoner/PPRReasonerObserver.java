package ppr.reasoner;


public interface PPRReasonerObserver {

	public void beforeResource(String file);
	public void afterResource(String file);
	public void beforeQuery();
	public void afterQuery();
	public void beforeSetup();
	public void afterSetup();
	public void setKBSize(long kbSize);
	public long getKBSize();
	public long resourceLoadTime(String f);
	public long resourcesLoadTime();
	public long setupTime();
	public long queryExecutionTime();
}

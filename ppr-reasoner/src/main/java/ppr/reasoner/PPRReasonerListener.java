package ppr.reasoner;


public interface PPRReasonerListener {

	public void beforeResource(String file);
	public void afterResource(String file);
	public void beforeResources();
	public void afterResources();
	public void beforeQuery();
	public void afterQuery();
	public void setKBSize(long kbSize);
	public long getKBSize();
	public long resourceLoadTime(String f);
	public long resourcesLoadTime();
	public long queryExecutionTime();
}

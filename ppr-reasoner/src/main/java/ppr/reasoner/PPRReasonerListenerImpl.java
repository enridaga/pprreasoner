package ppr.reasoner;

import java.util.Map;
import java.util.HashMap;

public class PPRReasonerListenerImpl implements PPRReasonerListener {
	private long beforeResources;
	private long afterResources;
	private long beforeQuery;
	private long afterQuery;
	private Map<String, Long> beforeResource = new HashMap<String, Long>();
	private Map<String, Long> afterResource = new HashMap<String, Long>();
	private long kbsize;
	@Override
	public void afterQuery() {
		afterQuery = System.currentTimeMillis();
	}

	public void afterResource(String file) {
		afterResource.put(file, System.currentTimeMillis());
	};

	public void afterResources() {
		afterResources = System.currentTimeMillis();
	};

	public void beforeQuery() {
		beforeQuery = System.currentTimeMillis();
	};

	public void beforeResource(String file) {
		beforeResource.put(file, System.currentTimeMillis());
	};

	public void beforeResources() {
		beforeResources = System.currentTimeMillis();
	};

	public long queryExecutionTime() {
		return afterQuery - beforeQuery;
	};

	public long resourceLoadTime(String f) {
		return afterResource.get(f) - beforeResource.get(f);
	};

	public long resourcesLoadTime() {
		return afterResources - beforeResources;
	}

	public long getKBSize() {
		return kbsize;
	}

	public void setKBSize(long kbsize) {
		this.kbsize = kbsize;
	};
}

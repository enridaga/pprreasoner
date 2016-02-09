package ppr.reasoner;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PPRReasonerListenerImpl implements PPRReasonerListener {
	private long beforeQuery;
	private long afterQuery;
	private Map<String, Long> beforeResource = new HashMap<String, Long>();
	private Map<String, Long> afterResource = new HashMap<String, Long>();
	private long kbsize;
	private long beforeSetup;
	private long afterSetup;

	@Override
	public void afterQuery() {
		afterQuery = System.currentTimeMillis();
	}

	public void afterResource(String file) {
		afterResource.put(file, System.currentTimeMillis());
	};

	public void beforeQuery() {
		beforeQuery = System.currentTimeMillis();
	};

	public void beforeResource(String file) {
		beforeResource.put(file, System.currentTimeMillis());
	};

	public long queryExecutionTime() {
		return afterQuery - beforeQuery;
	};

	public long resourceLoadTime(String f) {
		return afterResource.get(f) - beforeResource.get(f);
	};

	public long resourcesLoadTime() {
		long time = 0;
		for (Entry<String, Long> e : afterResource.entrySet()) {
			time += (e.getValue() - beforeResource.get(e.getKey()));
		}
		return time;
	}

	public long getKBSize() {
		return kbsize;
	}

	public void setKBSize(long kbsize) {
		this.kbsize = kbsize;
	};

	public void beforeSetup() {
		this.beforeSetup = System.currentTimeMillis();
	}

	public void afterSetup() {
		this.afterSetup = System.currentTimeMillis();
	}

	public long setupTime() {
		return afterSetup - beforeSetup;
	}
}

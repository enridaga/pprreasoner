package ppr.reasoner;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PPRReasonerObserverImpl implements PPRReasonerObserver {
	private long beforeQuery = 0;
	private long afterQuery = 0;
	private Map<String, Long> beforeResource = new HashMap<String, Long>();
	private Map<String, Long> afterResource = new HashMap<String, Long>();
	private long kbsize;
	private long beforeSetup = 0;
	private long afterSetup = 0;

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
		if(beforeQuery == 0){
			return -1;
		}else if(afterQuery == 0){
			return System.currentTimeMillis() - beforeQuery;
		}else{
			return afterQuery - beforeQuery;
		}
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
		if(beforeSetup == 0){
			return -1;
		}else if(afterSetup == 0){
			return System.currentTimeMillis() - beforeSetup;
		}else{
			return afterSetup - beforeSetup;
		}
	}
}

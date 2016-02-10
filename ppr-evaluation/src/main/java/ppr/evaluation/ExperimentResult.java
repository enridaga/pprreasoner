package ppr.evaluation;

import java.util.Map;
import java.util.Set;

import ppr.reasoner.PPRReasonerObserver;

public interface ExperimentResult {
	public long kbSize();
	public String name();
	public Map<String,Set<String>> policies();
	public PPRReasonerObserver observations();
	public long totalDuration();
}

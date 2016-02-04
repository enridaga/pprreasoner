package ppr.evaluation;

import java.util.Map;
import java.util.Set;

import ppr.reasoner.PPRReasonerListener;

public interface ExperimentResult {
	public long kbSize();
	public String name();
	public Map<String,Set<String>> policies();
	public PPRReasonerListener observations();
	public long totalDuration();
}

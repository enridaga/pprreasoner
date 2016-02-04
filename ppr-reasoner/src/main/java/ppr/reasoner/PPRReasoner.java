package ppr.reasoner;
import java.util.Set;


public interface PPRReasoner {
//	public void load(File file) throws PPRReasonerException;
	public Set<String> policies(String asset) throws PPRReasonerException;
	public PPRReasonerListener observer();
}

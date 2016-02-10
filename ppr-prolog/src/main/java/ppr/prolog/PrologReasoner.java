package ppr.prolog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ppr.reasoner.PPRReasoner;
import ppr.reasoner.PPRReasonerException;
import ppr.reasoner.PPRReasonerObserver;
import ppr.reasoner.PPRReasonerObserverImpl;
import ppr.reasoner.PPRReasonerObservable;
import ubc.cs.JLog.Foundation.iPrologFileServices;
import ubc.cs.JLog.Foundation.jPrologAPI;
import ubc.cs.JLog.Foundation.jPrologFileServices;

public class PrologReasoner implements PPRReasoner, PPRReasonerObservable {
	private static final Logger l = LoggerFactory.getLogger(PrologReasoner.class);
	private File[] sources = new File[0];
	iPrologFileServices fs = new jPrologFileServices();
	private jPrologAPI prolog;
	private boolean metaont = true;
	private PPRReasonerObserver listener = null;

	public void setListener(PPRReasonerObserver listener) {
		this.listener = listener;
	}

	private PrologReasoner(File... sources) throws PPRReasonerException {
		this.sources = sources;
		setListener(new PPRReasonerObserverImpl());
	}

	private PrologReasoner(boolean metaont, File... sources) throws IOException {
		this.metaont = false;
		this.sources = sources;
		setListener(new PPRReasonerObserverImpl());
	}

	public void load(File f) throws PPRReasonerException {
		listener.beforeResource(f.toString());
		l.debug("Loading source {}", f);
		try {
			this.prolog.consultSource(loadStream(new FileInputStream(f)));
		} catch (Exception e) {
			throw new PPRReasonerException(e);
		}
		listener.afterResource(f.toString());
	}

	private String loadStream(InputStream fis) throws IOException {
		InputStream is = new BufferedInputStream(fis);
		try {
			StringBuilder sb = new StringBuilder();
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					sb.append((char) c[i]);
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			count = (count == 0 && !empty) ? 1 : count;
			listener.setKBSize(listener.getKBSize() + count);
			return sb.toString();
		} finally {
			is.close();
		}
	}

	@Override
	public PPRReasonerObserver observer() {
		return listener;
	}

	public void init() throws PPRReasonerException {
		listener.beforeSetup();
		this.prolog = new jPrologAPI("");
		// Setting this to false would throw an exception when a predicate does
		// not have any fact.
		this.prolog.setFailUnknownPredicate(true);
		for (File f : this.sources) {
			load(f);
		}

		if (metaont) {
			listener.beforeResource("meta-ont");
			l.debug("Loading source 'meta-ont'");
			// The ontology file is embedded
			try {
				this.prolog.consultSource(loadStream(PrologReasoner.class.getResourceAsStream("/ontology.pl")));
			} catch (IOException e) {
				throw new PPRReasonerException(e);
			}
			listener.afterResource("meta-ont");
		}

		l.debug("KB is loaded, now querying");
		listener.afterSetup();
	}

	public boolean isTrue(String expression) {
		listener.beforeQuery();
		Hashtable<?, ?> ht;
		ht = prolog.queryOnce(expression);
		listener.afterQuery();
		return (ht == null) ? false : true;
	}

	public Hashtable<?, ?> binding(String expression) {
		listener.beforeQuery();
		Hashtable<?, ?> ht;
		ht = prolog.queryOnce(expression);
		listener.afterQuery();
		return (ht == null) ? new Properties() : ht;
	}

	public List<Hashtable<?, ?>> bindings(String expression) throws FailException {
		listener.beforeQuery();
		List<Hashtable<?, ?>> bindings = new ArrayList<Hashtable<?, ?>>();
		Hashtable<?, ?> ht = null;
		for (ht = prolog.query(expression); true; ht = prolog.retry()) {
			if (ht == null) {
				break;
			} else if (ht.size() == 0) {
				break;
			} else {
				bindings.add(ht);
			}
		}
		listener.afterQuery();
		return bindings;
	}

	protected String _hasPolicy(String node, String policy) {
		return new StringBuilder().append("i_has_policy('").append(node).append("',").append(policy).append(")").append(".").toString();
	}

	protected String _rdfsSubPropertyOf(String left, String right) {
		return new StringBuilder().append("i_rdfs_subproperty_of('").append(left).append("',").append(right).append(")").append(".").toString();
	}

	protected String _connected(String n1, String n2) {
		return new StringBuilder().append("i_connected('").append(n1).append("',").append(n2).append(")").append(".").toString();
	}

	protected Set<String> singleVarSet(List<Hashtable<?, ?>> hts, String var) {
		Set<String> set = new HashSet<String>();
		for (Hashtable<?, ?> b : hts) {
			if (b.size() == 0)
				break;
			set.add((String) b.get(var));
		}
		return set;
	}

	public Set<String> policies(String dn) {
		try {
			return singleVarSet(this.bindings(_hasPolicy(dn, "X")), "X");
		} catch (FailException e) {
			return Collections.emptySet();
		}
	}

	public Set<String> superProperties(String dn) {
		try {
			return singleVarSet(this.bindings(_rdfsSubPropertyOf(dn, "X")), "X");
		} catch (FailException e) {
			return Collections.emptySet();
		}
	}

	public Set<String> subProperties(String dn) {
		try {
			return singleVarSet(this.bindings(_rdfsSubPropertyOf("X", dn)), "X");
		} catch (FailException e) {
			return Collections.emptySet();
		}
	}

	public boolean connected(String dn1, String dn2) {
		return this.isTrue(_connected(dn1, dn2));
	}

	/**
	 * Creates a reasoner by only using passed resources
	 * 
	 * @param sources
	 * @return
	 * @throws IOException
	 */
	public static PrologReasoner create(File... sources) throws IOException {
		return new PrologReasoner(false, sources);
	}

	/**
	 * Creates a reasoner by adding the embedded PPR meta rules
	 * 
	 * @param sources
	 * @return
	 * @throws IOException
	 */
	public static PrologReasoner createCustom(File... sources) throws PPRReasonerException {
		List<File> stack = new ArrayList<File>();
		for (File s : sources) {
			stack.add(s);
		}

		return new PrologReasoner(stack.toArray(new File[stack.size()]));
	}

	/**
	 * Creates a reasoner by adding the embedded datanode file, policy
	 * propagation rules and meta rules.
	 * 
	 * @param sources
	 * @return
	 * @throws IOException
	 */
	public static PrologReasoner createDefault(File... sources) throws PPRReasonerException {
		List<File> stack = new ArrayList<File>();
		try {
			stack.add(new File(PrologReasoner.class.getResource("/relations.pl").toURI()));
			stack.add(new File(PrologReasoner.class.getResource("/rules.pl").toURI()));
		} catch (URISyntaxException e) {
			throw new PPRReasonerException(e);
		}
		for (File s : sources) {
			stack.add(s);
		}
		// to add the embeded ontology.pl file
		return createCustom(stack.toArray(new File[stack.size()]));
	}
}

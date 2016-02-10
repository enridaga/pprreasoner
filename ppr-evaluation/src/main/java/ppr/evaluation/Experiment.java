package ppr.evaluation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import ppr.prolog.Dataflow2Prolog;
import ppr.prolog.PrologReasoner;
import ppr.reasoner.PPRReasoner;
import ppr.reasoner.PPRReasonerException;
import ppr.reasoner.PPRReasonerObserver;
import ppr.spin.SPINReasoner;

public class Experiment {

	public enum Implementation {
		Prolog, SPIN;
		public String print() {
			return StringUtils.rightPad(this.name(), 6);
		}
	}

	public enum KBType {
		ORIGINAL, EVOLVED;
		public String print() {
			return StringUtils.rightPad(this.name(), 8);
		}
	}

	public enum Rules {
		FULL, COMPRESSED;
		public String print() {
			return StringUtils.rightPad(this.name(), 10);
		}
	}

	private static class Cli {
		private PrintStream E = System.out;
		private String[] args = null;
		private Options options = new Options();

		public Cli(String[] args) {
			this.args = args;
			options.addOption("h", "help", false, "Show this help.");
			options.addOption("r", "reasoner", true, "Set the reasoner implementation.");
			options.addOption("c", "compression", true, "Set the compression.");
			options.addOption("d", "dataflow", true,
					"Set the dataflow file.");
			options.addOption("s", "silent", false,
					"Only show statistics.");
			options.addOption("q", "query", true,
					"Set the query (csv list of assets).");
			options.addOption("k", "kbase", true,
					"Set the knowledge base type.");
		}

		/**
		 * Prints help.
		 */
		private void help() {
			String syntax = "java [java-opts] -jar [jarfile] ";
			new HelpFormatter().printHelp(syntax, options);
		}

		/**
		 * Parses command line arguments and acts upon them.
		 */
		public void parse() {
			CommandLineParser parser = new BasicParser();
			CommandLine cmd = null;

			Implementation r;
			KBType k;
			Rules c;
			String d;
			String[] q;
			boolean s;
			try {
				// E.println("Running with args: " + StringUtils.join(args,
				// " "));
				cmd = parser.parse(options, args);
				if (cmd.hasOption('h')) {
					help();
					return;
				}
				if (!cmd.hasOption('k')) {
					// Throw error
					E.println("Parameter 'k' is mandatory. Use -h for details.");
					return;
				}
				if (!cmd.hasOption('q')) {
					// Throw error
					E.println("Parameter 'q' is mandatory. Use -h for details.");
					return;
				}
				if (!cmd.hasOption('c')) {
					// Throw error
					E.println("Parameter 'c' is mandatory. Use -h for details.");
					return;
				}
				if (!cmd.hasOption('r')) {
					// Throw error
					E.println("Parameter 'r' is mandatory. Use -h for details.");
					return;
				}

				r = Implementation.valueOf(cmd.getOptionValue('r'));
				k = KBType.valueOf(cmd.getOptionValue('k'));
				c = cmd.getOptionValue('c').equals("1") ? Rules.COMPRESSED : Rules.FULL;
				d = cmd.getOptionValue('d');
				s = cmd.hasOption('s') ? true : false;
				q = cmd.getOptionValue('q').split(" ");
			} catch (ParseException e) {
				E.println("Failed to parse comand line properties");
				e.printStackTrace();
				help();
				return;
			}
			try {
				Experiment experiment = new Experiment(r, c, k, d);
				ShutdownThread t = new ShutdownThread(experiment);
				Runtime.getRuntime().addShutdownHook(t);
				ExperimentResult result = experiment.perform(q);
				t.completed();
				E.println(resultString(result, s));
			} catch (Exception e) {
				E.println("Failed to execute experiment.");
				e.printStackTrace();
			}
		}
		class ShutdownThread extends Thread{
			private boolean completed = false;
			private Experiment experiment;
			public ShutdownThread(Experiment experiment) {
				this.experiment = experiment;
			}
			public synchronized void completed(){
				this.completed = true;
			}
			@Override
			public void run() {
				if(!this.completed){
					E.println(interruptedResultString(experiment));
				}
			}
		}
		public String resultString(ExperimentResult r, boolean s) {
			StringBuilder sb = new StringBuilder().append(r.name())
					.append("\t").append(r.totalDuration())
					.append("\t").append(r.observations().setupTime())
					.append("\t").append(r.observations().resourcesLoadTime())
					.append("\t").append(r.observations().queryExecutionTime()).
					append('\t').append(r.kbSize()).append('\t');
			if (!s) {
				sb.append(r.policies());
			}
			return sb.toString();
		}

		public String interruptedResultString(Experiment e) {
			StringBuilder sb = new StringBuilder().append(e.getName())
					.append("\t").append(System.currentTimeMillis()-e.getStartTime())
					.append("\t").append(e.getReasoner().observer().setupTime())
					.append("\t").append(e.getReasoner().observer().resourcesLoadTime())
					.append("\t").append(e.getReasoner().observer().queryExecutionTime()).
					append('\t').append(e.getReasoner().observer().getKBSize()).append('\t').append("Interrupted.");
			return sb.toString();
		}
	}

	private Implementation rtype;
	private Rules rulesType;
	private Set<String> dataflows;
	private KBType kbtype;
	private PPRReasoner reasoner;
	private long start;

	public Experiment(Implementation rtype, Rules rulesType, KBType kbtype, String... dataflows) throws PPRReasonerException {
		this.rtype = rtype;
		this.rulesType = rulesType;
		this.kbtype = kbtype;
		this.dataflows = new HashSet<String>();
		this.dataflows.addAll(Arrays.asList(dataflows));
		setup();
	}

	public Implementation getImplementation() {
		return rtype;
	}

	public PPRReasoner getReasoner(){
		return reasoner;
	}

	public long getStartTime(){
		return start;
	}
	private void setup() throws PPRReasonerException {
		String kb = "kb2/";
		// String dataflowFile = dataflow;
		String datanode = kb + "datanode-compact-c";
		String datanodeNoHierarchy = kb + "datanode-no-hierarchy-c";
		String rules = kb;
		switch (rulesType) {
		case FULL:
			rules += "full-rules-c";
			break;
		case COMPRESSED:
			rules += "compressed-rules-c";
			break;
		}
		switch (kbtype) {
		case ORIGINAL:
			datanode += "0";
			rules += "0";
			datanodeNoHierarchy += "0";
			break;
		case EVOLVED:
			datanode += "ALL";
			rules += "ALL";
			datanodeNoHierarchy += "ALL";
			break;
		}

		List<File> dataflowFiles = new ArrayList<File>();
		// List<File> policiesFiles = new ArrayList<File>();
		switch (rtype) {
		case Prolog:
			datanode += ".pl";
			rules += ".pl";
			datanodeNoHierarchy += ".pl";
			for (String dataflowFile : dataflows) {
				if (!new File(dataflowFile + ".pl").exists() && new File(dataflowFile + ".ttl").exists()) {
					// Generate pl files
					try {
						Dataflow2Prolog.toProlog(new File(dataflowFile + ".ttl"), new File(dataflowFile + ".pl"));
						Dataflow2Prolog.toProlog(new File(dataflowFile + ".policies.ttl"), new File(dataflowFile + ".policies.pl"));
					} catch (IOException e) {
						throw new PPRReasonerException(e);
					}
				}
				dataflowFiles.add(new File(dataflowFile + ".pl"));
				dataflowFiles.add(new File(dataflowFile + ".policies.pl"));
			}
			break;
		case SPIN:
			datanode += ".nt";
			datanodeNoHierarchy += ".nt";
			rules += ".nt";
			for (String dataflowFile : dataflows) {
				dataflowFiles.add(new File(dataflowFile + ".ttl"));
				dataflowFiles.add(new File(dataflowFile + ".policies.ttl"));
			}
			break;
		}
		File[] files = null;
		switch (rulesType) {
		case FULL:
			// With SPIN we need to load a different relations file
			if (rtype == Implementation.SPIN) {
				files = new File[] { new File(datanodeNoHierarchy), new File(rules) };
			} else {
				files = new File[] { new File(datanodeNoHierarchy), new File(rules) };
			}
			break;
		case COMPRESSED:
			files = new File[] { new File(datanode), new File(rules) };
			break;
		}

		// We start counting the global duration here.
		start = System.currentTimeMillis();
		// for(File d:files){
		// System.out.println(d.getAbsolutePath());
		// }

		File[] f = (File[]) ArrayUtils.addAll(files, dataflowFiles.toArray());
		switch (rtype) {
		case Prolog:
			reasoner = PrologReasoner.createCustom(f);
			((PrologReasoner) reasoner).init();
			break;
		case SPIN:
			reasoner = SPINReasoner.createCustom(f);
			break;
		}
	}

	public String getName() {
		return StringUtils.join(new Object[] { dataflows, rtype.print(), kbtype.print(), rulesType.print() }, ' ');
	}

	public ExperimentResult perform(String... assets) throws Exception {

		final Map<String, Set<String>> policies = new HashMap<String, Set<String>>();
		for (String ass : assets) {
			Set<String> pol = reasoner.policies(ass);
			policies.put(ass, pol);
		}
		final PPRReasonerObserver listener = reasoner.observer();
		final long total = System.currentTimeMillis() - start;
		return new ExperimentResult() {

			public long totalDuration() {
				return total;
			};

			@Override
			public Map<String, Set<String>> policies() {
				return Collections.unmodifiableMap(policies);
			}

			@Override
			public String name() {
				return getName();
			}

			@Override
			public PPRReasonerObserver observations() {
				return listener;
			}

			@Override
			public long kbSize() {
				return observations().getKBSize();
			}
		};
	}

	public static void main(String[] args) throws Exception {
		new Cli(args).parse();
	}
}

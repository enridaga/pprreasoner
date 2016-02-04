package ppr.evaluation;

import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
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

import ppr.prolog.PrologReasoner;
import ppr.prolog.Dataflow2Prolog;
import ppr.reasoner.PPRReasoner;
import ppr.reasoner.PPRReasonerListener;
import ppr.spin.SPINReasoner;

public class Experiment {

	public enum Implementation {
		Prolog, SPIN
	}

	public enum KBType {
		ORIGINAL, EVOLVED
	}

	public enum Rules {
		FULL, COMPRESSED
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
			options.addOption("a", "annotator", true, "Set the url of the spotlight annotator. Defaults to http://spotlight.dbpedia.org/rest/annotate.");
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
				ExperimentResult result = new Experiment().perform(r, c, k, d, q);
				E.println(resultString(result, s));
			} catch (Exception e) {
				E.println("Failed to execute experiment.");
				e.printStackTrace();
			}
		}

		public String resultString(ExperimentResult r, boolean s) {
			StringBuilder sb = new StringBuilder().append(r.name())
					.append("\t").append(r.totalDuration())
					.append("\t").append(r.observations().resourcesLoadTime())
					.append("\t").append(r.observations().queryExecutionTime()).
					append('\t').append(r.kbSize()).append('\t');
			if(!s){
				sb.append(r.policies());
			}
			return sb.toString();
		}
	}

	public ExperimentResult perform(Implementation rtype, Rules rulesType, KBType kbtype, final String dataflow, String... assets) throws Exception {
		String dataflowFile = dataflow;
		String datanode = "kb1/relations-c";
		String datanodeNoHierarchy = "kb1/flat-relations-c";
		String rules = "kb1/";
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

		switch (rtype) {
		case Prolog:
			datanode += ".pl";
			rules += ".pl";
			if (!new File(dataflowFile + ".pl").exists() && new File(dataflowFile + ".ttl").exists()) {
				// Generate pl file
				Dataflow2Prolog.toProlog(new File(dataflowFile + ".ttl"), new File(dataflowFile + ".pl"));
			}
			dataflowFile = dataflowFile + ".pl";
			break;
		case SPIN:
			datanode += ".nt";
			datanodeNoHierarchy += ".nt";
			rules += ".nt";
			dataflowFile += ".ttl";
			break;
		}
		File[] files = null;
		switch (rulesType) {
		case FULL:
			// With SPIN we need to load a different relations file
			if (rtype == Implementation.SPIN) {
				files = new File[] { new File(datanodeNoHierarchy), new File(rules) };
			} else {
				files = new File[] { new File(rules) };
			}
			break;
		case COMPRESSED:
			files = new File[] { new File(datanode), new File(rules) };
			break;
		}
		long start = System.currentTimeMillis();

		PPRReasoner reasoner = null;
		switch (rtype) {
		case Prolog:
			File[] f = (File[]) ArrayUtils.addAll(files, new File[] { new File(dataflowFile) });
			reasoner = PrologReasoner.createCustom(f);
			((PrologReasoner) reasoner).init();
			break;
		case SPIN:
			File[] f2 = (File[]) ArrayUtils.addAll(files, new File[] { new File(dataflowFile) });
			 reasoner = SPINReasoner.createCustom(f2);
//			SPINReasonerFactory fac = new SPINReasonerFactory(files);
//			fac.setListener(listener);
//			reasoner = fac.createReasoner(new File(dataflowFile));
			break;
		}

		final Map<String, Set<String>> policies = new HashMap<String, Set<String>>();
		for (String ass : assets) {
			Set<String> pol = reasoner.policies(ass);
			policies.put(ass, pol);
		}
		final PPRReasonerListener listener = reasoner.observer();
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
				return StringUtils.join(new Object[] { dataflow, rtype, kbtype, rulesType }, "\t");
			}

			@Override
			public PPRReasonerListener observations() {
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

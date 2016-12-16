package ppr.cli;


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ppr.reasoner.PPRReasoner;
import ppr.spin.SPINReasoner;

public class Main {
	public static void main(String[] args) throws Exception {
		new Cli(args).parse();
	}

	
	private static class Cli {
		private PrintStream E = System.out;
		private String[] args = null;
		private Options options = new Options();

		public Cli(String[] args) {
			this.args = args;
			options.addOption("h", "help", false, "Show this help.");
			options.addOption("d", "data", true, "Include these data file(s). If path(s) are provided, includes all .ttl files under this path. Separate values by comma.");
			options.addOption("g", "gaps", true, "Show policies and relations never mentioned in the input files.");
			options.addOption("a", "asset", true, "Return policies of the given asset(s). Separate values by comma.");
			options.addOption("s", "silent", false, "Do not print out.");
		}

		/**
		 * Prints help.
		 */
		private void help() {
			String syntax = "java [java-opts] -jar [jarfile] ";
			new HelpFormatter().printHelp(syntax, options);
		}

		private void error(){
			System.exit(1);
		}
		/**
		 * Parses command line arguments and acts upon them.
		 */
		public void parse() {
			CommandLineParser parser = new BasicParser();
			CommandLine cmd = null;
			String action = "reason";
			List<String> assets = new ArrayList<String>();
			List<File> files = new ArrayList<File>();
			try {
				cmd = parser.parse(options, args);
				if (cmd.hasOption('h')) {
					help();
					return;
				}
				
				if(cmd.hasOption("d") && cmd.getOptionValue("d") != null){
					String v = cmd.getOptionValue("d");
					String[] vv = v.split(",");
					for(String av : vv){
						File dir = new File(av);
						if(dir.isFile()){
							files.add(dir);
						}else{
							File [] fl = dir.listFiles(new FilenameFilter() {
							    @Override
							    public boolean accept(File dir, String name) {
							        return name.endsWith(".ttl");
							    }
							});
							files.addAll(Arrays.asList(fl));
						}
					}
					
					//files.forEach((f) -> E.println( f));
				}else{
					E.println("Please specify the input data.");
					help();
					error();
				}

				if(cmd.hasOption("g")){
					action = "gap";
				}

				if(cmd.hasOption("a") ){
					String v = cmd.getOptionValue("a");
					String[] vv = v.split(",");
					for(String as : vv){
						assets.add(as);
					}
					if(assets.isEmpty()){
						E.println("Specify assets.");
						help();
						error();
					}
				}
				
			} catch (ParseException e) {
				E.println("Failed to parse comand line properties");
				e.printStackTrace();
				help();
				error();
			}

			try {
				switch(action){
				case "reason":
					List<InputStream> iss = new ArrayList<InputStream>();
					for(File f:files){
						iss.add(new FileInputStream(f));
					}
					iss.add(getClass().getResourceAsStream("compressed-rules-cALL.nt"));
					iss.add(getClass().getResourceAsStream("datanode-compact-cALL.nt"));
					PPRReasoner reasoner = SPINReasoner.createCustom(iss.toArray(new InputStream[files.size()]));
					final Map<String, Set<String>> policies = new HashMap<String, Set<String>>();
					for (String ass : assets) {
						Set<String> pol = reasoner.policies(ass);
						policies.put(ass, pol);
					}
					E.println(policies);
					break;
				case "gap":
					break;
				}
			} catch (Exception e) {
				E.println("Execution failed.");
				e.printStackTrace();
				error();
			}
		}
	}
}

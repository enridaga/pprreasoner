package ppr.prolog;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ppr.reasoner.PPRReasonerException;

public class PrologReasonerTest {
	private static Logger l = LoggerFactory.getLogger(PrologReasonerTest.class);

	@Test
	public void test() throws IOException, URISyntaxException, PPRReasonerException {
		PrologReasoner reasoner = PrologReasoner.createDefault(new File(PrologReasonerTest.class.getResource("/dataflow.pl").toURI()));
		reasoner.init();
		l.info("{}", reasoner.policies("output2"));
	}
	
	@Test
	public void loop1_1() throws Exception {
		PrologReasoner reasoner = PrologReasoner.createDefault(new File(PrologReasonerTest.class.getResource("/loop1.pl").toURI()));
		reasoner.init();
		l.info("{}", reasoner.policies("http://purl.org/datanode/ex/0.2/AEMOO/1#DBPedia"));
	}
	
	@Test
	public void loop1_2() throws Exception {
		PrologReasoner reasoner = PrologReasoner.createDefault(new File(PrologReasonerTest.class.getResource("/loop1.pl").toURI()));
		reasoner.init();
		l.info("{}", reasoner.policies("http://purl.org/datanode/ex/0.2/AEMOO/1#aboutResource"));
	}
}

package ppr.spin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class SPINReasonerTest {
	static final Logger log = LoggerFactory.getLogger(SPINReasonerTest.class);

	private InputStream toInputStream(String filename) throws IOException {
		return getClass().getClassLoader().getResourceAsStream(filename);
	}

	public void perform(String filename) throws Exception {
		SPINReasoner r = SPINReasoner.createDefault(toInputStream(filename));
		StmtIterator it = r.newTriples().listStatements();
		log.info("###### {} #########\nInferred triples:", filename);
		while (it.hasNext()) {
			Statement s = it.next();
			log.info("{}", s);
		}
	}
	
	@Test
	public void test1() throws Exception{
		perform("./dataflow1.ttl");
	}
	
	@Test
	public void test2() throws Exception {
		perform("./dataflow2.ttl");
	}

	@Test
	public void test3() throws Exception {
		perform("./dataflow3.ttl");
	}

	@Test
	public void policies1() throws Exception {
		SPINReasoner r = SPINReasoner.createDefault(toInputStream("./dataflow1.ttl"));
		Iterator<String> p = r.policies("http://purl.org/datanode/ppr/example/iput").iterator();
		while (p.hasNext()) {
			log.info(" > {}", p.next());
		}
	}
}

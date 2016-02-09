package ppr.prolog;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ppr.reasoner.PPRReasonerException;

public class PrologReasonerTest {
	private static Logger l = LoggerFactory.getLogger(PrologReasonerTest.class);

	@Rule
	public TestName n = new TestName();

	private PrologReasoner reasoner;

	@Before
	public void before() throws IOException, PPRReasonerException {
		reasoner = PrologReasoner.createDefault();
		reasoner.init();
	}

	@Test
	public void test() throws Exception {
		_load("/dataflow.pl");
		_assert("output2", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Attribution");
	}

	private void _load(String s) throws PPRReasonerException, URISyntaxException {
		URL u = PrologReasonerTest.class.getResource(s);
		File f = new File(u.toURI());
		reasoner.load(f);
	}

	private void _assert(String item, String... policies) {
		Set<String> result = reasoner.policies(item);
		l.info("{}: {}", n.getMethodName(), result);
		for (String p : policies) {
			String check = new StringBuilder().append("'").append(p).append("'").toString();
			Assert.assertTrue(result.contains(check));
		}
	}

	@Test
	public void loop1_1() throws Exception {
		_load("/loop1.pl");
		_assert("http://purl.org/datanode/ex/0.2/AEMOO/1#DBPedia", "http://www.w3.org/ns/odrl/2/permission http://creativecommons.org/ns#Distribution", "http://www.w3.org/ns/odrl/2/permission http://creativecommons.org/ns#DerivativeWorks", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Attribution", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Notice", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#ShareAlike", "http://www.w3.org/ns/odrl/2/permission http://creativecommons.org/ns#Reproduction");
		_assert("http://purl.org/datanode/ex/0.2/AEMOO/1#aboutResource", "http://www.w3.org/ns/odrl/2/permission http://creativecommons.org/ns#Distribution", "http://www.w3.org/ns/odrl/2/permission http://creativecommons.org/ns#DerivativeWorks", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Attribution", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Notice", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#ShareAlike", "http://www.w3.org/ns/odrl/2/permission http://creativecommons.org/ns#Reproduction");
	}

	@Test
	public void inverseof() throws Exception {
		_load("/inverseof.pl");
		_assert("http://purl.org/datanode/ex/0.2/AEMOO/1#aboutResource", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Attribution", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Notice", "http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#ShareAlike");
	}

	@Test
	public void aemoo1() throws Exception {
		_load("/aemoo1.pl");
		_assert("http://purl.org/datanode/ex/0.2/AEMOO/1#resourceSummary");
	}
}

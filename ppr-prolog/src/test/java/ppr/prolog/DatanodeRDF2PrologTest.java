package ppr.prolog;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.junit.Test;

public class DatanodeRDF2PrologTest {

	@Test
	public void relations() throws FileNotFoundException, URISyntaxException{
		Iterator<String> sss = Dataflow2Prolog.relationsFromTTL(new File(getClass().getClassLoader().getResource("./aemoo1.ttl").toURI()));
		while(sss.hasNext()){
			System.out.println(sss.next());
		}
	}
	
//	@Test
//	public void propagates() throws FileNotFoundException, URISyntaxException{
//		Iterator<String> sss = Dataflow2Prolog.propagatesFromTTL(new File(getClass().getClassLoader().getResource("./propagates.nt").toURI()));
//		while(sss.hasNext()){
//			System.out.println(sss.next());
//		}
//	}
	

	@Test
	public void toProlog() throws FileNotFoundException, URISyntaxException{
		Iterator<String> sss = Dataflow2Prolog.toProlog(new File(getClass().getClassLoader().getResource("./aemoo1.ttl").toURI()));
		while(sss.hasNext()){
			System.out.println(sss.next());
		}
	}
}

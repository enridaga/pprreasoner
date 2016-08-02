package ppr.reasoner;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

public class PoliciesNormalizer {

	public static Model normalize(Model input){
		Model m = ModelFactory.createDefaultModel();
		String PREFIX = "prefix odrl: <http://www.w3.org/ns/odrl/2/> "
				+ "prefix ppr: <http://purl.org/datanode/ppr/ns/> ";
		String WHERE = " WHERE "
				+ "{ "
				+ "?p ppr:asset ?asset . "
				+ " { "
				+ "   ?p ?deontic ?action . "
				+ " } UNION { "
				+ "   ?p ?deontic [ odrl:action ?action ] . "
				+ " } UNION { "
				+ "   ?p odrl:permission [ ?deontic [ odrl:action ?action ] ] . "
				+ " }"
				+ "FILTER (!isBlank(?action)) ."
				+ "FILTER (?deontic = odrl:permission||?deontic = odrl:prohibition||?deontic = odrl:duty) "
				+ "} ";
		String CONSTRUCT = "CONSTRUCT {"
				+ "?p ?deontic ?action . "
				+ "} ";
		String DELETE1 = "DELETE {"
				+ "   ?p ?deontic ?action . "
				+ "} WHERE { ?p ?deontic ?action . FILTER (?deontic = odrl:permission||?deontic = odrl:prohibition||?deontic = odrl:duty) }";
		String DELETE2 = "DELETE {"
				+ "   ?p ?deontic ?c . ?c odrl:action ?action . "
				+ "} WHERE { ?p ?deontic ?c . ?c odrl:action ?action . FILTER (?deontic = odrl:permission||?deontic = odrl:prohibition||?deontic = odrl:duty) }";
		String DELETE3 = "DELETE {"
				+ "   ?p odrl:permission ?c . ?c odrl:duty ?d . ?d odrl:action ?action . "
				+ "} WHERE { ?p odrl:permission ?c . ?c odrl:duty ?d . ?d odrl:action ?action  }";
		QueryExecution exec = QueryExecutionFactory.create(PREFIX + CONSTRUCT + WHERE, input);
		exec.execConstruct(m);
//		for(Statement s : m.listStatements().toSet()){
//			System.out.println(s);
//		}
		//System.out.println("-----------");
		UpdateRequest update = UpdateFactory.create();
		//System.exit(0);
//		System.out.println(PREFIX + DELETE1 );
		update.add(PREFIX + DELETE3 );
//		System.out.println(PREFIX + DELETE2 );
		update.add(PREFIX + DELETE2 );
//		System.out.println(PREFIX + DELETE3 );
		update.add(PREFIX + DELETE1 );
		//update.add(PREFIX + "DELETE {?X ?Y ?Z} WHERE {?X ?Y ?Z} ");
		UpdateAction.execute(update, input);
//		System.out.println(input.size());
		input.add(m);
//		for(Statement s : input.listStatements().toSet()){
//			System.out.println(s);
//		}
		return input;
	}
}

package ppr.spin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.inference.SPINExplanations;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.model.Query;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SPIN;

import ppr.reasoner.PPRReasoner;
import ppr.reasoner.PPRReasonerException;
import ppr.reasoner.PPRReasonerObserver;
import ppr.reasoner.PPRReasonerObserverImpl;
import ppr.reasoner.PPRReasonerObservable;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;

public class SPINReasoner implements PPRReasoner, PPRReasonerObservable {
	private static ClassLoader C = SPINReasoner.class.getClassLoader();
	private static SPINModuleRegistry reg;
	private OntModel ontModel;
	private static final Logger l = LoggerFactory.getLogger(SPINReasoner.class);
	private PPRReasonerObserver listener = null;
	private Object[] resources;
	final SPINExplanations explain = new SPINExplanations();
	private Reasoner reasoner;
	private Model newTriples = null;
	private OntModel data = null;

	public SPINReasoner(File... ttlResources) throws PPRReasonerException {
		resources = ttlResources;
		setListener(new PPRReasonerObserverImpl());
		try {
			init();
		} catch (FileNotFoundException e) {
			throw new PPRReasonerException(e);
		}
	}

	public SPINReasoner(InputStream... ttlResources) throws PPRReasonerException {
		resources = ttlResources;
		setListener(new PPRReasonerObserverImpl());
		try {
			init();
		} catch (FileNotFoundException e) {
			throw new PPRReasonerException(e);
		}
	}

	private void init() throws FileNotFoundException, PPRReasonerException {

		listener.beforeSetup();
		l.debug("Inizialising SPIN reasoner");
		if(reg==null){
			reg = SPINModuleRegistry.get();
			reg.init();
		}
		Model baseModel = ModelFactory.createDefaultModel();
		for (Object f : resources) {
			l.debug(" ... loading {}", f);
			listener.beforeResource(f.toString());
			// Phase 1: load the static data
			InputStream i;
			if (f instanceof File) {
				i = new FileInputStream((File) f);
			} else if (f instanceof InputStream) {
				i = (InputStream) f;
			} else {
				throw new PPRReasonerException("Unsupported resource type: " + f.getClass());
			}
			baseModel.read(i, "", Lang.TURTLE.getLabel());
			listener.afterResource(f.toString());
		}
		// Phase 2: prepare ontology model and add the rule reasoner
		ontModel = JenaUtil.createOntologyModel(OntModelSpec.RDFS_MEM, baseModel);
		Resource datanode = ontModel.createResource("http://purl.org/datanode/ns/Datanode");
		Query rule3 = ARQ2SPIN.parseQuery("CONSTRUCT { "
				+ "?this <http://purl.org/datanode/ppr/ns/policy> ?policy } "
				+ " WHERE {"
				+ "?int ?relatedWith ?this . ?int <http://purl.org/datanode/ppr/ns/policy> ?policy . "
				+ "?relatedWith <http://purl.org/datanode/ppr/ns/propagates> ?policy"
				+ "}", ontModel);
		ontModel.add(datanode, SPIN.rule, rule3);
		l.debug("spin meta rule included");
		//
		reasoner = ReasonerRegistry.getOWLMicroReasoner();
		reasoner.bindSchema(ontModel);
		SPINModuleRegistry.get().registerAll(ontModel, null);
		listener.setKBSize(ontModel.size());

		data = JenaUtil.createOntologyModel(OntModelSpec.RDFS_MEM);
		data.addSubModel(ontModel);

		Model materialized = alignPolicies(data);
		l.debug("{} policies aligned", materialized.size());
		data.addSubModel(materialized);

		// Phase 5: Create and add Model for inferred triples
		this.newTriples = ModelFactory.createDefaultModel();
		data.addSubModel(newTriples);

		// Phase 6: run the rule
		InfModel infModel = ModelFactory.createInfModel(reasoner, data);
		SPINInferences.run(infModel, newTriples, explain, null, false, null);
		listener.afterSetup();
	}

	public void load(File is) throws PPRReasonerException {
		listener.beforeResource(is.toString());
		try {
			loadResource(new FileInputStream(is));
		} catch (FileNotFoundException e) {
			throw new PPRReasonerException(e);
		}
		listener.afterResource(is.toString());
	}

	private void loadResource(InputStream is) {
		OntModel m = JenaUtil.createOntologyModel(OntModelSpec.RDFS_MEM);
		m.read(is, "", Lang.TURTLE.getLabel());
		m.addSubModel(alignPolicies(m));
		data.addSubModel(m);
	}

	@Override
	public PPRReasonerObserver observer() {
		return listener;
	}

	private Model alignPolicies(Model model) {
		// materialize policies of available assets (align
		// policies
		// referred in rules with policies stated in the license)
		QueryExecution exec = QueryExecutionFactory.create("CONSTRUCT { ?asset <http://purl.org/datanode/ppr/ns/policy> ?policy }"
				+ " where {"
				+ "FILTER EXISTS { [] <http://purl.org/datanode/ppr/ns/propagates> ?policy }."
				+ "{ [] <http://www.w3.org/ns/odrl/2/asset> ?asset ; "
				+ "     <http://www.w3.org/ns/odrl/2/prohibition> ?action . "
				+ "?policy <http://www.w3.org/ns/odrl/2/prohibition> ?action . } "
				+ " UNION "
				+ "{ [] <http://www.w3.org/ns/odrl/2/asset> ?asset ; "
				+ "     <http://www.w3.org/ns/odrl/2/permission> ?action . "
				+ " ?policy <http://www.w3.org/ns/odrl/2/permission> ?action . } "
				+ " UNION "
				+ "{ [] <http://www.w3.org/ns/odrl/2/asset> ?asset ; "
				+ "     <http://www.w3.org/ns/odrl/2/duty> ?action . "
				+ " ?policy <http://www.w3.org/ns/odrl/2/duty> ?action . } "
				+ "}", model);
		return exec.execConstruct();
	}

	@Override
	public Set<String> policies(String asset) throws PPRReasonerException {
		listener.beforeQuery();
		l.debug("querying for policies on asset {}", asset);
		QueryExecution exec = QueryExecutionFactory.create(""
				+ "PREFIX odrl: <http://www.w3.org/ns/odrl/2/> "
				+ "PREFIX ppr: <http://purl.org/datanode/ppr/ns/> "
				+ "SELECT ?deo ?act WHERE { <" + asset + ">  ppr:policy ?p . "
				+ "?p ?deo ?act "
				+ "}"
				+ " VALUES (?deo) {"
				+ "	(odrl:permission) "
				+ " (odrl:prohibition) "
				+ " (odrl:duty) "
				+ "}"
				+ "", data);
		ResultSet result = exec.execSelect();
		Set<String> p = new HashSet<String>();
		while (result.hasNext()) {
			QuerySolution qs = result.next();
			p.add(new StringBuilder().append(qs.get("deo")).
					append(" ").
					append(qs.get("act")).toString());
		}
		l.debug("returning {}", p);
		listener.afterQuery();
		listener.setKBSize(data.size());
		return p;
	}

	@Override
	public void setListener(PPRReasonerObserver listener) {
		this.listener = listener;
	}

	public Model newTriples() throws PPRReasonerException {
		return newTriples;
	}

	public SPINExplanations explanations() {
		return explanations();
	}

	public static SPINReasoner createDefault(InputStream dataflow) throws PPRReasonerException {
		InputStream f1 = C.getResourceAsStream("./datanode.0.4.ttl");
		InputStream f2 = C.getResourceAsStream("./propagates.nt");
		return new SPINReasoner(f1, f2, dataflow);
	}

	public static SPINReasoner createDefault(File dataflow) throws PPRReasonerException {
		InputStream f1 = C.getResourceAsStream("./datanode.0.4.ttl");
		InputStream f2 = C.getResourceAsStream("./propagates.nt");
		SPINReasoner r = new SPINReasoner(f1, f2);
		r.load(dataflow);
		return r;
	}

	public static SPINReasoner createDefault() throws PPRReasonerException {
		InputStream f1 = C.getResourceAsStream("./datanode.0.4.ttl");
		InputStream f2 = C.getResourceAsStream("./propagates.nt");
		return new SPINReasoner(f1, f2);
	}

	public static PPRReasoner createCustom(File[] files) throws PPRReasonerException {
		return new SPINReasoner(files);
	}

	public static PPRReasoner createCustom(InputStream[] files) throws PPRReasonerException {
		return new SPINReasoner(files);
	}
}

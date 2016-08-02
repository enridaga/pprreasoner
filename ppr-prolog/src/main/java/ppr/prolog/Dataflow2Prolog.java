package ppr.prolog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.collections4.iterators.IteratorChain;

import ppr.reasoner.PoliciesNormalizer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class Dataflow2Prolog {

	public static synchronized Iterator<String> relationsFromTTL(File ttl) throws FileNotFoundException {
		Model m = loadTTL(ttl);
		return buildHasRelation(m);
	}

	public static synchronized Iterator<String> propagatesFromTTL(File ttl) throws FileNotFoundException {
		Model m = loadTTL(ttl);
		StmtIterator t = m.listStatements();
		while(t.hasNext()){
			System.out.println(t.next());
		}
		return buildPropagates(m);
	}

	public static synchronized Iterator<String> hasPolicyFromTTL(File ttl) throws FileNotFoundException {
		Model m = loadTTL(ttl);
		return buildHasPolicy(m);
	}

	public static synchronized Iterator<String> rdfsSubClassOfFromTTL(File ttl) throws FileNotFoundException {
		Model m = loadTTL(ttl);
		return buildRdfsSubPropertyOf(m);
	}

	@SuppressWarnings("unchecked")
	public static synchronized Iterator<String> toProlog(File ttl) throws FileNotFoundException {
		Model m = loadTTL(ttl);
		Iterator<String> a = buildHasPolicy(m);
		Iterator<String> b = buildHasRelation(m);
		Iterator<String> c = buildPropagates(m);
		Iterator<String> d = buildRdfsSubPropertyOf(m);
		IteratorChain<String> i = new IteratorChain<String>(a, b, c, d);
		return i;
	}
	
	public static synchronized void toProlog(File ttl, File out) throws IOException {
		if(out.exists()){
			out.delete();
		}
		FileWriter fw = new FileWriter(out, true);
		Iterator<String> i = toProlog(ttl);
		while(i.hasNext()){
			fw.write(i.next());
			fw.write('\n');
		}
		fw.close();
	}

	private static synchronized Model loadTTL(File file) throws FileNotFoundException {
		Model m = ModelFactory.createDefaultModel();
		m.read(new FileInputStream(file), "", "TTL");
		m = PoliciesNormalizer.normalize(m);
		return m;
	}

	private static synchronized Iterator<String> buildHasPolicy(final Model m) {
		final StmtIterator i = m.listStatements();
		return new StatementTranslator() {
			@Override
			protected Statement findNextStatement() {
				while (i.hasNext()) {
					Statement s = i.next();
					if (s.getPredicate().getURI().equals("http://purl.org/datanode/ppr/ns/asset")) {
						return s;
					}
				}
				return null;
			}

			protected String translate(Statement st) {
				// Get All policies
				String asset = st.getObject().toString();
				StmtIterator ps = m.listStatements(st.getSubject(), null, (RDFNode) null);
				// Translate statement
				StringBuilder sb = new StringBuilder();
				while (ps.hasNext()) {
					Statement nn = ps.next();
					String p = nn.getPredicate().getURI();
					if ("http://www.w3.org/ns/odrl/2/prohibition".equals(p) ||
							"http://www.w3.org/ns/odrl/2/permission".equals(p) ||
							"http://www.w3.org/ns/odrl/2/duty".equals(p)) {
						sb.append("has_policy(");
						sb.append("'").append(asset).append("','");
						sb.append(p).append(' ').append(nn.getObject().toString()).append("') . ");
						sb.append('\n');
					}
				}
				return sb.toString();
			}
		};
	}

	private static synchronized Iterator<String> buildHasRelation(final Model m) {
		final StmtIterator i = m.listStatements();
		return new StatementTranslator() {
			@Override
			protected Statement findNextStatement() {
				while (i.hasNext()) {
					Statement s = i.next();
					if (s.getPredicate().getNameSpace().equals("http://purl.org/datanode/ns/")) {
						return s;
					}
				}
				return null;
			}

			protected String translate(Statement st) {
				// Translate statement
				StringBuilder sb = new StringBuilder();
				sb.append("has_relation(");
				sb.append("'").append(st.getSubject().toString()).append("','");
				sb.append(st.getObject().toString()).append("','");
				sb.append(st.getPredicate().toString()).append("') . ");
				return sb.toString();
			}
		};
	}

	private static synchronized Iterator<String> buildRdfsSubPropertyOf(final Model m) {
		final StmtIterator i = m.listStatements();
		return new StatementTranslator() {
			@Override
			protected Statement findNextStatement() {
				while (i.hasNext()) {
					Statement s = i.next();
					if (s.getPredicate().getNameSpace().equals("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")) {
						return s;
					}
				}
				return null;
			}

			protected String translate(Statement st) {
				// Translate statement
				StringBuilder sb = new StringBuilder();
				sb.append("rdfs_subproperty_of(");
				sb.append("'").append(st.getSubject().toString()).append("','");
				sb.append(st.getObject().toString()).append("') . ");
				return sb.toString();
			}
		};
	}

	private static synchronized Iterator<String> buildPropagates(final Model m) {
		final StmtIterator i = m.listStatements();

		return new StatementTranslator() {
			@Override
			protected Statement findNextStatement() {
				while (i.hasNext()) {
					Statement s = i.next();
					if (s.getPredicate().getURI().equals("http://purl.org/datanode/ppr/ns/propagates")) {
						return s;
					}
				}
				return null;
			}

			protected String translate(Statement st) {
				
				// Translate statement
				StringBuilder sb = new StringBuilder();
				StmtIterator v = m.listStatements(st.getObject().asResource(), null, (RDFNode) null);
				System.out.println(m.size());
				System.out.println(v.toList().size());
				sb.append("propagates(");
				sb.append("'").append(st.getSubject().toString()).append("','");
				while (v.hasNext()) {
					Statement a = v.next();
					sb.append(a.getPredicate().getURI()).append(" ");
					sb.append(a.getObject().toString());
				}
				sb.append("') . ");
				return sb.toString();
			}
		};
	}

	private abstract static class StatementTranslator implements Iterator<String> {

		Statement next = null;

		@Override
		public boolean hasNext() {
			if (next == null) {
				next = findNextStatement();
			}
			// If it is still null, no more exist
			return (next != null);
		}

		protected abstract Statement findNextStatement();

		protected abstract String translate(Statement st);

		@Override
		public String next() {
			if (next == null) {
				next = findNextStatement();
			}
			// If it is still null, return null
			if (next == null) {
				return null;
			}
			// Translate statement
			String n = translate(next);
			next = null;
			return n;
		}
	}
}

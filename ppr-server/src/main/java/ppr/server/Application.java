package ppr.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application
		extends ResourceConfig implements ServletContextListener {
	private Logger log = LoggerFactory.getLogger(Application.class);

	public Application() {
		packages("ppr.rest");
	}

	public void contextDestroyed(ServletContextEvent arg0) {

	}

	public void contextInitialized(ServletContextEvent arg0) {
		log.info("Initializing context.");
	}

}

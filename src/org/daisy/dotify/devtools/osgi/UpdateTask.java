package org.daisy.dotify.devtools.osgi;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class UpdateTask extends TimerTask {
	private final Framework f;
	private final static Logger logger = Logger.getLogger(UpdateTask.class.getCanonicalName());
	
	public UpdateTask(Framework f) {
		this.f = f;
	}

	@Override
	public void run() {
		for (Bundle b : f.getBundleContext().getBundles()) {
			try {
				URI uri = new URI(b.getLocation());
				if ("file".equals(uri.getScheme())) {
					File f = new File(uri);
					if (f.exists() && f.lastModified()>b.getLastModified()) {
						logger.info("Updating " + f.getName());
						try {
							b.update();
						} catch (BundleException e) {
							logger.log(Level.WARNING, "Failed to update bundle " + b.getSymbolicName(), e);
						}
					}
				}
			} catch (URISyntaxException e) { //never mind
			}
		}
	}
}
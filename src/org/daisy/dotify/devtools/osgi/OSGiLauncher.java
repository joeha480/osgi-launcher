package org.daisy.dotify.devtools.osgi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class OSGiLauncher {

	public static void main(String[] args) throws FileNotFoundException {
		final Logger logger = Logger.getLogger(OSGiLauncher.class.getCanonicalName());
		File plugins;
		if (args.length>=1) {
			plugins = new File(args[0]);
		} else {
			//plugins = findPlugins("bundles");
			plugins = new File("bundles");
		}
		if (!plugins.exists()) {
			logger.severe("Failed to locate bundles directory: " + plugins);
		}
		logger.info("Plugins directory: "+plugins);
		// 1. get a FrameworkFactory using java.util.ServiceLoader.
		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class);
    	//2. create an OSGi framework using the FrameworkFactory
		Iterator<FrameworkFactory> ffi = sl.iterator();
		if (ffi.hasNext()) {
			FrameworkFactory ff = sl.iterator().next();
			//3. start the OSGi framework
			final Framework f = ff.newFramework(new HashMap<String, String>());
			// Add a shutdown hook to shutdown cleanly when System.exit is called
			Runtime.getRuntime().addShutdownHook(new Thread(OSGiLauncher.class.getName() + " Shutdown Hook") {
				@Override
				public void run() {
					try {
						if (f!=null) {
							System.out.println("Stopping framework...");
							f.stop();
							f.waitForStop(0);
						}
					} catch (Exception e) {
						System.out.println("Error stopping framework: " + e);
					}
				}
				
			});
			try {
				f.start();
				// 4. Install your bundle(s).

				for (File jar : plugins.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".jar");
					}})) {
					logger.info("Found jar: " + jar);
					f.getBundleContext().installBundle(jar.toURI().toString());
				}
				//Set a timer to check for bundle updates
				new Timer().schedule(new UpdateTask(f), 0, 15_000);

				//5. Start all the bundles you installed.
				logger.info("OSGi framework started");
				for (Bundle b1 : f.getBundleContext().getBundles()) {
					b1.start();
					logger.fine("Started " + b1.getSymbolicName());
				}
				try {
					//6. Wait for the OSGi framework to shutdown.
					logger.info("Waiting");
					f.waitForStop(0);
					logger.info("Done");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (BundleException  e) {
				e.printStackTrace();
			}
		} else {
			logger.severe("No OSGi framework");
		}
	}

	private final static File findPlugins(String folder) {
		File parent;
		try {
			parent = new File((OSGiLauncher.class.getProtectionDomain().getCodeSource().getLocation()).toURI());
			File plugins;
			do {
				plugins = new File(parent, folder);
				parent = parent.getParentFile();
			} while (!plugins.exists() && parent!=null);
			return plugins;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}

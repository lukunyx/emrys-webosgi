package org.emrys.webosgi.help;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.emrys.webosgi.core.ServiceInitException;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * The help support component for the Framework.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-7-7
 */
public class HelpSupportActivator extends WebComActivator implements
		CommandProvider {
	private static HelpSupportActivator instance;

	public static HelpSupportActivator getInstance() {
		return instance;
	}

	private IWABServletContext servletContext = null;
	private ServiceRegistration serviceRegisteration;
	private ServiceRegistration updateCmdRegistration;

	Set<ITocResProvider> resProviders = new HashSet<ITocResProvider>();

	public void regTocResProvider(ITocResProvider resProvider) {
		resProviders.add(resProvider);
	}

	public void unregTocResProvier(ITocResProvider resProvider) {
		resProviders.remove(resProvider);
	}

	@Override
	public void startComponent(BundleContext context) {
		instance = this;
		super.startComponent(context);
	}

	@Override
	public IWABServletContext getBundleServletContext() {
		if (servletContext == null)
			servletContext = new HttpServiceContext(this);
		return servletContext;
	}

	@Override
	public void initWebConfig() throws ServiceInitException {
		super.initWebConfig();
		registerServices();
	}

	public void registerServices() {
		if (serviceRegisteration == null) {
			// add a update doc command.
			updateCmdRegistration = HelpSupportActivator.getInstance()
					.getBundle().getBundleContext().registerService(
							CommandProvider.class.getName(), this, null);

			final Dictionary d = new Hashtable();
			d.put("http.port", new Integer(8080)); //$NON-NLS-1$
			// set the base URL
			d.put("context.path", "/help"); //$NON-NLS-1$ //$NON-NLS-2$
			d.put("other.info", "org.eclipse.help"); //$NON-NLS-1$ //$NON-NLS-2$
			serviceRegisteration = getContext().registerService(
					HttpService.class.getName(), new HelpHttpService(), d);
			// Start the help server. This will start jetty, not we want.
			// BaseHelpSystem.ensureWebappRunning();
			HelpPlugin.getTocManager().clearCache();
		}
	}

	public void unregisterHttpService() {
		if (serviceRegisteration != null) {
			serviceRegisteration.unregister();
			serviceRegisteration = null;
		}
	}

	@Override
	public String getServiceNSPrefix() {
		return "help";
	}

	public String getHelp() {
		return "---Help Document Support---\n\t-updatedoc update Document Content from component jars\n";
	}

	public void _updatedoc(CommandInterpreter interpreter) {
		try {
			// Clear the Toc contribution caches and refresh.
			HelpPlugin.getTocManager().clearCache();
			interpreter.println("Document server refreshed the data.");
			return;
		} catch (Exception e) {
			e.printStackTrace();
			interpreter.printStackTrace(e);
		}
	}
}

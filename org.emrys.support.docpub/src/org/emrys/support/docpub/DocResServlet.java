package org.emrys.support.docpub;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * 
 * @author Leo Chang
 * @version 2011-7-11
 */
public class DocResServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String pathInfo = req.getPathInfo();

		if (pathInfo == null || pathInfo.length() == 0) {
			super.doPost(req, resp);
			return;
		}

		if (pathInfo.startsWith("/"
				+ Activator.getInstance().getServiceNSPrefix())) {
			pathInfo = pathInfo.replaceFirst("/"
					+ Activator.getInstance().getServiceNSPrefix(), "");
		}

		IPath reqPath = new Path(pathInfo);
		if (reqPath.segment(0).equals("rtopic")) {
			if (reqPath.segment(1).startsWith(
					AutoFindTocProvider.FEATURE_SITE_TOC_CON_PREFIX)) {

			}
			String fullPluginId = reqPath.segment(1);
			fullPluginId = fullPluginId.replace(
					AutoFindTocProvider.FEATURE_SITE_TOC_CON_PREFIX, "");
			String[] segs = fullPluginId.split("\\$");
			if (segs.length == 3) {
				String siteName = segs[0];
				String versionName = segs[1];
				String pluginName = segs[2];

				Iterator<IPath> it = AutoFindTocProvider.docJarPaths.keySet()
						.iterator();
				while (it.hasNext()) {
					IPath bufferedPath = it.next();
					if (bufferedPath.toPortableString().contains(
							new Path(siteName + "/" + versionName).append(
									"plugins").append(pluginName)
									.toPortableString())) {
						File jarFile = bufferedPath.toFile();
						JarFile jar = new JarFile(jarFile);
						try {
							JarEntry entry = jar.getJarEntry(reqPath
									.removeFirstSegments(2).toPortableString());
							if (entry != null) {
								ServletOutputStream out = resp
										.getOutputStream();
								InputStream in = jar.getInputStream(entry);
								while (in.available() > 0) {
									out.write(in.read());
								}
								in.close();
								if (!resp.isCommitted())
									resp.setContentType(req.getSession()
											.getServletContext().getMimeType(
													reqPath.lastSegment()));
								return;
							}
						} finally {
							// Release the lock of the jar file.
							jar.close();
						}
					}
				}
			}
		}

		super.doPost(req, resp);
	}
}

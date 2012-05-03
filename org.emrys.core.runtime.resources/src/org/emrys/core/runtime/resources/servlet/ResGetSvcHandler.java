/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.common.ComActivator;
import org.emrys.common.IComActivator;
import org.emrys.common.util.BundleServiceUtil;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.handlers.AbstractFwkReqeustHandler;
import org.emrys.core.runtime.handlers.IFwkHandlerChain;
import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeecontainer.IBundledServletContext;
import org.emrys.core.runtime.jeecontainer.IOSGiWebContainer;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainer;
import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.core.runtime.jeewrappers.HttpServletResponseWrapper;
import org.emrys.core.runtime.jeewrappers.IBuffferdServletResponse;
import org.emrys.core.runtime.jsp.JspServletPool;
import org.emrys.core.runtime.jsp.OSGIJspServlet;
import org.emrys.core.runtime.resources.ResroucesCom;
import org.emrys.core.runtime.resources.WebResComActivator;
import org.emrys.core.runtime.resources.extension.BaseResource;
import org.emrys.core.runtime.resources.extension.DefinesRoot;
import org.emrys.core.runtime.resources.extension.IResourceVisitController;
import org.emrys.core.runtime.resources.extension.ResFile;
import org.emrys.core.runtime.resources.extension.ResFilter;
import org.emrys.core.runtime.resources.extension.ResFolder;
import org.emrys.core.runtime.resources.extension.ResPublishSVCRegister;
import org.emrys.core.runtime.util.NamedThreadLocal;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


/**
 * The Handler process request for web resources.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-6-1
 */
public class ResGetSvcHandler extends AbstractFwkReqeustHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8681318899660006305L;

	/**
	 * If returned the folder information, the response's head content type
	 * should be as following.
	 */
	public static final String FOLDER_CONTENT_TYPE = "report/folder";// "text/plain"
	// "text/html";

	/**
	 * The request thread local variant: the current servlet request.
	 */
	private static final String RESPONSE_REQ = "REQ";
	/**
	 * The request thread local variant: the current servlet response.
	 */
	private static final String RESPONSE_RESP = "RRESP";
	/**
	 * The request thread local variant: the current bundle.
	 */
	private static final String RESPONSE_CUR_BUNDLE = "CURBUNDLE";
	/**
	 * The request thread local variant: the result inputstream.
	 */
	private static final String RESPONSE_INPUT = "RIN";
	/**
	 * The request thread local variant: the result folder report file.
	 */
	private static final String RESPONSE_FOLDER_REPORT_FILE = "RFRF";
	/**
	 * The request thread local variant: whether the found resource is a folder.
	 */
	private static final String RESPONSE_IS_FOLDER = "RIF";
	/**
	 * The request thread local variant: whether the found resource is a dynimal
	 * file and has been processed.
	 */
	private static final String RESPONSE_DYNIMIC_FILE = "RDF";
	/**
	 * The request thread local variant: the current searching resource root.
	 */
	private static final String RESPONSE_CUR_VISIT_CTRL = "RCRT";

	/**
	 * The request thread local variant: the temporary file's prefix.
	 */
	private static final String STATIC_FILE_NAME = "STFNAME";

	/**
	 * The request thread lcoal variant: whether the resource not modified after
	 * the last request. If ture, this serlvet will return http 304 status.
	 */
	private static final String RES_NO_MODIFIED = "RES_NO_MODIFIED";

	/**
	 * the folder report's charset.
	 */
	private static final String TEXT_CHARSET = "UTF-8";

	/**
	 * The Resource publish register.
	 */
	private final ResPublishSVCRegister resRegister;
	/**
	 * The OSGiJEEContainer's reference.
	 */
	private final IOSGiWebContainer jeeContainerSVC;

	/**
	 * The start time of this servlet. This time be used as the resource last
	 * modified time.
	 */
	private long startTimeMillis = -1L;

	/**
	 * Because the Servlet is a singleton instance for multiple thread to share,
	 * and it cann't have any status variants. Here use a ThreadLocal type to
	 * buffer the status data for each Request Thread.
	 */
	private static final ThreadLocal<Map<String, Object>> threadScope = new NamedThreadLocal<Map<String, Object>>(
			ResGetSvcHandler.class.getName()) {
		@Override
		protected Map<String, Object> initialValue() {
			Map<String, Object> varaintMap = new HashMap<String, Object>();
			// initialize default values.
			varaintMap.put(RESPONSE_IS_FOLDER, Boolean.FALSE);
			varaintMap.put(RESPONSE_DYNIMIC_FILE, Boolean.FALSE);
			return varaintMap;
		}
	};

	private static final int PRIORITY = 150;

	/**
	 * The default constructor.
	 */
	public ResGetSvcHandler(IOSGiWebContainer fwkContainer) {
		super(fwkContainer);
		resRegister = ResPublishSVCRegister.getInstance();
		BundleContext bundleCtx = ResroucesCom.getInstance().getBundle()
				.getBundleContext();
		ServiceReference svcRef = bundleCtx
				.getServiceReference(IOSGiWebContainer.class.getName());
		jeeContainerSVC = (IOSGiWebContainer) bundleCtx.getService(svcRef);
		startTimeMillis = System.currentTimeMillis();
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response, IFwkHandlerChain handlerChain)
			throws IOException, ServletException {
		Object oldBundle = request.getBundle();
		try {
			getFwkContainerHelper().switchReqBundleContext(
					ResroucesCom.getInstance().getBundle());
			// Do resource get service.
			service(request, response);
			int state = response.getState();
			if (state == IBuffferdServletResponse.RESULT_CANCEL) {
				// Reset the state
				if (!request.isInclude())
					response.reset();
				// Do handler chain continuely.
				handlerChain.handle(request, response);
				// FIXME: need to throw exception when included jsp file
				// not found. To support servlet resource, here not do
				// so temporarily.
				// 4xx http status are not exception, not throw servelt
				// exception.
			} else if (/* state % 100 == 4 || */state % 100 == 5) {
				// Construct full exception information when exception
				// occurs.
				String errMsg = response.getErrorMessage();
				StringBuffer errInfoOperStr = new StringBuffer(
						"Exception occured ");
				if (request.isDispatched()) {
					errInfoOperStr.append("when dispatched ");
					if (request.isInclude())
						errInfoOperStr.append("including resource ");
					else
						errInfoOperStr.append("forwarding to resource ");
				} else
					errInfoOperStr.append(" when request for resource ");
				throw new ServletException(errInfoOperStr + "("
						+ request.getRequestURI() + "). "
						+ (errMsg != null ? errMsg : ""));
			}
		} finally {
			// Revert bundle of Top Req
			getFwkContainerHelper().switchReqBundleContext((Bundle) oldBundle);
		}
	}

	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String method = req.getMethod();
		// FIXME: Judge a found resource a jsp file should not so
		// rash. A jsp file may allow POST, PUT, DELETE http methods.
		if (method.equals("GET") || req.getServletPath().endsWith(".jsp")) {
			// The check if the resource not modified after the time request has
			// been done during search process.
			doGet(req, resp);
		} else {
			// Case POST, PUT, DELETE method, skip resource search and return
			// RESULT_CANCEL status.
			resp.sendError(IBuffferdServletResponse.RESULT_CANCEL);
		}
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// Because the serlvet may be invoked many times in one Request
		// thread for Request.include or forward. here buffer the old Thread
		// local variants and revert them at last.
		Object oldRESPONSE_INPUT = threadScope.get().get(RESPONSE_INPUT);
		Object oldRESPONSE_FOLDER_REPORT_FILE = threadScope.get().get(
				RESPONSE_FOLDER_REPORT_FILE);
		Object oldRESPONSE_IS_FOLDER = threadScope.get()
				.get(RESPONSE_IS_FOLDER);

		Object oldReq = threadScope.get().get(RESPONSE_REQ);
		Object oldResp = threadScope.get().get(RESPONSE_RESP);
		Bundle oldBundle = (Bundle) threadScope.get().get(RESPONSE_CUR_BUNDLE);
		Object oldDynimicFile = threadScope.get().get(RESPONSE_DYNIMIC_FILE);
		Object oldSTATIC_FILE_NAME = threadScope.get().get(STATIC_FILE_NAME);
		Object oldRES_NO_MODIFIED = threadScope.get().get(RES_NO_MODIFIED);
		try {
			// Bunffer the current req to thread local map.
			threadScope.get().put(RESPONSE_INPUT, null);
			threadScope.get().put(RESPONSE_FOLDER_REPORT_FILE, null);
			threadScope.get().put(RESPONSE_IS_FOLDER, Boolean.FALSE);

			threadScope.get().put(RESPONSE_CUR_BUNDLE, null);
			threadScope.get().put(RESPONSE_DYNIMIC_FILE, Boolean.FALSE);
			threadScope.get().put(RESPONSE_REQ, req);
			threadScope.get().put(RESPONSE_RESP, resp);

			doSearchResource(req, resp);
		} catch (Exception e) {
			if (e instanceof ServletException)
				throw (ServletException) e;
			if (e instanceof IOException)
				throw (IOException) e;
			throw new ServletException(e);
		} finally {
			// revert the thread load variants.
			threadScope.get().put(RES_NO_MODIFIED, oldRES_NO_MODIFIED);
			threadScope.get().put(RESPONSE_REQ, oldReq);
			threadScope.get().put(RESPONSE_RESP, oldResp);
			threadScope.get().put(RESPONSE_CUR_BUNDLE, oldBundle);
			threadScope.get().put(RESPONSE_DYNIMIC_FILE, oldDynimicFile);
			threadScope.get().put(RESPONSE_INPUT, oldRESPONSE_INPUT);
			threadScope.get().put(RESPONSE_FOLDER_REPORT_FILE,
					oldRESPONSE_FOLDER_REPORT_FILE);
			threadScope.get().put(RESPONSE_IS_FOLDER, oldRESPONSE_IS_FOLDER);
			threadScope.get().put(STATIC_FILE_NAME, oldSTATIC_FILE_NAME);
		}
	}

	/**
	 * Do resource search from published resource repository. If the desiring
	 * file is a dymical file like jsp, it will be process be apache jasper jsp
	 * servlet. If the desiring file is a folder, the folder's structure
	 * information will be returned if its {@link IResourceVisitController}
	 * allows.
	 * 
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	private void doSearchResource(HttpServletRequest req,
			HttpServletResponse resp) throws Exception {
		try {
			String originalPath = req.getServletPath();
			if (originalPath.length() == 0)
				resp.sendError(IBuffferdServletResponse.RESULT_CANCEL);

			// If the first segment of req path is not Web Bundle's urlPattern,
			// add the host
			// bundle's urlPattern before it.
			String path = null;
			try {
				path = ajustOriginalReqPath(originalPath);
			} catch (Exception e) {
				e.printStackTrace();
				throw new ServletException(e);

			}
			// If return null, no need to do following process.
			if (path == null)
				return;

			if (path != null && path.length() > 0) {
				// FIXME: this repositories can be buffer as static to optimize
				// the performance?
				List<DefinesRoot> reps = resRegister
						.getVirtualRepositories(false);
				// Sort the DefinesRoots to let the host web bundle's web
				// Content root be the first be search.
				WebComActivator hostBundleActivator = FwkRuntime.getInstance()
						.getHostBundleActivator();
				if (hostBundleActivator != null) {
					for (int i = 0; i < reps.size(); i++) {
						DefinesRoot root = reps.get(i);
						if (hostBundleActivator.getBundle().equals(
								root.getSourceBundle())) {
							if (root.getResources().size() == 1) {
								if (root
										.getResources()
										.get(0)
										.getPath()
										.equals(
												"/"
														+ hostBundleActivator
																.getServiceNSPrefix())) {
									DefinesRoot tmp = reps.get(0);
									reps.set(0, root);
									reps.set(i, tmp);
									break;
								}
							}
						}
					}
				}

				boolean hostWebRootProecessed = false;
				Iterator<DefinesRoot> it = reps.iterator();
				while (it.hasNext()) {
					DefinesRoot curResRoot = it.next();

					// Buffer the context bundle to thread local variant map.
					Bundle contextBundle = curResRoot.getSourceBundle();
					threadScope.get().put(RESPONSE_CUR_BUNDLE, contextBundle);

					// Get the resource visit controller and judge the
					// authority.
					IResourceVisitController controller = curResRoot
							.getVisitControler();

					// Buffer the current Resource Define Root to thread local
					// variant map. Do not
					// need to judge null.
					threadScope.get().put(RESPONSE_CUR_VISIT_CTRL, controller);

					if (controller == null || controller.canRead(req)) {
						// Search the published resource root in a tree
						// construct for the
						// resource presented by the request path. The result
						// will buffered into
						// thread local variants map.
						if (hostWebRootProecessed && !path.equals(originalPath))
							searchForRes(curResRoot, originalPath);
						else
							searchForRes(curResRoot, path);
						// Check if found a dynimic file and processed by jasper
						// jsp serlvet.
						if (Boolean.TRUE.equals(threadScope.get().get(
								RESPONSE_DYNIMIC_FILE))) {
							// Jsp file not add to found resource buffer and not
							// set last modified time. This means each jsp file
							// request will do a new search.
							return;
						}

						// Check if need to update to client by the thread local
						// mark RES_NO_MODIFIED. The mark was set during search
						// process. If true, no need to consider the found
						// RESPONSE_INPUT variant, coz it's null in this case.
						if (Boolean.TRUE.equals(threadScope.get().get(
								RES_NO_MODIFIED))) {
							// Also need to set Response Mime Type if
							// RES_NO_MODIFIED status.
							String findFileName = originalPath;
							if (findFileName != null) {
								ServletContext ctx = FwkRuntime.getInstance()
										.getJeeContainer().findServletContext(
												contextBundle);
								resp.setContentType(ctx
										.getMimeType(findFileName));
								resp
										.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
								return;
							}
						}

						// If the response content returned in a input stream,
						// write it to HttpResponse. Otherwise, the resource
						// cann't be found and sent CANCEL status.
						InputStream resSrcInput = (InputStream) threadScope
								.get().get(RESPONSE_INPUT);
						if (resSrcInput != null) {
							// Set MIME type before write data.
							// Set mine type of http head according to the file
							// name found.
							// Check if found a directory and its sub resource
							// be reported as a string.
							if (Boolean.TRUE.equals(threadScope.get().get(
									RESPONSE_IS_FOLDER))) {
								resp.setContentType(FOLDER_CONTENT_TYPE
										+ ";charset=" + TEXT_CHARSET);
							} else {
								String findFileName = (String) threadScope
										.get().get(STATIC_FILE_NAME);
								if (findFileName != null) {
									ServletContext ctx = FwkRuntime
											.getInstance().getJeeContainer()
											.findServletContext(contextBundle);
									resp.setContentType(ctx
											.getMimeType(findFileName));
								}
							}

							try {
								// Set the last modified response head. This
								// method should be done before the response be
								// write out.
								maybeSetCacheHeaders(resp);
								int availableByteCount = resSrcInput
										.available();
								if (availableByteCount > 0) {
									try {
										ServletOutputStream output = resp
												.getOutputStream();
										while ((availableByteCount = resSrcInput
												.available()) > 0) {
											byte[] data = new byte[availableByteCount];
											resSrcInput.read(data);
											output.write(data);
										}
									} catch (IOException e) {
										// If client abort, Socket Reset
										// Exception
										// ocurres and ingore
										// it.
										// e.printStackTrace();
									}
								} else {
									resp
											.setStatus(HttpServletResponse.SC_NO_CONTENT);
									// FIXME: If folder or file empty, here
									// write a NULL char to client ,this may
									// cause some exception.
									// if
									// (Boolean.TRUE.equals(threadScope.get().get(RESPONSE_IS_FOLDER)))
									// resp.getWriter().write(0);
									resp.setContentLength(0);
								}
							} finally {
								resSrcInput.close();
								resSrcInput = null;
							}

							return;
						} else {
							// If result inputstream is null because the found
							// folder not allowed to
							// browse. Return 404 error status.
							Boolean isForbbidenBrowsingFolder = (Boolean) threadScope
									.get().get(RESPONSE_IS_FOLDER);
							if (isForbbidenBrowsingFolder) {
								// Send the 404 status
								resp
										.sendError(
												IBuffferdServletResponse.SC_UNAUTHORIZED,
												"not found resource for path:"
														+ req.getRequestURI());
								return;
							}
						}
					}
					hostWebRootProecessed = true;
				}
			}

			// Search all WebComponent's ServletContext by call its
			// getResource(path)
			Collection<ComActivator> activators = FwkRuntime.getInstance()
					.getAllComponentActivators();
			for (Iterator<ComActivator> it = activators.iterator(); it
					.hasNext();) {
				ComActivator activator = it.next();
				if (!(activator instanceof WebResComActivator)
						&& activator instanceof WebComActivator) {
					WebComActivator webComActivator = ((WebComActivator) activator);
					String resPath = originalPath;
					if (originalPath.startsWith("/"
							+ webComActivator.getServiceNSPrefix() + "/")) {
						// No need to remove the current web bundle's prefix,
						// BundledServletContext
						// will do it for us. And the processDynimicFile()
						// method also need this
						// prefix to identifing the jsp file.
						/*
						 * resPath = resPath.replaceFirst("/" +
						 * webComActivator.getServiceNSPrefix(), "");
						 */
						IBundledServletContext ctx = ((WebComActivator) activator)
								.getBundleServletContext();
						URL url = ctx.getResource(resPath);
						if (url != null) {
							String lastSeg = new Path(url.getPath())
									.lastSegment();
							if (lastSeg != null
									&& (lastSeg.endsWith(".jsp") || lastSeg
											.endsWith(".jsf"))) {
								processJspFile(null, resPath, ctx.getBundle());
							} else {
								InputStream resSrcInput = url.openStream();
								int availableByteCount = resSrcInput
										.available();
								if (availableByteCount > 0) {
									while ((availableByteCount = resSrcInput
											.available()) > 0) {
										resp.getWriter().write(
												resSrcInput.read());
									}
								}
								resSrcInput.close();
								resSrcInput = null;
								resp.setContentType(ctx.getMimeType(url
										.getFile()));
							}
							return;
						}
					}
				}
			}

			// Send the customized CANCEL status marking not any resource can be
			// found and service be Canceled.
			resp.sendError(IBuffferdServletResponse.RESULT_CANCEL);
		} finally {
			// delete created folder report file if any.
			File tmpFile = (File) threadScope.get().get(
					RESPONSE_FOLDER_REPORT_FILE);
			if (tmpFile != null)
				tmpFile.deleteOnExit();
		}
	}

	/**
	 * Adjust the request path.
	 * 
	 * @param oReqPath
	 * @return
	 */
	private String ajustOriginalReqPath(String oReqPath) throws Exception {
		IPath path = new Path(oReqPath);
		String firstSeg = path.segment(0);
		if (firstSeg != null) {
			Set<IBundledServletContext> set = jeeContainerSVC
					.getAllBundledServletContext();
			for (IBundledServletContext ctx : set) {
				String webBundlePrefix = ctx.getServletContextName();
				if (firstSeg.equals(webBundlePrefix))
					return oReqPath;
			}
		}

		IBundledServletContext hostServletContext = jeeContainerSVC
				.findHostServletContext();
		// If not Host Bundle, not adjust the req path.
		if (hostServletContext == null)
			return oReqPath;

		if (oReqPath.equals("/")) {
			// Try to load the welcome page from the root of a web bundle, then
			// redirect
			// the response.
			List<String> welcomePages = hostServletContext.getWelcomePages();
			for (Iterator<String> it = welcomePages.iterator(); it.hasNext();) {
				String welcomeFilePath = it.next();
				if (hostServletContext.getResource(welcomeFilePath) != null) {
					BundledHttpServletRequestWrapper req = (BundledHttpServletRequestWrapper) threadScope
							.get().get(RESPONSE_REQ);
					// oReqPath =
					// path.append(welcomeFilePath).toPortableString();
					// req.setPathInfo(oReqPath);
					HttpServletResponse resp = (HttpServletResponse) threadScope
							.get().get(RESPONSE_RESP);
					StringBuffer rawUrl = req.getRequestURL();
					if (rawUrl.charAt(rawUrl.length() - 1) == '/')
						rawUrl.deleteCharAt(rawUrl.length() - 1);

					resp.sendRedirect(rawUrl.append(
							new Path(welcomeFilePath).makeAbsolute()
									.toPortableString()).toString());
					return null;
				}
			}
		}

		String prefix = hostServletContext.getBundleActivator()
				.getServiceNSPrefix();
		return "/" + prefix + oReqPath;
	}

	private void maybeSetCacheHeaders(HttpServletResponse resp) {
		if (!resp.containsHeader("Last-Modified"))
			resp.setDateHeader("Last-Modified", System.currentTimeMillis());
		// Set client cacke header, the max age as 3600000 = 1000 hours.
		// if (!resp.containsHeader("Cache-Control"))
		resp.setHeader("Cache-Control", "public,max-age=3600000");
	}

	/**
	 * Search from each Resource Repository root for desiring resource the path
	 * presented.
	 * 
	 * @param r
	 * @param reqPath
	 * @throws Exception
	 */
	private void searchForRes(DefinesRoot r, String reqPath) throws Exception {
		List<BaseResource> reses = r.getResources();
		for (BaseResource res : reses) {
			Object parentVisitController = threadScope.get().get(
					RESPONSE_CUR_VISIT_CTRL);
			try {
				IResourceVisitController visitController = res
						.getVisitControler();
				// Buffer the current Resource Define Root to thread local
				// variant map. If null, not
				// set
				// to null, just use parent Resource Define's Visit Controller.
				if (visitController != null)
					threadScope.get().put(RESPONSE_CUR_VISIT_CTRL,
							visitController);
				HttpServletRequest req = (HttpServletRequest) threadScope.get()
						.get(RESPONSE_REQ);
				if (visitController == null || visitController.canRead(req)) {
					boolean found = visitRes(res, reqPath, null, null, null);
					if (found)
						break;
				}
			} finally {
				threadScope.get().put(RESPONSE_CUR_VISIT_CTRL,
						parentVisitController);
			}
		}
	}

	/**
	 * Visit each Resource for the give path.
	 * 
	 * @param res
	 * @param reqRawPath
	 * @param reqPath
	 * @param localPath
	 * @param aliasPath
	 * @return
	 * @throws Exception
	 */
	private boolean visitRes(BaseResource res, String reqRawPath,
			IPath reqPath, IPath localPath, IPath aliasPath) throws Exception {
		HttpServletRequest req = (HttpServletRequest) threadScope.get().get(
				RESPONSE_REQ);

		if (reqPath == null)
			reqPath = new Path(reqRawPath);
		if (localPath == null)
			localPath = new Path("/");
		if (aliasPath == null)
			aliasPath = new Path("/");

		String path = res.getPath();
		if (path == null || path.length() == 0)
			return false;

		String alias = res.getAlias();
		if (alias == null || alias.length() == 0)
			alias = path;

		IPath newLocalPath = localPath.append(path);
		IPath newAliasPath = aliasPath.append(alias);
		String quickID = res.getQuickID();
		if (quickID != null && !quickID.startsWith("/"))
			quickID = "/" + quickID;

		if (res instanceof ResFile) {
			if ((quickID != null && quickID.length() > 0 && reqRawPath
					.equals(new Path(res.getQuickID()).toPortableString()))
					|| reqPath.equals(newAliasPath)) {
				File localFile = null;
				// Get file from resolver.
				if (res.getResolver() != null)
					localFile = res.getResolver().resolve(req, path, alias,
							quickID);
				if (localFile == null)
					localFile = newLocalPath.toFile();

				if (localFile != null && localFile.exists()) {
					boolean processedDynimicFile = false;
					String fileExt = new Path(localFile.getAbsolutePath())
							.getFileExtension();
					// If found file is directory, file extension will be null;
					if (fileExt != null
							&& (fileExt.equals("jsp") || fileExt.equals("jsf") || fileExt
									.equals("jspx"))) {
						processedDynimicFile = true;
						// Decide this resource is under the bundle's webContent
						// and need process.
						Bundle currentContextBundle = (Bundle) threadScope
								.get().get(RESPONSE_CUR_BUNDLE);
						if (checkIfWebContent(currentContextBundle, localFile
								.getAbsolutePath())) {
							processJspFile(localFile, newAliasPath
									.toPortableString(), currentContextBundle);
						}
					}

					if (!processedDynimicFile) {
						try {
							// If not modified after the last request, skip
							// output.
							if (!checkLastModifiedTime()) {
								threadScope.get().put(RES_NO_MODIFIED, true);
								return true;
							}

							FileInputStream fin = new FileInputStream(localFile);
							threadScope.get().put(STATIC_FILE_NAME,
									localFile.getName());
							threadScope.get().put(RESPONSE_INPUT, fin);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					} else {
						threadScope.get().put(RESPONSE_DYNIMIC_FILE,
								Boolean.TRUE);
					}

					return true;
				}
			}
		}

		if (res instanceof ResFolder) {
			ResFolder resFolder = ((ResFolder) res);
			List<ResFilter> filters = resFolder.getFilter();

			if ((quickID != null && quickID.length() > 0 && reqPath
					.equals(new Path(res.getQuickID()).toPortableString()))
					|| reqPath.equals(newAliasPath)) {

				// check if folder browse allowed???
				IResourceVisitController visitController = (IResourceVisitController) threadScope
						.get().get(RESPONSE_CUR_VISIT_CTRL);
				if (visitController != null
						&& !visitController.canBrowseFolder(req)) {
					// Buffer the status marking that a folder be found.
					threadScope.get().put(RESPONSE_IS_FOLDER, Boolean.TRUE);
					return true;
				}

				File resolvedFolder = null;
				// Get file from resolver.
				if (res.getResolver() != null)
					resolvedFolder = res.getResolver().resolve(req, path,
							alias, quickID);

				// Case the folder report to large, here create a temporary file
				// to store the
				// content.
				File tmpFile;
				FileOutputStream fout;
				try {
					tmpFile = File.createTempFile("FolderRep"
							+ Thread.currentThread().getId(), "tmp");
					fout = new FileOutputStream(tmpFile);

					PrintWriter writer = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(fout, TEXT_CHARSET)));
					List<BaseResource> subReses = resFolder.getResources();
					if (subReses.size() > 0) {
						for (BaseResource r : subReses) {
							String childAlias = r.getAlias();
							if (childAlias == null)
								childAlias = r.getPath();
							IPath reqPathRelResPath = FileUtil.makeRelativeTo(
									newAliasPath.append(childAlias), reqPath);
							writer.println(newAliasPath
									.append(reqPathRelResPath));
							reportVirtualRes(r, writer, newAliasPath,
									newLocalPath, reqPath, filters);
						}
					} else {
						// If not sub resource extension exists, get all real
						// resource in local
						// file
						// system.
						File localFile = null;
						if (resolvedFolder != null) {
							if (resolvedFolder.isDirectory()
									&& resolvedFolder.exists())
								localFile = resolvedFolder;
						} else {
							String contextPath = req.getSession()
									.getServletContext().getRealPath("/");
							localFile = new Path(contextPath).append(
									newLocalPath).toFile();
						}

						if (localFile != null && localFile.exists()) {
							reportLocalRes(localFile, writer, reqPath,
									newAliasPath, filters);
						}
					}

					writer.flush();
					writer.close();

					// Buffer the status marking that a folder be found.
					threadScope.get().put(RESPONSE_IS_FOLDER, Boolean.TRUE);
					// buffer the result stream to thread local variant.
					threadScope.get().put(RESPONSE_INPUT,
							new FileInputStream(tmpFile));
					// buffer the create temporary file to thread local variant
					// and delete it at
					// last.
					threadScope.get().put(RESPONSE_FOLDER_REPORT_FILE, tmpFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			} else {
				// Only when the pfefix of current Resource is the required
				// path, search sun
				// resource.
				if ((quickID != null && quickID.length() > 0 && new Path(res
						.getQuickID()).isPrefixOf(reqPath))
						|| newAliasPath.isPrefixOf(reqPath)) {
					List<BaseResource> subReses = ((ResFolder) res)
							.getResources();
					if (subReses.size() > 0) {
						for (BaseResource r : subReses) {
							boolean ok = visitRes(r, reqRawPath, reqPath,
									newLocalPath, newAliasPath);
							if (ok)
								return true;
						}
					} else {
						File resolvedFolder = null;
						// Get file from resolver.
						if (res.getResolver() != null)
							resolvedFolder = res.getResolver().resolve(req,
									path, alias, quickID);

						if (resolvedFolder == null) {
							String contextPath = req.getSession()
									.getServletContext().getRealPath("/");
							resolvedFolder = new Path(contextPath).append(
									newLocalPath).toFile();
						}
						boolean found = searchLocalRes(resolvedFolder, reqPath,
								newAliasPath, filters);
						if (found)
							return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Report the virtual resource in of folder.
	 * 
	 * @param r
	 * @param writer
	 * @param newAliasPath
	 * @param reqPath
	 * @param parentLocalPath
	 * @param filters
	 */
	private void reportVirtualRes(BaseResource res, PrintWriter writer,
			IPath parentAliasPath, IPath rawReqPath, IPath parentLocalPath,
			List<ResFilter> filters) {
		if (!(res instanceof ResFolder))
			return;
		ResFolder resFolder = (ResFolder) res;

		HttpServletRequest req = (HttpServletRequest) threadScope.get().get(
				RESPONSE_REQ);
		String path = resFolder.getPath();
		if (path == null || path.length() == 0)
			return;

		String alias = res.getAlias();
		String quickID = res.getQuickID();

		if (alias == null || alias.length() == 0)
			alias = path;

		IPath newAliasPath = parentAliasPath.append(alias);
		IPath newLocalPath = parentLocalPath.append(path);

		File resolvedFolder = null;
		// Get file from resolver.
		if (res.getResolver() != null)
			resolvedFolder = res.getResolver().resolve(req, path, alias,
					quickID);

		List<BaseResource> subReses = resFolder.getResources();
		if (resolvedFolder == null && subReses != null && subReses.size() > 0) {
			for (BaseResource r : subReses) {
				String childAlias = r.getAlias();
				if (childAlias == null)
					childAlias = r.getPath();
				IPath reqPathRelResPath = FileUtil.makeRelativeTo(newAliasPath
						.append(childAlias), rawReqPath);
				writer.println(newAliasPath.append(reqPathRelResPath));
				reportVirtualRes(r, writer, newAliasPath, newLocalPath,
						rawReqPath, filters);
			}
		} else {
			// If not sub resource extension exists, get all real resource in
			// local file
			// system.
			File localFile = null;
			if (resolvedFolder != null) {
				if (resolvedFolder.isDirectory() && resolvedFolder.exists())
					localFile = resolvedFolder;
			} else {
				String contextPath = req.getSession().getServletContext()
						.getRealPath("/");
				localFile = new Path(contextPath).append(newLocalPath).toFile();
			}

			if (localFile != null && localFile.exists()) {
				reportLocalRes(localFile, writer, rawReqPath, newAliasPath,
						filters);
			}
		}
		writer.flush();
	}

	/**
	 * Report a local resource in a folder.
	 * 
	 * @param parentFile
	 * @param writer
	 * @param rawReqPath
	 * @param aliasPath
	 * @param filters
	 */
	private void reportLocalRes(File parentFile, PrintWriter writer,
			IPath rawReqPath, IPath aliasPath, List<ResFilter> filters) {
		File[] cfs = parentFile.listFiles();
		if (cfs.length > 0) {
			for (File f : cfs) {
				if (!f.exists())
					continue;
				// Let real file name as alias name here.
				if (filterName(f.getName(), filters)) {
					IPath newAliasPath = aliasPath
							.append(new Path(f.getName()));
					IPath reqPathRelResPath = FileUtil.makeRelativeTo(
							newAliasPath, rawReqPath);
					writer.println(reqPathRelResPath.toPortableString());
					if (f.isDirectory()) {
						reportLocalRes(f, writer, rawReqPath, newAliasPath,
								null);
					}
				}
			}
		}
		writer.flush();
	}

	/**
	 * Search resource form resources in local file system.
	 * 
	 * @param parentFile
	 * @param rawReqPath
	 * @param aliasPath
	 * @param filters
	 * @return
	 * @throws Exception
	 */
	private boolean searchLocalRes(File parentFile, IPath rawReqPath,
			IPath aliasPath, List<ResFilter> filters) throws Exception {
		File[] cfs = parentFile.listFiles();
		if (cfs == null)
			return false;
		for (File f : cfs) {
			if (!f.exists())
				continue;

			// Let real file name as alias name here.
			IPath newAliasPath = aliasPath.append(new Path(f.getName()));

			if (filterName(f.getName(), filters)) {
				if (rawReqPath.equals(newAliasPath)) {
					if (f.isDirectory()) {
						File tmpFile;
						FileOutputStream bo;
						try {
							tmpFile = File.createTempFile("FolderRep"
									+ Thread.currentThread().getId(), "tmp");
							bo = new FileOutputStream(tmpFile);
						} catch (IOException e1) {
							e1.printStackTrace();
							continue;
						}

						PrintWriter writer;
						try {
							writer = new PrintWriter(new BufferedWriter(
									new OutputStreamWriter(bo, TEXT_CHARSET)));
							reportLocalRes(f, writer, rawReqPath, newAliasPath,
									null);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}

						threadScope.get().put(RESPONSE_IS_FOLDER, Boolean.TRUE);
						threadScope.get().put(RESPONSE_FOLDER_REPORT_FILE,
								tmpFile);
						try {
							threadScope.get().put(RESPONSE_INPUT,
									new FileInputStream(tmpFile));
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					} else {
						boolean processedDynimicFile = false;
						String fileExt = new Path(f.getAbsolutePath())
								.getFileExtension();
						// If found file is directory, file extension will be
						// null;
						if (fileExt != null
								&& (fileExt.equals("jsp")
										|| fileExt.equals("jsf") || fileExt
										.equals("jspx"))) {
							// Decide this resource is under the bundle's
							// webContent and need
							// process.
							Bundle currentContextBundle = (Bundle) threadScope
									.get().get(RESPONSE_CUR_BUNDLE);
							if (checkIfWebContent(currentContextBundle, f
									.getAbsolutePath())) {
								processedDynimicFile = true;
								processJspFile(f, newAliasPath
										.toPortableString(),
										currentContextBundle);
								// Buffer the status marking that the dynimic
								// file found and be
								// processed successfully.
								threadScope.get().put(RESPONSE_DYNIMIC_FILE,
										Boolean.TRUE);
							}
						}

						if (!processedDynimicFile) {
							try {
								// If not modified after the last request, skip
								// output.
								if (!checkLastModifiedTime()) {
									threadScope.get()
											.put(RES_NO_MODIFIED, true);
									return true;
								}

								threadScope.get().put(STATIC_FILE_NAME,
										f.getName());
								threadScope.get().put(RESPONSE_INPUT,
										new FileInputStream(f));
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							}
						}
					}
					return true;
				} else if (f.isDirectory()) {
					// Only when the pfefix of current Resource is the required
					// path, search sun
					// resource.
					if (newAliasPath.isPrefixOf(rawReqPath)) {
						boolean found = searchLocalRes(f, rawReqPath,
								newAliasPath, null);
						if (found)
							return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Check if the found resource not modified since the time request
	 * delivered. If true, http 304 status will be returned directly.
	 * 
	 * @return
	 */
	private boolean checkLastModifiedTime() {

		HttpServletRequest curReq = (HttpServletRequest) threadScope.get().get(
				RESPONSE_REQ);
		long ifLastModifiedSince = curReq.getDateHeader("If-Modified-Since");
		if (ifLastModifiedSince < 0)
			return true;

		IResourceVisitController curVisitController = (IResourceVisitController) threadScope
				.get().get(RESPONSE_CUR_VISIT_CTRL);
		long lastMidifiedTimeMills = -1;

		if (curVisitController == null
				|| curVisitController.getLastModifiedTimeMillis() <= 0)
			lastMidifiedTimeMills = startTimeMillis;
		else
			lastMidifiedTimeMills = curVisitController
					.getLastModifiedTimeMillis();
		return ifLastModifiedSince < lastMidifiedTimeMills;
	}

	/**
	 * Because only WebContent resource can be a dynamic resource like jsp, jsp,
	 * jspx, which should be forward to further process. This method to check
	 * this before found a target resource and decide whether deliver it to
	 * further servlet.
	 * 
	 * @param currentContextBundle2
	 * @param absolutePath
	 * @return
	 */
	private boolean checkIfWebContent(Bundle bundle, String absolutePath) {
		try {
			IComActivator activator = BundleServiceUtil
					.getBundleActivator(bundle);
			if (activator instanceof WebComActivator) {
				WebComActivator webBundleActivator = (WebComActivator) activator;
				// unzip webContent file form jar.
				File tmpWebRoot = webBundleActivator
						.getResolvedWebContentRoot(false);
				if (tmpWebRoot != null)
					return new Path(tmpWebRoot.getAbsolutePath())
							.isPrefixOf(new Path(absolutePath));
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}

		return false;
	}

	/**
	 * If a found file is a dynimical file liks jsp, jsf. etc, let apache jasper
	 * servler to process it.
	 * 
	 * @param jspFile
	 * @param virtualPath
	 * @param targetBundle
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	private boolean processJspFile(File jspFile, String virtualPath,
			Bundle targetBundle) throws Exception {

		WebComActivator activator = (WebComActivator) FwkRuntime.getInstance()
				.getBundleActivator(targetBundle.getBundleId());

		// here should not remove the bundle prefix before the virtual path.
		// if
		// servlet path is the same, the jasper may not compile the jsp just
		// use the form class
		// file. here set the servlet name to virtual name to hint that
		// these are different. The
		// BundleServletContext will remove the prefix for us.
		// get the target bundle's servlet config.
		/*
		 * if (newResPath.startsWith("/" + activator.getServiceNSPrefix() +
		 * "/")) newResPath = newResPath.replaceFirst("/" +
		 * activator.getServiceNSPrefix() + "/", "/");
		 */

		// Get a poolled servlet to process jsp file.
		OSGIJspServlet jspServlet = JspServletPool.getInstance(targetBundle);
		// ClassLoader c = jspServlet.getClass().getClassLoader();
		// if newResPath is the same, the jasper may not compile the jsp
		// just use the form class
		// file. here set the servlet name to virtual name to hint that
		// these are different.
		// get the target bundle's servlet config.
		// ServletConfig newConfig = new
		// BundledServletConfig(targetBundle,getJspServletConfig(),
		// virtualPath);
		// jspServlet.init(newConfig);

		BundledHttpServletRequestWrapper topReq = (BundledHttpServletRequestWrapper) jeeContainerSVC
				.getReqThreadVariants().get(OSGiJEEContainer.THREAD_V_REQUEST);

		HttpServletRequest req = (HttpServletRequest) threadScope.get().get(
				RESPONSE_REQ);
		ServletResponse resp = (ServletResponse) threadScope.get().get(
				RESPONSE_RESP);

		BundledHttpServletRequestWrapper wrapper = BundledHttpServletRequestWrapper
				.getHttpServletRequestWrapper(req, targetBundle);

		Bundle oldBundle = topReq.getBundle();
		String oldServletPath = topReq.getServletPath();
		String oldPathInfo = topReq.getPathInfo();
		try {
			// Switch to another bundled servelt request.
			// No need to swith thread context class loader, coz Jsp Servlet's
			// class loader has been changed.
			topReq.setBundle(targetBundle);
			// wrapper.setBundle(targetBundle);
			wrapper.setServletPath(virtualPath);
			wrapper.setPathInfo(null);
			jspServlet.service(wrapper, resp);
			jspServlet.destroy();
		} finally {
			if (topReq.equals(wrapper)) {
				topReq.setBundle(oldBundle);
				topReq.setServletPath(oldServletPath);
				topReq.setPathInfo(oldPathInfo);
			} else {
				BundledHttpServletRequestWrapper.releaseRequestWrapper(wrapper);
			}
		}

		return true;
	}

	/**
	 * Filter resource by a folder's filters.
	 * 
	 * @param name
	 * @param filters
	 * @return
	 */
	private boolean filterName(String name, List<ResFilter> filters) {
		if (filters == null || filters.size() == 0)
			return true;
		else {
			boolean result = true;
			for (ResFilter filter : filters) {
				boolean include = filter.isIncluded();
				String patternStr = filter.getPattern();
				if (patternStr != null && patternStr.length() > 0) {
					Pattern p = Pattern.compile(patternStr);
					Matcher m = p.matcher(name);
					result = m.matches();
				} else
					result = include;
				if (result == false)
					return false;
			}
		}
		return true;
	}
}

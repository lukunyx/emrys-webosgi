package org.emrys.webosgi.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.emrys.webosgi.common.CommonActivator;
import org.osgi.framework.Bundle;

/**
 * The utilitarian class providing many convenient method to manipulate file.
 * 
 * @author Leo Chang
 * @version 2010-10-22
 */
public class FileUtil {
	/**
	 * Write input stream to a file.
	 * 
	 * @param in
	 * @param targetFile
	 * @throws Exception
	 */
	public static void writeToFile(InputStream in, File targetFile)
			throws Exception {
		createFile(targetFile, false);
		FileOutputStream fout = new FileOutputStream(targetFile);
		while (in.available() > 0) {
			int data = in.read();
			fout.write(data);
		}
		fout.close();
		in.close();
	}

	public static File getBundleJar(Bundle bundle) throws IOException {
		URL bundleFileUrl = FileLocator.resolve(bundle.getEntry("/"));
		return new Path(bundleFileUrl.getPath()).toFile();
	}

	/**
	 * Unzip jar file to target directory.
	 * 
	 * @param file
	 * @param srcRelPath
	 *            relative path to jar root. if not null, only unzip this path
	 *            in jar to target file, otherwise, unzip all content. This path
	 *            should not start with '/'.
	 * @param targetFile
	 *            if not exists, automatically create.
	 * @throws IOException
	 */
	public static void unZipFile(File file, String srcRelPath, File targetFile)
			throws IOException {
		if (!targetFile.exists() && !createFile(targetFile, true))
			throw new IOException("Cann't create target file:"
					+ targetFile.getAbsolutePath());

		if (targetFile.isFile())
			throw new IllegalArgumentException("target file not a directory");

		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
		} catch (Exception e1) {
			// e1.printStackTrace();
			throw new IllegalArgumentException("file not a zip file.");
		}

		IPath srcPath = null;
		if (srcRelPath == null || srcRelPath.length() == 0
				|| srcRelPath.equals("/"))
			srcPath = null;
		else {
			// Remove the first '/' if any.
			if (srcRelPath.startsWith("/"))
				srcRelPath = srcRelPath.substring(1);
			srcPath = new Path(srcRelPath);
		}

		ZipEntry zipEntry;
		Enumeration<? extends ZipEntry> enumer;
		BufferedInputStream is = null;
		FileOutputStream fos = null;
		byte data[] = new byte[1024];
		try {
			if (zipFile.size() == 0)
				return;

			enumer = zipFile.entries();
			while (enumer.hasMoreElements()) {
				zipEntry = enumer.nextElement();
				if (srcPath != null
						&& !srcPath.isPrefixOf(new Path(zipEntry.getName())))
					continue;

				if (zipEntry.isDirectory())
					continue;
				is = new BufferedInputStream(zipFile.getInputStream(zipEntry));
				// System.out.println(xJarEntry.getName());

				IPath relPath = new Path(zipEntry.getName());
				if (srcPath != null && !srcPath.equals(relPath))
					relPath = makeRelativeTo(relPath, srcPath);
				// If the src file path is a file, use the file's name as
				// relative path.
				if (srcPath != null && srcPath.equals(relPath))
					relPath = new Path(relPath.lastSegment());

				File newFile = new File(new Path(targetFile.getAbsolutePath())
						.append(relPath).toPortableString());
				FileUtil.createFile(newFile, false);
				fos = new FileOutputStream(newFile);
				int len = 0;
				while ((len = is.read(data)) != -1) {
					fos.write(data, 0, len);
				}
				fos.flush();
				fos.close();
				fos = null;

				// Copy the last modified time.
				long modifiedTime = zipEntry.getTime();
				if (modifiedTime > 0)
					newFile.setLastModified(modifiedTime);
			}
		} finally {
			try {
				zipFile.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * To support org.eclipse.equinox.common 3.4, here supply our
	 * IPath.makeRelativeTo(IPath base) method in 3.5.
	 * 
	 * Returns a path equivalent to this path, but relative to the given base
	 * path if possible.
	 * <p>
	 * The path is only made relative if the base path if both paths have the
	 * same device and have a non-zero length common prefix. If the paths have
	 * different devices, or no common prefix, then this path is simply
	 * returned. If the path is successfully made relative, then appending the
	 * returned path to the base will always produce a path equal to this path.
	 * </p>
	 * 
	 * @param path
	 *            the source path to make relative.
	 * @param base
	 *            The base path to make this path relative to
	 * @return A path relative to the base path, or this path if it could
	 */
	public static IPath makeRelativeTo(IPath path, IPath base) {
		// can't make relative if devices are not equal
		String device = path.getDevice();
		if (device != base.getDevice()
				&& (device == null || !device
						.equalsIgnoreCase(base.getDevice())))
			return path;
		int commonLength = path.matchingFirstSegments(base);
		final int differenceLength = base.segmentCount() - commonLength;
		final int newSegmentLength = differenceLength + path.segmentCount()
				- commonLength;
		if (newSegmentLength == 0)
			return Path.EMPTY;
		String[] newSegments = new String[newSegmentLength];
		// add parent references for each segment different from the base
		Arrays.fill(newSegments, 0, differenceLength, ".."); //$NON-NLS-1$
		// append the segments of this path not in common with the base
		System.arraycopy(path.segments(), commonLength, newSegments,
				differenceLength, newSegmentLength - differenceLength);
		StringBuffer pathStr = new StringBuffer(newSegments[0]);
		for (int i = 1; i < newSegmentLength; i++) {
			pathStr.append(Path.SEPARATOR);
			pathStr.append(newSegments[i]);
			if (i == newSegmentLength - 1 && path.hasTrailingSeparator())
				pathStr.append(Path.SEPARATOR);
		}
		return new Path(null, pathStr.toString());
	}

	/**
	 * Convert a URL to local file resource. This means if this url is local
	 * file, it's easy to understand. But it this url's protocol is "http" or
	 * "ftp", "jar", "zip", a local file or folder indicated by this url will be
	 * created and this temporary file or folder's contents will obtained by the
	 * protocol, for example, if "http", this method will request form web and
	 * the folder construction will remain.
	 * 
	 * @param fileUrl
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public static File convert2LocalFileByURL(URL fileUrl,
			IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor = new NullProgressMonitor();

		if (fileUrl.getProtocol().equals("file")) {
			File tiLocalFile = new File(fileUrl.getFile());
			if (tiLocalFile.exists())
				return tiLocalFile;
		}

		if (fileUrl.getProtocol().equals("http")) {

			try {
				fileUrl.openConnection();
				InputStream in = fileUrl.openStream();
				String urlFileName = fileUrl.getFile();
				String path = fileUrl.getQuery();
				if (urlFileName != null && urlFileName.length() > 0) {
					int index = urlFileName.lastIndexOf(".");
					String fName = urlFileName.substring(0, index);
					String fExt = (index == -1 || index == urlFileName.length() - 1) ? ".tmp"
							: urlFileName.substring(index + 1);

					File tmpLocalTiFile = File.createTempFile(
					/* "BTOOLS_TMP_" + */fName
					/* + System.currentTimeMillis() */, fExt, getTmpFileDir(
							path, true));
					try {
						writeToFile(in, tmpLocalTiFile);
					} catch (Exception e) {
						// e.printStackTrace();
					}

					return tmpLocalTiFile;
				} else {
					// Case this url is a folder, not a web file resource.
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(in, "UTF-8"));
					String line = null;
					while ((line = reader.readLine()) != null) {
						try {
							URL url = new URL(line);
							convert2LocalFileByURL(url, monitor);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					reader.close();
					in.close();
					return getTmpFileDir(path, true);
				}
			} catch (IOException e) {
				// e.printStackTrace();
				throw new CoreException(new Status(Status.ERROR,
						CommonActivator.ID,
						"Local file for Template Information File of"
								+ fileUrl.toString() + "cann't be retrieved!",
						e));
			}
		}

		throw new CoreException(new Status(Status.ERROR, CommonActivator.ID,
				"Local file for Template Information File of"
						+ fileUrl.toString() + "cann't be retrieved!"));
	}

	private static File getTmpFileDir(String path, boolean forceCreate) {
		File file = CommonActivator.getInstance().getStateLocation().append(
				new Path(path)).toFile();
		if (!file.exists() && forceCreate)
			file.mkdirs();

		return file;
	}

	/**
	 * Create a file or a folder and its parent folders if need.
	 * 
	 * @param target
	 * @param isDirectory
	 * @throws IOException
	 */
	public static boolean createFile(File target, boolean isDirectory)
			throws IOException {
		if (!target.exists() || (isDirectory && target.isFile())
				|| (!isDirectory && target.isDirectory())) {

			if (createFile(target.getParentFile(), true)) {
				if (isDirectory)
					return target.mkdir();
				else
					return target.createNewFile();
			}
		} else {
			return true;
		}

		return false;
	}

	public static interface IFileVisitor {
		/**
		 * @param f
		 * @return whether to visit child files if the current file is a
		 *         directory.
		 */
		boolean visit(File f);
	}

	public static interface IFileFilter {
		boolean intersted(File f);
	}

	/**
	 * Visit each child file of root file.
	 * 
	 * @param root
	 *            the root to search for files to visit, include this root file.
	 * @param filter
	 * @return
	 */
	public static void visitFiles(File file, IFileVisitor visitor) {
		Assert.isNotNull(visitor);
		if (file.exists()) {
			if (file.isDirectory()) {
				if (visitor.visit(file)) {
					File[] fileList = file.listFiles();
					for (int i = 0; i < fileList.length; i++) {
						visitFiles(fileList[i], visitor);
					}
				}
			} else
				visitor.visit(file);
		}
	}

	/**
	 * Delete the given file and its all sub contents files.
	 * 
	 * @param file
	 * @param filter
	 * @return
	 */
	public static boolean deleteAllFile(File file, IFileFilter filter) {
		boolean ret = false;
		if (file.exists()) {
			if (file.isDirectory()) {
				if (filter == null || filter.intersted(file)) {
					filter = null;
				}
				File[] fileList = file.listFiles();
				for (int i = 0; i < fileList.length; i++) {
					deleteAllFile(fileList[i], filter);
				}
			}

			if (filter == null || filter.intersted(file)) {
				file.delete();
			}
		}

		return ret;
	}

	/**
	 * Copy source file to target file, Caution: this method will override the
	 * target file.
	 * 
	 * @param sourceFile
	 * @param targetFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File targetFile)
			throws IOException {
		if (!sourceFile.exists())
			throw new IOException("Source file to copy("
					+ sourceFile.getAbsolutePath() + ") not exists!");
		// Create target file if need.
		// FIXME: Maybe override some file, caution.
		boolean targetFileExists = targetFile.exists();
		if (!targetFileExists && !createFile(targetFile, false))
			throw new IOException("Cann't create new file:"
					+ targetFile.getAbsolutePath());

		FileInputStream input = new FileInputStream(sourceFile);
		BufferedInputStream inBuff = new BufferedInputStream(input);

		FileOutputStream output = new FileOutputStream(targetFile);
		BufferedOutputStream outBuff = new BufferedOutputStream(output);

		byte[] b = new byte[1024 * 5];
		int len;
		while ((len = inBuff.read(b)) != -1) {
			outBuff.write(b, 0, len);
		}

		outBuff.flush();
		inBuff.close();
		outBuff.close();
		output.close();
		input.close();

		// Copy the last modified time, if the target file is new created.
		if (!targetFileExists) {
			long modifiedTime = sourceFile.lastModified();
			if (modifiedTime > 0)
				targetFile.setLastModified(modifiedTime);
		} /*
		 * else targetFile.setLastModified(System.currentTimeMillis());
		 */
	}

	/**
	 * Copy contents of source folder to target folder. Caution: the file with
	 * duplicated name will be overwrite.
	 * 
	 * @param sourceDir
	 * @param targetDir
	 * @throws IOException
	 */
	public static void copyDirectiory(String sourceDir, String targetDir)
			throws IOException {
		new File(targetDir).mkdirs();
		File[] file = (new File(sourceDir)).listFiles();
		for (int i = 0; i < file.length; i++) {
			if (file[i].isFile()) {
				File sourceFile = file[i];
				File targetFile = new File(new File(targetDir)
						.getAbsolutePath()
						+ File.separator + file[i].getName());
				copyFile(sourceFile, targetFile);
			}
			if (file[i].isDirectory()) {
				String dir1 = sourceDir + "/" + file[i].getName();
				String dir2 = targetDir + "/" + file[i].getName();
				copyDirectiory(dir1, dir2);
			}
		}
	}

	/**
	 * Get a text file's content with default encoding "UTF-8" if not set.
	 * 
	 * @param file
	 * @param charset
	 *            Can be null, and default UTF-8
	 * @return
	 * @throws IOException
	 */
	public static StringBuffer getContent(File file, String charset)
			throws IOException {
		return getContent(new FileInputStream(file), charset);
	}

	/**
	 * Convert input stream to string content with default encoding "UTF-8" if
	 * not set.
	 * 
	 * @param inputStream
	 * @param charset
	 *            can be null, and default is UTF-8
	 * @return
	 * @throws IOException
	 */
	public static StringBuffer getContent(InputStream inputStream,
			String charset) throws IOException {
		if (charset == null)
			charset = "UTF-8";

		StringBuffer sb = new StringBuffer();
		InputStreamReader fileReader = new InputStreamReader(inputStream,
				charset);
		BufferedReader br = new BufferedReader(fileReader);
		String str;
		while ((str = br.readLine()) != null) {
			sb.append(str);
			sb.append(System.getProperty("line.separator"));
		}

		fileReader.close();
		br.close();

		return sb;
	}

	public static void zipFolder(String sourcePath, String targetZipFilePath)
			throws Exception {
		File inputRoot = new Path(sourcePath).toFile();
		if (!inputRoot.exists() || !inputRoot.isDirectory())
			throw new IllegalArgumentException(
					"Source folder path to zip not invalid.");

		File zipFile = new Path(targetZipFilePath).toFile();

		if (FileUtil.createFile(zipFile, false)) {
			FileOutputStream out = new FileOutputStream(zipFile);
			CheckedOutputStream cs = new CheckedOutputStream(out, new Adler32());
			ZipOutputStream zout = new ZipOutputStream(
					new BufferedOutputStream(cs));
			zip(zout, inputRoot, "", cs);
			zout.close();
		} else
			throw new IOException("target zip file create failed.");

	}

	private static void zip(ZipOutputStream out, File inFile, String root,
			CheckedOutputStream cs) throws Exception {
		if (inFile.isDirectory()) {
			File[] files = inFile.listFiles();
			if (root == null)
				root = "";
			if (root.length() != 0)
				out.putNextEntry(new ZipEntry(root + "/"));
			root = root.length() == 0 ? "" : root + "/";
			for (int i = 0; i < files.length; i++) {
				zip(out, files[i], root + files[i].getName(), cs);
			}
		} else {
			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream(inFile));
			long lastModifiedTime = inFile.lastModified();
			ZipEntry entry = new ZipEntry(root);
			if (lastModifiedTime > 0)
				entry.setTime(lastModifiedTime);
			out.putNextEntry(entry);
			int c;
			while ((c = in.read()) != -1)
				out.write(c);
			in.close();
			// System.out.println("Checksum::" + cs.getChecksum().getValue());
		}
	}

	public static byte[] getByteArray(InputStream in) throws IOException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		while (in.available() > 0) {
			bo.write(in.read());
		}
		return bo.toByteArray();

	}
}

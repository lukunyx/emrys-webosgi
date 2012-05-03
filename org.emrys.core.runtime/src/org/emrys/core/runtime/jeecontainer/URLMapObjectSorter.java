/**
 * 
 */
package org.emrys.core.runtime.jeecontainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.emrys.core.runtime.jeeres.AbstMultiInstUrlMapObject;
import org.emrys.core.runtime.jeeres.ClonedExecutableServletObject;


/**
 * The sorter classs for multiple url mapping resource of servlet.
 * 
 * @author Leo Chang
 * @version 2011-9-16
 * @param <T>
 */
class URLMapObjectSorter<T extends AbstMultiInstUrlMapObject> {
	List<ClonedExecutableServletObject<T>> buffer = null;

	/**
	 * For Servlet and Filter, they may has mutiple URL pattern mapping. To sort
	 * the execute order by their url patterns, we cloned many object of the
	 * servlet or filter according their url patterns. These cloned object will
	 * be sort into a list to be execute, but a servlet or filter can only be
	 * executed once. This method will sort all cloned object for servlet or
	 * filter into a list. The result will be buffered only update need.
	 * 
	 * @param <T>
	 *            the type to sort.
	 * @param type
	 *            the type to sort.
	 * @param forceUpdate
	 *            whether to update the buffer.
	 * @return the sorted cloned object for servlet or filter.
	 */
	public List<ClonedExecutableServletObject<T>> sort(
			Collection<? extends AbstMultiInstUrlMapObject> multiInstObjs, boolean forceUpdate) {
		if (buffer == null || forceUpdate) {
			// Collect all filters, servlets by a certain bundle order.
			buffer = new ArrayList<ClonedExecutableServletObject<T>>();
			for (Iterator<? extends AbstMultiInstUrlMapObject> it = multiInstObjs.iterator(); it
					.hasNext();) {
				AbstMultiInstUrlMapObject urlPatternMutiInsObject = it.next();
				ClonedExecutableServletObject<AbstMultiInstUrlMapObject>[] clonedInstances = urlPatternMutiInsObject
						.cloneInstances();
				if (clonedInstances == null)
					continue;
				for (int i = 0; i < clonedInstances.length; i++) {
					buffer.add((ClonedExecutableServletObject<T>) clonedInstances[i]);
				}
			}

			Comparator<ClonedExecutableServletObject<T>> comparator = new ReqServletMapComparator<ClonedExecutableServletObject<T>>();
			Collections.sort(buffer, comparator);
		}

		return buffer;
	}

	/**
	 * The comparator to sort the url map pattern of serlvet or filter. The
	 * regular as following: 1. exact map. 2. the longest map regular if
	 * wildcard path map. 3. the extesion map
	 * 
	 * @author Leo Chang - Hirisun
	 * @version 2011-6-26
	 * @param <T0>
	 */
	class ReqServletMapComparator<T0 extends ClonedExecutableServletObject<T>> implements
			Comparator<T0> {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(ClonedExecutableServletObject o1, ClonedExecutableServletObject o2) {

			String p1 = ((AbstMultiInstUrlMapObject) o1.getOriginalObj()).getURLPatterns()[o1
					.getId()];
			String p2 = ((AbstMultiInstUrlMapObject) o2.getOriginalObj()).getURLPatterns()[o2
					.getId()];

			if (p1 == null || p1.length() == 0)
				return 1;
			if (p2 == null || p2.length() == 0)
				return -1;

			int index1 = p1.indexOf('*');
			int index2 = p2.indexOf('*');

			if (index1 != -1 && index2 != -1) {
				return -p1.compareTo(p2);
			}

			if (index1 == -1 && index2 != -1) {
				return -1;
			}

			if (index1 != -1 && index2 == -1) {
				return 1;
			}

			if (index1 != -1 && index2 != -1) {
				String h1 = p1.substring(0, index1);
				String e1 = p1.substring(index1 + 1);
				String h2 = p2.substring(0, index2);
				String e2 = p2.substring(index2 + 1);

				if (h1.length() > h2.length())
					return -1;
				else if (h1.length() < h2.length())
					return 1;
				else {
					int r = h1.compareTo(h2);
					if (r != 0) {
						return r > 0 ? -1 : 1;
					} else {
						// Compare the end sub str.
						if (e1.length() > e2.length())
							return -1;
						else if (e1.length() < e2.length())
							return 1;
						else
							return e1.compareTo(e2);
					}
				}
			}
			return 0;
		}
	}
}

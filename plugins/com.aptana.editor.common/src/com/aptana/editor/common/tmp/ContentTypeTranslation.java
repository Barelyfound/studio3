/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.editor.common.tmp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aptana.editor.common.QualifiedContentType;

/**
 * @author Max Stepanov
 */
public class ContentTypeTranslation
{

	// TODO Hide the implementation behind an interface, have clients grab singleton from the plugin, rather than
	// directly on this class!
	private static ContentTypeTranslation instance;

	private Map<QualifiedContentType, QualifiedContentType> map = new HashMap<QualifiedContentType, QualifiedContentType>();

	private ContentTypeTranslation()
	{
	}

	public static ContentTypeTranslation getDefault()
	{
		if (instance == null)
		{
			instance = new ContentTypeTranslation();
		}
		return instance;
	}

	/**
	 * Allows plugins to contribute a scope translation. This is used to give different scope names to various
	 * partitions based on their nesting within other languages.
	 * 
	 * @param left
	 * @param right
	 */
	public void addTranslation(QualifiedContentType left, QualifiedContentType right)
	{
		map.put(left, right);
	}

	public QualifiedContentType translate(QualifiedContentType contentType)
	{
		QualifiedContentType i = contentType;
		QualifiedContentType result;
		List<String> parts = new ArrayList<String>();
		// Chop off last portion of scope until we find that the full scope is in our translation map
		while ((result = map.get(i)) == null && i.getPartCount() > 0)
		{
			parts.add(0, i.getLastPart());
			i = i.supertype();
		}
		if (result != null)
		{
			// Remaining scope is in our translation mapping
			for (String part : parts)
			{
				// Now try to translate the chopped off parts
				QualifiedContentType qtype = new QualifiedContentType(part);
				if (map.containsKey(qtype))
				{
					result = result.subtype(map.get(qtype).getParts());
				}
			}
			return result;
		}
		return contentType;
	}

}

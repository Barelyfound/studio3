/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
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
package com.aptana.editor.js.sdoc.parsing;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

import beaver.Scanner;
import beaver.Symbol;

import com.aptana.editor.js.sdoc.lexer.SDocTokenType;

public class SDocScanner extends Scanner
{
	private SDocTokenScanner fTokenScanner;
	private SDocTypeTokenScanner fTypeTokenScanner;
	private IDocument fDocument;
	private Queue<Symbol> fQueue;
	private int fOffset;

	/**
	 * SDocScanner
	 */
	public SDocScanner()
	{
		fTokenScanner = new SDocTokenScanner();
		fTypeTokenScanner = new SDocTypeTokenScanner();
		fQueue = new ArrayDeque<Symbol>();
	}

	/*
	 * (non-Javadoc)
	 * @see beaver.Scanner#nextToken()
	 */
	@Override
	public Symbol nextToken() throws IOException, Exception
	{
		Symbol result;

		if (fQueue.size() > 0)
		{
			result = fQueue.poll();
		}
		else
		{
			IToken token = fTokenScanner.nextToken();
			Object data = token.getData();

			while (data == SDocTokenType.WHITESPACE)
			{
				token = fTokenScanner.nextToken();
				data = token.getData();
			}

			int offset = fTokenScanner.getTokenOffset();
			int length = fTokenScanner.getTokenLength();
			SDocTokenType type = (data == null) ? SDocTokenType.EOF : (SDocTokenType) data;

			if (type == SDocTokenType.TYPES)
			{
				this.queueTypeTokens(offset, length);

				result = fQueue.poll();
			}
			else
			{
				try
				{
					int totalLength = fDocument.getLength();

					if (offset > totalLength)
					{
						offset = totalLength;
					}
					if (length == -1)
					{
						length = 0;
					}

					result = new Symbol(type.getIndex(), offset + fOffset, offset + fOffset + length - 1, fDocument.get(offset, length));
				}
				catch (BadLocationException e)
				{
					throw new Scanner.Exception(e.getLocalizedMessage());
				}
			}
		}

		return result;
	}

	/**
	 * queueTypeTokens
	 * 
	 * @param typesOffset
	 * @param typesLength
	 * @throws Exception
	 */
	protected void queueTypeTokens(int typesOffset, int typesLength) throws Exception
	{
		fTypeTokenScanner.setRange(fDocument, typesOffset, typesLength);
		IToken token = fTypeTokenScanner.nextToken();

		while (token != Token.EOF)
		{
			int offset = fTypeTokenScanner.getTokenOffset();
			int length = fTypeTokenScanner.getTokenLength();
			Object data = token.getData();
			SDocTokenType type = (data == null) ? SDocTokenType.EOF : (SDocTokenType) data;

			try
			{
				Symbol symbol = new Symbol(type.getIndex(), offset + fOffset, offset + fOffset + length - 1, fDocument.get(offset, length));

				fQueue.offer(symbol);

				token = fTypeTokenScanner.nextToken();
			}
			catch (BadLocationException e)
			{
				throw new Scanner.Exception(e.getLocalizedMessage());
			}
		}
	}

	/**
	 * The offset to apply to all symbols generated by this scanner
	 * 
	 * @param offset
	 */
	public void setOffset(int offset)
	{
		fOffset = offset;
	}

	/**
	 * setSource
	 * 
	 * @param document
	 */
	public void setSource(IDocument document)
	{
		fDocument = document;
		fTokenScanner.setRange(fDocument, 0, fDocument.getLength());
	}

	/**
	 * setSource
	 * 
	 * @param text
	 */
	public void setSource(String text)
	{
		setSource(new Document(text));
	}
}

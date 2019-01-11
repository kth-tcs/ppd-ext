 package org.apache.lucene.misc;
 
 /* ====================================================================
  * The Apache Software License, Version 1.1
  *
 * Copyright (c) 2001,2004 The Apache Software Foundation.  All rights
  * reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  *
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  * 3. The end-user documentation included with the redistribution,
  *    if any, must include the following acknowledgment:
  *       "This product includes software developed by the
  *        Apache Software Foundation (http://www.apache.org/)."
  *    Alternately, this acknowledgment may appear in the software itself,
  *    if and wherever such third-party acknowledgments normally appear.
  *
  * 4. The names "Apache" and "Apache Software Foundation" and
  *    "Apache Lucene" must not be used to endorse or promote products
  *    derived from this software without prior written permission. For
  *    written permission, please contact apache@apache.org.
  *
  * 5. Products derived from this software may not be called "Apache",
  *    "Apache Lucene", nor may "Apache" appear in their name, without
  *    prior written permission of the Apache Software Foundation.
  *
  * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
  * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
  * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
  * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  * ====================================================================
  *
  * This software consists of voluntary contributions made by many
  * individuals on behalf of the Apache Software Foundation.  For more
  * information on the Apache Software Foundation, please see
  * <http://www.apache.org/>.
  */
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermEnum;
import org.apache.lucene.util.PriorityQueue;
 
 /**
  * <code>HighFreqTerms</code> class extracts terms and their frequencies out
  * of an existing Lucene index.
  *
  * @version $Id$
  */
public class HighFreqTerms {
 
	// The top numTerms will be displayed
	public static final int numTerms = 100;

	public static void main(String[] args) throws Exception {
         IndexReader reader = null;
		if (args.length == 1) {
             reader = IndexReader.open(args[0]);
		} else {
             usage();
             System.exit(1);
         }
 
         TermInfoQueue tiq = new TermInfoQueue(numTerms);
         TermEnum terms = reader.terms();
 
		while (terms.next()) {
			tiq.insert(new TermInfo(terms.term(), terms.docFreq()));
         }
 
		while (tiq.size() != 0) {
			TermInfo termInfo = (TermInfo) tiq.pop();
             System.out.println(termInfo.term + " " + termInfo.docFreq);
         }
 
         reader.close();
     }
 
	private static void usage() {
		System.out.println(
			"\n\n"
				+ "java org.apache.lucene.misc.HighFreqTerms <index dir>\n\n");
     }
 }
 
final class TermInfo {
	TermInfo(Term t, int df) {
         term = t;
         docFreq = df;
     }
     int docFreq;
     Term term;
 }
 
final class TermInfoQueue extends PriorityQueue {
	TermInfoQueue(int size) {
         initialize(size);
     }

	protected final boolean lessThan(Object a, Object b) {
		TermInfo termInfoA = (TermInfo) a;
		TermInfo termInfoB = (TermInfo) b;
         return termInfoA.docFreq < termInfoB.docFreq;
     }
 }
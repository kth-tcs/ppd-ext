 package org.apache.lucene.analysis.stempel;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.pl.PolishAnalyzer;
 import org.apache.lucene.analysis.stempel.StempelFilter;
 import org.apache.lucene.analysis.stempel.StempelStemmer;
 import org.apache.lucene.analysis.util.AbstractAnalysisFactory; // javadocs
 import org.apache.lucene.analysis.util.TokenFilterFactory;
 
 /**
  * Factory for {@link StempelFilter} using a Polish stemming table.
  */
 public class StempelPolishStemFilterFactory extends TokenFilterFactory {  
   
   /** Sole constructor. See {@link AbstractAnalysisFactory} for initialization lifecycle. */
   public StempelPolishStemFilterFactory() {}
 
  @Override
   public TokenStream create(TokenStream input) {
     return new StempelFilter(input, new StempelStemmer(PolishAnalyzer.getDefaultTable()));
   }
 }
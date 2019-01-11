 ij> --
 --   Licensed to the Apache Software Foundation (ASF) under one or more
 --   contributor license agreements.  See the NOTICE file distributed with
 --   this work for additional information regarding copyright ownership.
 --   The ASF licenses this file to You under the Apache License, Version 2.0
 --   (the "License"); you may not use this file except in compliance with
 --   the License.  You may obtain a copy of the License at
 --
 --      http://www.apache.org/licenses/LICENSE-2.0
 --
 --   Unless required by applicable law or agreed to in writing, software
 --   distributed under the License is distributed on an "AS IS" BASIS,
 --   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 --   See the License for the specific language governing permissions and
 --   limitations under the License.
 --
 -- test subquery flattening into outer query block
 set isolation to rr;
 0 rows inserted/updated/deleted
 ij> -- tests for flattening a subquery based on a
 -- uniqueness condition
 -- by default, holdability of ResultSet objects created using this Connection object is true. Following will set it to false for this connection.
 NoHoldForConnection;
 ij> -- create some tables
 create table outer1 (c1 int, c2 int, c3 int);
 0 rows inserted/updated/deleted
 ij> create table outer2 (c1 int, c2 int, c3 int);
 0 rows inserted/updated/deleted
 ij> create table noidx (c1 int);
 0 rows inserted/updated/deleted
 ij> create table idx1 (c1 int);
 0 rows inserted/updated/deleted
 ij> create unique index idx1_1 on idx1(c1);
 0 rows inserted/updated/deleted
 ij> create table idx2 (c1 int, c2 int);
 0 rows inserted/updated/deleted
 ij> create unique index idx2_1 on idx2(c1, c2);
 0 rows inserted/updated/deleted
 ij> create table nonunique_idx1 (c1 int);
 0 rows inserted/updated/deleted
 ij> create index nonunique_idx1_1 on nonunique_idx1(c1);
 0 rows inserted/updated/deleted
 ij> insert into outer1 values (1, 2, 3);
 1 row inserted/updated/deleted
 ij> insert into outer1 values (4, 5, 6);
 1 row inserted/updated/deleted
 ij> insert into outer2 values (1, 2, 3);
 1 row inserted/updated/deleted
 ij> insert into outer2 values (4, 5, 6);
 1 row inserted/updated/deleted
 ij> insert into noidx values 1, 1;
 2 rows inserted/updated/deleted
 ij> insert into idx1 values 1, 2;
 2 rows inserted/updated/deleted
 ij> insert into idx2 values (1, 1), (1, 2);
 2 rows inserted/updated/deleted
 ij> insert into nonunique_idx1 values 1, 1;
 2 rows inserted/updated/deleted
 ij> -- cases where subqueries don't get flattened
 -- (we would get incorrect results with 
 -- incorrect flattening)
 -- one of tables in subquery doesn't have index
 select * from outer1 where c1 in (select idx1.c1 from noidx, idx1 where idx1.c1 = noidx.c1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> -- group by in subquery
 select * from outer1 o where c1 <= (select c1 from idx1 i group by c1);
 ERROR 21000: Scalar subquery is only allowed to return a single row.
 ij> -- otherwise flattenable subquery under an or 
 -- subquery returns no rows
 select * from outer1 o where c1 + 0 = 1 or c1 in (select c1 from idx1 i where i.c1 = 0);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> select * from outer1 o where c1 in (select c1 from idx1 i where i.c1 = 0) or c1 + 0 = 1;
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> -- empty subquery in select list which is otherwise flattenable
 select (select c1 from idx1 where c1 = 0) from outer1;
 1          
 -----------
 NULL       
 NULL       
 ij> -- multiple tables in subquery
 -- no one table's equality condition based
 -- solely on constants and correlation columns
 select * from outer1 o where exists (select * from idx2 i, idx1 where o.c1 = i.c1 and i.c2 = idx1.c1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> -- subqueries that should get flattened
 call SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(1);
 0 rows inserted/updated/deleted
 ij> maximumdisplaywidth 40000;
 ij> -- simple IN
 select * from outer1 o where o.c1 in (select c1 from idx1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- simple IN
 select * from outer1 o where o.c1 in (select c1 from idx1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=2
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 ij> -- simple EXISTS
 select * from outer1 o where exists (select * from idx1 i where o.c1 = i.c1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- simple EXISTS
 select * from outer1 o where exists (select * from idx1 i where o.c1 = i.c1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=2
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 ij> -- simple ANY
 select * from outer1 o where o.c1 = ANY (select c1 from idx1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- simple ANY
 select * from outer1 o where o.c1 = ANY (select c1 from idx1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=2
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 ij> -- another simple ANY
 select * from outer1 o where o.c2 > ANY (select c1 from idx1 i where o.c1 = i.c1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- another simple ANY
 select * from outer1 o where o.c2 > ANY (select c1 from idx1 i where o.c1 = i.c1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=2
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: <
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- comparisons with parameters
 prepare p1 as 'select * from outer1 o where exists (select * from idx1 i where i.c1 = ?)';
 ij> execute p1 using 'values 1';
 IJ WARNING: Autocommit may close using result set
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 4          |5          |6          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select * from outer1 o where exists (select * from idx1 i where i.c1 = ?)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 2
 	Rows filtered = 0
 	Rows returned = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 	Right result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 ij> prepare p2 as 'select * from outer1 o where ? = ANY (select c1 from idx1)';
 ij> execute p2 using 'values 1';
 IJ WARNING: Autocommit may close using result set
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 4          |5          |6          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select * from outer1 o where ? = ANY (select c1 from idx1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 2
 	Rows filtered = 0
 	Rows returned = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 	Right result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 ij> -- mix constants with correlation columns
 select * from outer1 o where exists (select * from idx2 i where o.c1 = i.c1 and i.c2 = 2);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- mix constants with correlation columns
 select * from outer1 o where exists (select * from idx2 i where o.c1 = i.c1 and i.c2 = 2)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Index Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0, 1}
 			Number of columns fetched=2
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=2
 			Scan type=btree
 			Tree height=1
 			start position:
 				None
 			stop position:
 				None
 			qualifiers:
 				Column[0][0] Id: 1
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 	Right result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- multiple tables in subquery
 select * from outer1 o where exists (select * from idx2 i, idx1 where o.c1 = i.c1 and i.c2 = idx1.c1 and i.c2 = 1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- multiple tables in subquery
 select * from outer1 o where exists (select * from idx2 i, idx1 where o.c1 = i.c1 and i.c2 = idx1.c1 and i.c2 = 1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (6):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Nested Loop Join ResultSet:
 		Number of opens = 1
 		Rows seen from the left = 1
 		Rows seen from the right = 1
 		Rows filtered = 0
 		Rows returned = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 		Left result set:
 			Index Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 1
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0, 1}
 				Number of columns fetched=2
 				Number of deleted rows visited=0
 				Number of pages visited=1
 				Number of rows qualified=1
 				Number of rows visited=2
 				Scan type=btree
 				Tree height=1
 				start position:
 					None
 				stop position:
 					None
 				qualifiers:
 					Column[0][0] Id: 1
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 		Right result set:
 			Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 1
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched=All
 				Number of columns fetched=3
 				Number of pages visited=1
 				Number of rows qualified=1
 				Number of rows visited=2
 				Scan type=heap
 				start position:
 					null
 				stop position:
 					null
 				qualifiers:
 					Column[0][0] Id: 0
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 ij> -- comparisons with non-join expressions
 select * from outer1 o where exists (select * from idx1 where idx1.c1 = 1 + 0);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 4          |5          |6          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- comparisons with non-join expressions
 select * from outer1 o where exists (select * from idx1 where idx1.c1 = 1 + 0)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 2
 	Rows filtered = 0
 	Rows returned = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 	Right result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 ij> select * from outer1 o where exists (select * from idx2 i, idx1 where o.c1 + 0 = i.c1 and i.c2 + 0 = idx1.c1 and i.c2 = 1 + 0);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select * from outer1 o where exists (select * from idx2 i, idx1 where o.c1 + 0 = i.c1 and i.c2 + 0 = idx1.c1 and i.c2 = 1 + 0)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (7):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Nested Loop Join ResultSet:
 		Number of opens = 1
 		Rows seen from the left = 1
 		Rows seen from the right = 1
 		Rows filtered = 0
 		Rows returned = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 		Left result set:
 			Index Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 1
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0, 1}
 				Number of columns fetched=2
 				Number of deleted rows visited=0
 				Number of pages visited=1
 				Number of rows qualified=1
 				Number of rows visited=2
 				Scan type=btree
 				Tree height=1
 				start position:
 					None
 				stop position:
 					None
 				qualifiers:
 					Column[0][0] Id: 1
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 		Right result set:
 			Project-Restrict ResultSet (5):
 			Number of opens = 1
 			Rows seen = 2
 			Rows filtered = 1
 			restriction = true
 			projection = false
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 2
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched=All
 					Number of columns fetched=3
 					Number of pages visited=1
 					Number of rows qualified=2
 					Number of rows visited=2
 					Scan type=heap
 					start position:
 						null
 					stop position:
 						null
 					qualifiers:
 						None
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 ij> -- multilevel subqueries
 -- only flatten bottom of where exists, any, or in with 
 -- exists, any, or in in its own where clause. DERBY-3301.
 select * from outer1 o where exists
     (select * from idx2 i where exists
         (select * from idx1 ii 
          where o.c1 = i.c1 and i.c2 = ii.c1 and i.c2 = 1));
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- multilevel subqueries
 -- only flatten bottom of where exists, any, or in with 
 -- exists, any, or in in its own where clause. DERBY-3301.
 select * from outer1 o where exists
     (select * from idx2 i where exists
         (select * from idx1 ii 
          where o.c1 = i.c1 and i.c2 = ii.c1 and i.c2 = 1))
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Attached subqueries:
 	Begin Subquery Number 0
 	Any ResultSet  (Attached to 2):
 	Number of opens = 2
 	Rows seen = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 2
 			Rows seen from the left = 1
 			Rows seen from the right = 1
 			Rows filtered = 0
 			Rows returned = 1
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share row locking chosen by the optimizer
 				Number of opens = 2
 				Rows seen = 1
 				Rows filtered = 0
 				Fetch Size = 1
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0, 1}
 					Number of columns fetched=2
 					Number of deleted rows visited=0
 					Number of pages visited=2
 					Number of rows qualified=1
 					Number of rows visited=1
 					Scan type=btree
 					Tree height=1
 					start position:
 						>= on first 2 column(s).
 						Ordered null semantics on the following columns: 
 					stop position:
 						> on first 2 column(s).
 						Ordered null semantics on the following columns: 
 					qualifiers:
 						None
 			Right result set:
 				Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 1
 				Rows filtered = 0
 				Fetch Size = 1
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=1
 					Number of rows visited=1
 					Scan type=btree
 					Tree height=1
 					start position:
 						>= on first 1 column(s).
 						Ordered null semantics on the following columns: 
 					stop position:
 						> on first 1 column(s).
 						Ordered null semantics on the following columns: 
 					qualifiers:
 						None
 	End Subquery Number 0
 Project-Restrict ResultSet (2):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 1
 restriction = true
 projection = false
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 	Number of opens = 1
 	Rows seen = 2
 	Rows filtered = 0
 	Fetch Size = 16
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 		next time in milliseconds/row = 0
 	scan information:
 		Bit set of columns fetched=All
 		Number of columns fetched=3
 		Number of pages visited=1
 		Number of rows qualified=2
 		Number of rows visited=2
 		Scan type=heap
 		start position:
 			null
 		stop position:
 			null
 		qualifiers:
 			None
 ij> -- only flatten bottom
 select * from outer1 o where exists
     (select * from idx2 i where exists
         (select * from idx1 ii 
          where o.c1 = i.c1 and i.c2 = ii.c1));
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- only flatten bottom
 select * from outer1 o where exists
     (select * from idx2 i where exists
         (select * from idx1 ii 
          where o.c1 = i.c1 and i.c2 = ii.c1))
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Attached subqueries:
 	Begin Subquery Number 0
 	Any ResultSet  (Attached to 2):
 	Number of opens = 2
 	Rows seen = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 2
 			Rows seen from the left = 1
 			Rows seen from the right = 1
 			Rows filtered = 0
 			Rows returned = 1
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Hash Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share table locking: 
 				Number of opens = 2
 				Hash table size = 1
 				Hash key is column number 0
 				Rows seen = 1
 				Rows filtered = 0
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information: 
 					Bit set of columns fetched={0, 1}
 					Number of columns fetched=2
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=2
 					Number of rows visited=2
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					scan qualifiers:
 						None
 					next qualifiers:
 						Column[0][0] Id: 0
 						Operator: =
 						Ordered nulls: false
 						Unknown return value: false
 						Negate comparison result: false
 			Right result set:
 				Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 1
 				Rows filtered = 0
 				Fetch Size = 1
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=1
 					Number of rows visited=1
 					Scan type=btree
 					Tree height=1
 					start position:
 						>= on first 1 column(s).
 						Ordered null semantics on the following columns: 
 					stop position:
 						> on first 1 column(s).
 						Ordered null semantics on the following columns: 
 					qualifiers:
 						None
 	End Subquery Number 0
 Project-Restrict ResultSet (2):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 1
 restriction = true
 projection = false
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 	Number of opens = 1
 	Rows seen = 2
 	Rows filtered = 0
 	Fetch Size = 16
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 		next time in milliseconds/row = 0
 	scan information:
 		Bit set of columns fetched=All
 		Number of columns fetched=3
 		Number of pages visited=1
 		Number of rows qualified=2
 		Number of rows visited=2
 		Scan type=heap
 		start position:
 			null
 		stop position:
 			null
 		qualifiers:
 			None
 ij> -- flatten innermost into exists join, but dont flatten middle into outer as it
 -- is a where exists, any, or in with exists, any, or in in its own where clause. 
 -- DERBY-3301.
 select * from outer1 o where exists
     (select * from idx2 i 
      where  o.c1 = i.c1 and i.c2 = 1 and exists
         (select * from idx1 ii));
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- flatten innermost into exists join, but dont flatten middle into outer as it
 -- is a where exists, any, or in with exists, any, or in in its own where clause. 
 -- DERBY-3301.
 select * from outer1 o where exists
     (select * from idx2 i 
      where  o.c1 = i.c1 and i.c2 = 1 and exists
         (select * from idx1 ii))
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Attached subqueries:
 	Begin Subquery Number 0
 	Any ResultSet  (Attached to 2):
 	Number of opens = 2
 	Rows seen = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 2
 			Rows seen from the left = 1
 			Rows seen from the right = 1
 			Rows filtered = 0
 			Rows returned = 1
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share row locking chosen by the optimizer
 				Number of opens = 2
 				Rows seen = 1
 				Rows filtered = 0
 				Fetch Size = 1
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0, 1}
 					Number of columns fetched=2
 					Number of deleted rows visited=0
 					Number of pages visited=2
 					Number of rows qualified=1
 					Number of rows visited=1
 					Scan type=btree
 					Tree height=1
 					start position:
 						>= on first 2 column(s).
 						Ordered null semantics on the following columns: 
 					stop position:
 						> on first 2 column(s).
 						Ordered null semantics on the following columns: 
 					qualifiers:
 						None
 			Right result set:
 				Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 1
 				Rows filtered = 0
 				Fetch Size = 1
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={}
 					Number of columns fetched=0
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=1
 					Number of rows visited=1
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 	End Subquery Number 0
 Project-Restrict ResultSet (2):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 1
 restriction = true
 projection = false
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 	Number of opens = 1
 	Rows seen = 2
 	Rows filtered = 0
 	Fetch Size = 16
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 		next time in milliseconds/row = 0
 	scan information:
 		Bit set of columns fetched=All
 		Number of columns fetched=3
 		Number of pages visited=1
 		Number of rows qualified=2
 		Number of rows visited=2
 		Scan type=heap
 		start position:
 			null
 		stop position:
 			null
 		qualifiers:
 			None
 ij> -- flatten a subquery that has a subquery in its select list
 -- verify that subquery gets copied up to outer block
 select * from outer1 o where c1 in
     (select (select c1 from idx1 where c1 = i.c1)
      from idx2 i where o.c1 = i.c1 and i.c2 = 1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- flatten a subquery that has a subquery in its select list
 -- verify that subquery gets copied up to outer block
 select * from outer1 o where c1 in
     (select (select c1 from idx1 where c1 = i.c1)
      from idx2 i where o.c1 = i.c1 and i.c2 = 1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Attached subqueries:
 	Begin Subquery Number 1
 	Once ResultSetAttached to 4):
 	Number of opens = 1
 	Rows seen = 1
 	Source result set:
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 	End Subquery Number 1
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = true
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Index Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0, 1}
 			Number of columns fetched=2
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=2
 			Scan type=btree
 			Tree height=1
 			start position:
 				None
 			stop position:
 				None
 			qualifiers:
 				Column[0][0] Id: 1
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 	Right result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- expression subqueries
 -- simple =
 select * from outer1 o where o.c1 = (select c1 from idx1 i where o.c1 = i.c1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- expression subqueries
 -- simple =
 select * from outer1 o where o.c1 = (select c1 from idx1 i where o.c1 = i.c1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=2
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> select * from outer1 o where o.c1 <= (select c1 from idx1 i where o.c1 = i.c1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select * from outer1 o where o.c1 <= (select c1 from idx1 i where o.c1 = i.c1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=2
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: <
 				Ordered nulls: false
 				Unknown return value: true
 				Negate comparison result: true
 ij> -- multiple tables in subquery
 select * from outer1 o where c1 =  (select i.c1 from idx2 i, idx1 where o.c1 = i.c1 and i.c2 = idx1.c1 and i.c2 = 1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- multiple tables in subquery
 select * from outer1 o where c1 =  (select i.c1 from idx2 i, idx1 where o.c1 = i.c1 and i.c2 = idx1.c1 and i.c2 = 1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (6):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Nested Loop Join ResultSet:
 		Number of opens = 1
 		Rows seen from the left = 1
 		Rows seen from the right = 1
 		Rows filtered = 0
 		Rows returned = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 		Left result set:
 			Index Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 1
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0, 1}
 				Number of columns fetched=2
 				Number of deleted rows visited=0
 				Number of pages visited=1
 				Number of rows qualified=1
 				Number of rows visited=2
 				Scan type=btree
 				Tree height=1
 				start position:
 					None
 				stop position:
 					None
 				qualifiers:
 					Column[0][0] Id: 1
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 		Right result set:
 			Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 1
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched=All
 				Number of columns fetched=3
 				Number of pages visited=1
 				Number of rows qualified=1
 				Number of rows visited=2
 				Scan type=heap
 				start position:
 					null
 				stop position:
 					null
 				qualifiers:
 					Column[0][0] Id: 0
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 					Column[0][1] Id: 0
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share row locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=1
 			Scan type=btree
 			Tree height=1
 			start position:
 				>= on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			stop position:
 				> on first 1 column(s).
 				Ordered null semantics on the following columns: 
 			qualifiers:
 				None
 ij> -- flattening to an exists join
 -- no index on subquery table
 select * from outer1 where c1 in (select c1 from noidx);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- flattening to an exists join
 -- no index on subquery table
 select * from outer1 where c1 in (select c1 from noidx)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Hash Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Hash Scan ResultSet for NOIDX at serializable isolation level using share table locking: 
 		Number of opens = 2
 		Hash table size = 1
 		Hash key is column number 0
 		Rows seen = 1
 		Rows filtered = 0
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information: 
 			Bit set of columns fetched=All
 			Number of columns fetched=1
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			scan qualifiers:
 				None
 			next qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- no unique index on subquery table
 select * from outer1 where c1 in (select c1 from nonunique_idx1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- no unique index on subquery table
 select * from outer1 where c1 in (select c1 from nonunique_idx1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Hash Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Hash Scan ResultSet for NONUNIQUE_IDX1 using index NONUNIQUE_IDX1_1 at serializable isolation level using share table locking: 
 		Number of opens = 2
 		Hash table size = 1
 		Hash key is column number 0
 		Rows seen = 1
 		Rows filtered = 0
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information: 
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=btree
 			Tree height=1
 			start position:
 				None
 			stop position:
 				None
 			scan qualifiers:
 				None
 			next qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- columns in subquery are not superset of unique index
 select * from outer1 where c1 in (select c1 from idx2);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- columns in subquery are not superset of unique index
 select * from outer1 where c1 in (select c1 from idx2)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Hash Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Hash Scan ResultSet for IDX2 using index IDX2_1 at serializable isolation level using share table locking: 
 		Number of opens = 2
 		Hash table size = 1
 		Hash key is column number 0
 		Rows seen = 1
 		Rows filtered = 0
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information: 
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=btree
 			Tree height=1
 			start position:
 				None
 			stop position:
 				None
 			scan qualifiers:
 				None
 			next qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- single table subquery, self join on unique column
 select * from outer1 where exists (select * from idx1 where c1 = c1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 4          |5          |6          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- single table subquery, self join on unique column
 select * from outer1 where exists (select * from idx1 where c1 = c1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (5):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 2
 	Rows filtered = 0
 	Rows returned = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Project-Restrict ResultSet (4):
 		Number of opens = 2
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = true
 		projection = false
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 2
 			Rows seen = 2
 			Rows filtered = 0
 			Fetch Size = 1
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0}
 				Number of columns fetched=1
 				Number of deleted rows visited=0
 				Number of pages visited=2
 				Number of rows qualified=2
 				Number of rows visited=2
 				Scan type=btree
 				Tree height=1
 				start position:
 					None
 				stop position:
 					None
 				qualifiers:
 					None
 ij> -- flattening values subqueries
 -- flatten unless contains a subquery
 select * from outer1 where c1 in (values 1);
 C1         |C2         |C3         
 -----------------------------------
 1          |2          |3          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- flattening values subqueries
 -- flatten unless contains a subquery
 select * from outer1 where c1 in (values 1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 Fetch Size = 16
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	next time in milliseconds/row = 0
 scan information:
 	Bit set of columns fetched=All
 	Number of columns fetched=3
 	Number of pages visited=1
 	Number of rows qualified=1
 	Number of rows visited=2
 	Scan type=heap
 	start position:
 		null
 	stop position:
 		null
 	qualifiers:
 		Column[0][0] Id: 0
 		Operator: =
 		Ordered nulls: false
 		Unknown return value: false
 		Negate comparison result: false
 ij> select * from outer1 where c1 in (values (select max(c1) from outer1));
 C1         |C2         |C3         
 -----------------------------------
 4          |5          |6          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select * from outer1 where c1 in (values (select max(c1) from outer1))
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Materialized subqueries:
 	Begin Subquery Number 1
 	Once ResultSet:
 	Number of opens = 1
 	Rows seen = 1
 	Source result set:
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 		Project-Restrict ResultSet (12):
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Scalar Aggregate ResultSet:
 			Number of opens = 1
 			Rows input = 2
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Index Key Optimization = false
 			Source result set:
 				Project-Restrict ResultSet (11):
 				Number of opens = 1
 				Rows seen = 2
 				Rows filtered = 0
 				restriction = false
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 2
 					Rows filtered = 0
 					Fetch Size = 16
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of pages visited=1
 						Number of rows qualified=2
 						Number of rows visited=2
 						Scan type=heap
 						start position:
 							null
 						stop position:
 							null
 						qualifiers:
 							None
 	End Subquery Number 1
 Project-Restrict ResultSet (14):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=3
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 	Right result set:
 		Row ResultSet:
 		Number of opens = 1
 		Rows returned = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 ij> -- beetle 4459 - problems with flattening to exist joins and then flattening to 
 -- normal join
 -- non correlated exists subquery with conditional join
 maximumdisplaywidth 40000;
 ij> select o.c1 from outer1 o join outer2 o2 on (o.c1 = o2.c1) 
 where exists (select c1 from idx1);
 C1         
 -----------
 1          
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select o.c1 from outer1 o join outer2 o2 on (o.c1 = o2.c1) 
 where exists (select c1 from idx1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (6):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 2
 	Rows filtered = 0
 	Rows returned = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Hash Join ResultSet:
 		Number of opens = 1
 		Rows seen from the left = 2
 		Rows seen from the right = 2
 		Rows filtered = 0
 		Rows returned = 2
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 		Left result set:
 			Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 2
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0}
 				Number of columns fetched=1
 				Number of pages visited=1
 				Number of rows qualified=2
 				Number of rows visited=2
 				Scan type=heap
 				start position:
 					null
 				stop position:
 					null
 				qualifiers:
 					None
 		Right result set:
 			Hash Scan ResultSet for OUTER2 at serializable isolation level using share table locking: 
 			Number of opens = 2
 			Hash table size = 2
 			Hash key is column number 0
 			Rows seen = 2
 			Rows filtered = 0
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information: 
 				Bit set of columns fetched={0}
 				Number of columns fetched=1
 				Number of pages visited=1
 				Number of rows qualified=2
 				Number of rows visited=2
 				Scan type=heap
 				start position:
 					null
 				stop position:
 					null
 				scan qualifiers:
 					None
 				next qualifiers:
 					Column[0][0] Id: 0
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 	Right result set:
 		Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of deleted rows visited=0
 			Number of pages visited=2
 			Number of rows qualified=2
 			Number of rows visited=2
 			Scan type=btree
 			Tree height=1
 			start position:
 				None
 			stop position:
 				None
 			qualifiers:
 				None
 ij> -- in predicate (will be flattened to exists)
 select o.c1 from outer1 o join outer2 o2 on (o.c1 = o2.c1) 
 where o.c1 in (select c1 from idx1);
 C1         
 -----------
 1          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- in predicate (will be flattened to exists)
 select o.c1 from outer1 o join outer2 o2 on (o.c1 = o2.c1) 
 where o.c1 in (select c1 from idx1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (6):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 1
 	Rows seen from the right = 1
 	Rows filtered = 0
 	Rows returned = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Hash Join ResultSet:
 		Number of opens = 1
 		Rows seen from the left = 2
 		Rows seen from the right = 1
 		Rows filtered = 0
 		Rows returned = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 		Left result set:
 			Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 2
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0}
 				Number of columns fetched=1
 				Number of deleted rows visited=0
 				Number of pages visited=1
 				Number of rows qualified=2
 				Number of rows visited=2
 				Scan type=btree
 				Tree height=1
 				start position:
 					None
 				stop position:
 					None
 				qualifiers:
 					None
 		Right result set:
 			Hash Scan ResultSet for OUTER1 at serializable isolation level using share table locking: 
 			Number of opens = 2
 			Hash table size = 2
 			Hash key is column number 0
 			Rows seen = 1
 			Rows filtered = 0
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information: 
 				Bit set of columns fetched={0}
 				Number of columns fetched=1
 				Number of pages visited=1
 				Number of rows qualified=2
 				Number of rows visited=2
 				Scan type=heap
 				start position:
 					null
 				stop position:
 					null
 				scan qualifiers:
 					None
 				next qualifiers:
 					Column[0][0] Id: 0
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 	Right result set:
 		Table Scan ResultSet for OUTER2 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of pages visited=1
 			Number of rows qualified=1
 			Number of rows visited=2
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- flattened exists join in nested subquery
 select c1 from (select t.c1 from (select o.c1 from outer1 o join outer2 o2 on (o.c1 = o2.c1) where exists (select c1 from idx1)) t, outer2 where t.c1 = outer2.c1) t2;
 C1         
 -----------
 1          
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- flattened exists join in nested subquery
 select c1 from (select t.c1 from (select o.c1 from outer1 o join outer2 o2 on (o.c1 = o2.c1) where exists (select c1 from idx1)) t, outer2 where t.c1 = outer2.c1) t2
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (9):
 Number of opens = 1
 Rows seen = 2
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Nested Loop Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 2
 	Rows seen from the right = 2
 	Rows filtered = 0
 	Rows returned = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 2
 			Rows seen from the right = 2
 			Rows filtered = 0
 			Rows returned = 2
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Hash Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 2
 				Rows seen from the right = 2
 				Rows filtered = 0
 				Rows returned = 2
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Table Scan ResultSet for OUTER1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 2
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of pages visited=1
 						Number of rows qualified=2
 						Number of rows visited=2
 						Scan type=heap
 						start position:
 							null
 						stop position:
 							null
 						qualifiers:
 							None
 				Right result set:
 					Hash Scan ResultSet for OUTER2 at serializable isolation level using share table locking: 
 					Number of opens = 2
 					Hash table size = 2
 					Hash key is column number 0
 					Rows seen = 2
 					Rows filtered = 0
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information: 
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of pages visited=1
 						Number of rows qualified=2
 						Number of rows visited=2
 						Scan type=heap
 						start position:
 							null
 						stop position:
 							null
 						scan qualifiers:
 							None
 						next qualifiers:
 							Column[0][0] Id: 0
 							Operator: =
 							Ordered nulls: false
 							Unknown return value: false
 							Negate comparison result: false
 			Right result set:
 				Index Scan ResultSet for IDX1 using index IDX1_1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 2
 				Rows seen = 2
 				Rows filtered = 0
 				Fetch Size = 1
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=2
 					Number of rows qualified=2
 					Number of rows visited=2
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 	Right result set:
 		Table Scan ResultSet for OUTER2 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 2
 		Rows seen = 2
 		Rows filtered = 0
 		Fetch Size = 1
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched={0}
 			Number of columns fetched=1
 			Number of pages visited=1
 			Number of rows qualified=2
 			Number of rows visited=4
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> -- original reported bug
 create table business(businesskey int, name varchar(50), changedate int);
 0 rows inserted/updated/deleted
 ij> create table nameelement(parentkey int, parentelt varchar(50), seqnum int);
 0 rows inserted/updated/deleted
 ij> create table categorybag(cbparentkey int, cbparentelt varchar(50), 
 	krtModelKey varchar(50), keyvalue varchar(50));
 0 rows inserted/updated/deleted
 ij> select businesskey, name, changedate 
 from business as biz left outer join nameelement as nameElt 
 	on (businesskey = parentkey and parentelt = 'businessEntity') 
 where (nameElt.seqnum = 1) 
 	and businesskey in 
 		 (select cbparentkey 
 			from categorybag 
 			where (cbparentelt = 'businessEntity') and 
 				(krtModelKey = 'UUID:CD153257-086A-4237-B336-6BDCBDCC6634' and keyvalue = '40.00.00.00.00'))  order by name asc , biz.changedate asc;
 BUSINESSKEY|NAME                                              |CHANGEDATE 
 --------------------------------------------------------------------------
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select businesskey, name, changedate 
 from business as biz left outer join nameelement as nameElt 
 	on (businesskey = parentkey and parentelt = 'businessEntity') 
 where (nameElt.seqnum = 1) 
 	and businesskey in 
 		 (select cbparentkey 
 			from categorybag 
 			where (cbparentelt = 'businessEntity') and 
 				(krtModelKey = 'UUID:CD153257-086A-4237-B336-6BDCBDCC6634' and keyvalue = '40.00.00.00.00'))  order by name asc , biz.changedate asc
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Sort ResultSet:
 Number of opens = 1
 Rows input = 0
 Rows returned = 0
 Eliminate duplicates = false
 In sorted order = false
 Sort information: 
 	Number of rows input=0
 	Number of rows output=0
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 Source result set:
 	Project-Restrict ResultSet (6):
 	Number of opens = 1
 	Rows seen = 0
 	Rows filtered = 0
 	restriction = false
 	projection = true
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 		restriction time (milliseconds) = 0
 		projection time (milliseconds) = 0
 	Source result set:
 		Nested Loop Exists Join ResultSet:
 		Number of opens = 1
 		Rows seen from the left = 0
 		Rows seen from the right = 0
 		Rows filtered = 0
 		Rows returned = 0
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 		Left result set:
 			Nested Loop Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 0
 			Rows seen from the right = 0
 			Rows filtered = 0
 			Rows returned = 0
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Table Scan ResultSet for NAMEELEMENT at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 0
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				scan information:
 					Bit set of columns fetched=All
 					Number of columns fetched=3
 					Number of pages visited=1
 					Number of rows qualified=0
 					Number of rows visited=0
 					Scan type=heap
 					start position:
 						null
 					stop position:
 						null
 					qualifiers:
 						Column[0][0] Id: 1
 						Operator: =
 						Ordered nulls: false
 						Unknown return value: false
 						Negate comparison result: false
 						Column[0][1] Id: 2
 						Operator: =
 						Ordered nulls: false
 						Unknown return value: false
 						Negate comparison result: false
 			Right result set:
 				Table Scan ResultSet for BUSINESS at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 0
 				Rows seen = 0
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				scan information:
 					start position:
 						null
 					stop position:
 						null
 					qualifiers:
 						Column[0][0] Id: 0
 						Operator: =
 						Ordered nulls: false
 						Unknown return value: false
 						Negate comparison result: false
 		Right result set:
 			Table Scan ResultSet for CATEGORYBAG at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 0
 			Rows seen = 0
 			Rows filtered = 0
 			Fetch Size = 1
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			scan information:
 				start position:
 					null
 				stop position:
 					null
 				qualifiers:
 					Column[0][0] Id: 0
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 					Column[0][1] Id: 3
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 					Column[0][2] Id: 2
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 					Column[0][3] Id: 1
 					Operator: =
 					Ordered nulls: false
 					Unknown return value: false
 					Negate comparison result: false
 ij> -- clean up
 drop table outer1;
 0 rows inserted/updated/deleted
 ij> drop table outer2;
 0 rows inserted/updated/deleted
 ij> drop table noidx;
 0 rows inserted/updated/deleted
 ij> drop table idx1;
 0 rows inserted/updated/deleted
 ij> drop table idx2;
 0 rows inserted/updated/deleted
 ij> drop table nonunique_idx1;
 0 rows inserted/updated/deleted
 ij> drop table business;
 0 rows inserted/updated/deleted
 ij> drop table nameelement;
 0 rows inserted/updated/deleted
 ij> drop table categorybag;
 0 rows inserted/updated/deleted
 ij> -- --------------------------------------------------------------------
 -- TEST CASES for different kinds of subquery flattening, Beetle 5173
 -- --------------------------------------------------------------------
 drop table colls;
 ERROR 42Y55: 'DROP TABLE' cannot be performed on 'COLLS' because it does not exist.
 ij> drop table docs;
 ERROR 42Y55: 'DROP TABLE' cannot be performed on 'DOCS' because it does not exist.
 ij> CREATE TABLE "APP"."COLLS" ("ID" VARCHAR(128) NOT NULL, "COLLID" SMALLINT NOT NULL);
 0 rows inserted/updated/deleted
 ij> CREATE INDEX "APP"."NEW_INDEX3" ON "APP"."COLLS" ("COLLID");
 0 rows inserted/updated/deleted
 ij> CREATE INDEX "APP"."NEW_INDEX2" ON "APP"."COLLS" ("ID");
 0 rows inserted/updated/deleted
 ij> ALTER TABLE "APP"."COLLS" ADD CONSTRAINT "NEW_KEY2" UNIQUE ("ID", "COLLID");
 0 rows inserted/updated/deleted
 ij> CREATE TABLE "APP"."DOCS" ("ID" VARCHAR(128) NOT NULL);
 0 rows inserted/updated/deleted
 ij> CREATE INDEX "APP"."NEW_INDEX1" ON "APP"."DOCS" ("ID");
 0 rows inserted/updated/deleted
 ij> ALTER TABLE "APP"."DOCS" ADD CONSTRAINT "NEW_KEY1" PRIMARY KEY ("ID");
 0 rows inserted/updated/deleted
 ij> insert into colls values ('123', 2);
 1 row inserted/updated/deleted
 ij> insert into colls values ('124', -5);
 1 row inserted/updated/deleted
 ij> insert into colls values ('24', 1);
 1 row inserted/updated/deleted
 ij> insert into colls values ('26', -2);
 1 row inserted/updated/deleted
 ij> insert into colls values ('36', 1);
 1 row inserted/updated/deleted
 ij> insert into colls values ('37', 8);
 1 row inserted/updated/deleted
 ij> insert into docs values '24', '25', '36', '27', '124', '567';
 6 rows inserted/updated/deleted
 ij> call SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(1);
 0 rows inserted/updated/deleted
 ij> maximumdisplaywidth 40000;
 ij> -- NOT IN is flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( ID NOT IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) ) )
 ) AS TAB;
 1          
 -----------
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- NOT IN is flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( ID NOT IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) ) )
 ) AS TAB
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 4
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 4
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Project-Restrict ResultSet (6):
 			Number of opens = 1
 			Rows seen = 4
 			Rows filtered = 0
 			restriction = false
 			projection = true
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Nested Loop Exists Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 6
 				Rows seen from the right = 4
 				Rows filtered = 0
 				Rows returned = 4
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 6
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of deleted rows visited=0
 						Number of pages visited=1
 						Number of rows qualified=6
 						Number of rows visited=6
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							None
 						qualifiers:
 							None
 				Right result set:
 					Project-Restrict ResultSet (5):
 					Number of opens = 6
 					Rows seen = 3
 					Rows filtered = 1
 					restriction = true
 					projection = false
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						restriction time (milliseconds) = 0
 						projection time (milliseconds) = 0
 					Source result set:
 						Index Scan ResultSet for COLLS using constraint NEW_KEY2 at serializable isolation level using share row locking chosen by the optimizer
 						Number of opens = 6
 						Rows seen = 3
 						Rows filtered = 0
 						Fetch Size = 1
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information:
 							Bit set of columns fetched={0, 1}
 							Number of columns fetched=2
 							Number of deleted rows visited=0
 							Number of pages visited=6
 							Number of rows qualified=3
 							Number of rows visited=6
 							Scan type=btree
 							Tree height=1
 							start position:
 								>= on first 1 column(s).
 								Ordered null semantics on the following columns: 
 								0 
 							stop position:
 								> on first 1 column(s).
 								Ordered null semantics on the following columns: 
 								0 
 							qualifiers:
 								None
 ij> -- NOT EXISTS is flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( NOT EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB;
 1          
 -----------
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- NOT EXISTS is flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( NOT EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 4
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 4
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Project-Restrict ResultSet (6):
 			Number of opens = 1
 			Rows seen = 4
 			Rows filtered = 0
 			restriction = false
 			projection = true
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Nested Loop Exists Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 6
 				Rows seen from the right = 4
 				Rows filtered = 0
 				Rows returned = 4
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 6
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of deleted rows visited=0
 						Number of pages visited=1
 						Number of rows qualified=6
 						Number of rows visited=6
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							None
 						qualifiers:
 							None
 				Right result set:
 					Project-Restrict ResultSet (5):
 					Number of opens = 6
 					Rows seen = 3
 					Rows filtered = 1
 					restriction = true
 					projection = false
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						restriction time (milliseconds) = 0
 						projection time (milliseconds) = 0
 					Source result set:
 						Index Scan ResultSet for COLLS using constraint NEW_KEY2 at serializable isolation level using share row locking chosen by the optimizer
 						Number of opens = 6
 						Rows seen = 3
 						Rows filtered = 0
 						Fetch Size = 1
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information:
 							Bit set of columns fetched={0, 1}
 							Number of columns fetched=2
 							Number of deleted rows visited=0
 							Number of pages visited=6
 							Number of rows qualified=3
 							Number of rows visited=6
 							Scan type=btree
 							Tree height=1
 							start position:
 								>= on first 1 column(s).
 								Ordered null semantics on the following columns: 
 								0 
 							stop position:
 								> on first 1 column(s).
 								Ordered null semantics on the following columns: 
 								0 
 							qualifiers:
 								None
 ij> -- EXISTS is flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB;
 1          
 -----------
 2          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- EXISTS is flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Project-Restrict ResultSet (6):
 			Number of opens = 1
 			Rows seen = 2
 			Rows filtered = 0
 			restriction = false
 			projection = true
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Nested Loop Exists Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 6
 				Rows seen from the right = 2
 				Rows filtered = 0
 				Rows returned = 2
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 6
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of deleted rows visited=0
 						Number of pages visited=1
 						Number of rows qualified=6
 						Number of rows visited=6
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							None
 						qualifiers:
 							None
 				Right result set:
 					Project-Restrict ResultSet (5):
 					Number of opens = 6
 					Rows seen = 3
 					Rows filtered = 1
 					restriction = true
 					projection = false
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						restriction time (milliseconds) = 0
 						projection time (milliseconds) = 0
 					Source result set:
 						Index Scan ResultSet for COLLS using constraint NEW_KEY2 at serializable isolation level using share row locking chosen by the optimizer
 						Number of opens = 6
 						Rows seen = 3
 						Rows filtered = 0
 						Fetch Size = 1
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information:
 							Bit set of columns fetched={0, 1}
 							Number of columns fetched=2
 							Number of deleted rows visited=0
 							Number of pages visited=6
 							Number of rows qualified=3
 							Number of rows visited=6
 							Scan type=btree
 							Tree height=1
 							start position:
 								>= on first 1 column(s).
 								Ordered null semantics on the following columns: 
 								0 
 							stop position:
 								> on first 1 column(s).
 								Ordered null semantics on the following columns: 
 								0 
 							qualifiers:
 								None
 ij> -- IN is flattened
 SELECT count(ID) FROM DOCS WHERE ID IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 2          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- IN is flattened
 SELECT count(ID) FROM DOCS WHERE ID IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (7):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 2
 			Rows filtered = 0
 			Rows returned = 2
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (5):
 				Number of opens = 6
 				Rows seen = 3
 				Rows filtered = 1
 				restriction = true
 				projection = false
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Scan ResultSet for COLLS using constraint NEW_KEY2 at serializable isolation level using share row locking chosen by the optimizer
 					Number of opens = 6
 					Rows seen = 3
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0, 1}
 						Number of columns fetched=2
 						Number of deleted rows visited=0
 						Number of pages visited=6
 						Number of rows qualified=3
 						Number of rows visited=6
 						Scan type=btree
 						Tree height=1
 						start position:
 							>= on first 1 column(s).
 							Ordered null semantics on the following columns: 
 							0 
 						stop position:
 							> on first 1 column(s).
 							Ordered null semantics on the following columns: 
 							0 
 						qualifiers:
 							None
 ij> -- ANY is flattened
 SELECT count(ID) FROM DOCS WHERE ID > ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ANY is flattened
 SELECT count(ID) FROM DOCS WHERE ID > ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (7):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 4
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 1
 		Rows seen = 4
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 4
 			Rows filtered = 0
 			Rows returned = 4
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (5):
 				Number of opens = 6
 				Rows seen = 15
 				Rows filtered = 11
 				restriction = true
 				projection = false
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Scan ResultSet for COLLS using constraint NEW_KEY2 at serializable isolation level using share row locking chosen by the optimizer
 					Number of opens = 6
 					Rows seen = 15
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0, 1}
 						Number of columns fetched=2
 						Number of deleted rows visited=0
 						Number of pages visited=6
 						Number of rows qualified=15
 						Number of rows visited=17
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							>= on first 1 column(s).
 							Ordered null semantics on the following columns: 
 							0 
 						qualifiers:
 							None
 ij> -- ANY is flattened
 SELECT count(ID) FROM DOCS WHERE ID <> ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 6          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ANY is flattened
 SELECT count(ID) FROM DOCS WHERE ID <> ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 6
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 6
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 6
 			Rows filtered = 0
 			Rows returned = 6
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (6):
 				Number of opens = 6
 				Rows seen = 6
 				Rows filtered = 0
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Row to Base Row ResultSet for COLLS:
 					Number of opens = 6
 					Rows seen = 6
 					Columns accessed from heap = {0}
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						Index Scan ResultSet for COLLS using index NEW_INDEX3 at serializable isolation level using share row locking chosen by the optimizer
 						Number of opens = 6
 						Rows seen = 6
 						Rows filtered = 0
 						Fetch Size = 1
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information:
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of deleted rows visited=0
 							Number of pages visited=6
 							Number of rows qualified=6
 							Number of rows visited=6
 							Scan type=btree
 							Tree height=1
 							start position:
 								>= on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							stop position:
 								> on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							qualifiers:
 								None
 ij> -- ALL is flattened, what's not?
 SELECT count(ID) FROM DOCS WHERE ID = ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 0          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ALL is flattened, what's not?
 SELECT count(ID) FROM DOCS WHERE ID = ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 0
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 0
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 0
 			Rows filtered = 0
 			Rows returned = 0
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (6):
 				Number of opens = 6
 				Rows seen = 6
 				Rows filtered = 0
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Row to Base Row ResultSet for COLLS:
 					Number of opens = 6
 					Rows seen = 6
 					Columns accessed from heap = {0}
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						Index Scan ResultSet for COLLS using index NEW_INDEX3 at serializable isolation level using share row locking chosen by the optimizer
 						Number of opens = 6
 						Rows seen = 6
 						Rows filtered = 0
 						Fetch Size = 1
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information:
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of deleted rows visited=0
 							Number of pages visited=6
 							Number of rows qualified=6
 							Number of rows visited=6
 							Scan type=btree
 							Tree height=1
 							start position:
 								>= on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							stop position:
 								> on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							qualifiers:
 								None
 ij> -- ALL is flattened, what's not?
 SELECT count(ID) FROM DOCS WHERE ID < ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 1          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ALL is flattened, what's not?
 SELECT count(ID) FROM DOCS WHERE ID < ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (7):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 1
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 1
 		Rows seen = 1
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 1
 			Rows filtered = 0
 			Rows returned = 1
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (5):
 				Number of opens = 6
 				Rows seen = 17
 				Rows filtered = 12
 				restriction = true
 				projection = false
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Scan ResultSet for COLLS using constraint NEW_KEY2 at serializable isolation level using share row locking chosen by the optimizer
 					Number of opens = 6
 					Rows seen = 17
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0, 1}
 						Number of columns fetched=2
 						Number of deleted rows visited=0
 						Number of pages visited=6
 						Number of rows qualified=17
 						Number of rows visited=18
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							> on first 1 column(s).
 							Ordered null semantics on the following columns: 
 							0 
 						qualifiers:
 							None
 ij> -- ALL is flattened, what's not?
 SELECT count(ID) FROM DOCS WHERE ID <> ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ALL is flattened, what's not?
 SELECT count(ID) FROM DOCS WHERE ID <> ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (7):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 4
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 1
 		Rows seen = 4
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 4
 			Rows filtered = 0
 			Rows returned = 4
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (5):
 				Number of opens = 6
 				Rows seen = 3
 				Rows filtered = 1
 				restriction = true
 				projection = false
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Scan ResultSet for COLLS using constraint NEW_KEY2 at serializable isolation level using share row locking chosen by the optimizer
 					Number of opens = 6
 					Rows seen = 3
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0, 1}
 						Number of columns fetched=2
 						Number of deleted rows visited=0
 						Number of pages visited=6
 						Number of rows qualified=3
 						Number of rows visited=6
 						Scan type=btree
 						Tree height=1
 						start position:
 							>= on first 1 column(s).
 							Ordered null semantics on the following columns: 
 							0 
 						stop position:
 							> on first 1 column(s).
 							Ordered null semantics on the following columns: 
 							0 
 						qualifiers:
 							None
 ij> -- Now test nullable correlated columns
 drop table colls;
 0 rows inserted/updated/deleted
 ij> -- the only change is ID is now nullable
 CREATE TABLE "APP"."COLLS" ("ID" VARCHAR(128), "COLLID" SMALLINT NOT NULL);
 0 rows inserted/updated/deleted
 ij> CREATE INDEX "APP"."NEW_INDEX3" ON "APP"."COLLS" ("COLLID");
 0 rows inserted/updated/deleted
 ij> CREATE INDEX "APP"."NEW_INDEX2" ON "APP"."COLLS" ("ID");
 0 rows inserted/updated/deleted
 ij> insert into colls values ('123', 2);
 1 row inserted/updated/deleted
 ij> insert into colls values ('124', -5);
 1 row inserted/updated/deleted
 ij> insert into colls values ('24', 1);
 1 row inserted/updated/deleted
 ij> insert into colls values ('26', -2);
 1 row inserted/updated/deleted
 ij> insert into colls values ('36', 1);
 1 row inserted/updated/deleted
 ij> insert into colls values ('37', 8);
 1 row inserted/updated/deleted
 ij> insert into colls values (null, -2);
 1 row inserted/updated/deleted
 ij> insert into colls values (null, 1);
 1 row inserted/updated/deleted
 ij> insert into colls values (null, 8);
 1 row inserted/updated/deleted
 ij> -- NOT EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( NOT EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB;
 1          
 -----------
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- NOT EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( NOT EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 4
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 4
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Project-Restrict ResultSet (6):
 			Number of opens = 1
 			Rows seen = 4
 			Rows filtered = 0
 			restriction = false
 			projection = true
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Hash Exists Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 6
 				Rows seen from the right = 4
 				Rows filtered = 0
 				Rows returned = 4
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 6
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of deleted rows visited=0
 						Number of pages visited=1
 						Number of rows qualified=6
 						Number of rows visited=6
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							None
 						qualifiers:
 							None
 				Right result set:
 					Project-Restrict ResultSet (5):
 					Number of opens = 6
 					Rows seen = 3
 					Rows filtered = 1
 					restriction = true
 					projection = false
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						restriction time (milliseconds) = 0
 						projection time (milliseconds) = 0
 					Source result set:
 						Hash Scan ResultSet for COLLS at serializable isolation level using share table locking: 
 						Number of opens = 6
 						Hash table size = 6
 						Hash key is column number 0
 						Rows seen = 3
 						Rows filtered = 0
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information: 
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of pages visited=1
 							Number of rows qualified=9
 							Number of rows visited=9
 							Scan type=heap
 							start position:
 								null
 							stop position:
 								null
 							scan qualifiers:
 								None
 							next qualifiers:
 								Column[0][0] Id: 0
 								Operator: =
 								Ordered nulls: false
 								Unknown return value: false
 								Negate comparison result: false
 ij> -- EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB;
 1          
 -----------
 2          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Project-Restrict ResultSet (6):
 			Number of opens = 1
 			Rows seen = 2
 			Rows filtered = 0
 			restriction = false
 			projection = true
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Hash Exists Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 6
 				Rows seen from the right = 2
 				Rows filtered = 0
 				Rows returned = 2
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 6
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of deleted rows visited=0
 						Number of pages visited=1
 						Number of rows qualified=6
 						Number of rows visited=6
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							None
 						qualifiers:
 							None
 				Right result set:
 					Project-Restrict ResultSet (5):
 					Number of opens = 6
 					Rows seen = 3
 					Rows filtered = 1
 					restriction = true
 					projection = false
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						restriction time (milliseconds) = 0
 						projection time (milliseconds) = 0
 					Source result set:
 						Hash Scan ResultSet for COLLS at serializable isolation level using share table locking: 
 						Number of opens = 6
 						Hash table size = 6
 						Hash key is column number 0
 						Rows seen = 3
 						Rows filtered = 0
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information: 
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of pages visited=1
 							Number of rows qualified=9
 							Number of rows visited=9
 							Scan type=heap
 							start position:
 								null
 							stop position:
 								null
 							scan qualifiers:
 								None
 							next qualifiers:
 								Column[0][0] Id: 0
 								Operator: =
 								Ordered nulls: false
 								Unknown return value: false
 								Negate comparison result: false
 ij> -- IN should be flattened
 SELECT count(ID) FROM DOCS WHERE ID IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 2          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- IN should be flattened
 SELECT count(ID) FROM DOCS WHERE ID IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (7):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Hash Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 2
 			Rows filtered = 0
 			Rows returned = 2
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (5):
 				Number of opens = 6
 				Rows seen = 3
 				Rows filtered = 1
 				restriction = true
 				projection = false
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Hash Scan ResultSet for COLLS at serializable isolation level using share table locking: 
 					Number of opens = 6
 					Hash table size = 6
 					Hash key is column number 0
 					Rows seen = 3
 					Rows filtered = 0
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information: 
 						Bit set of columns fetched=All
 						Number of columns fetched=2
 						Number of pages visited=1
 						Number of rows qualified=9
 						Number of rows visited=9
 						Scan type=heap
 						start position:
 							null
 						stop position:
 							null
 						scan qualifiers:
 							None
 						next qualifiers:
 							Column[0][0] Id: 0
 							Operator: =
 							Ordered nulls: false
 							Unknown return value: false
 							Negate comparison result: false
 ij> -- ANY should be flattened
 SELECT count(ID) FROM DOCS WHERE ID > ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ANY should be flattened
 SELECT count(ID) FROM DOCS WHERE ID > ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 4
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 4
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 6
 			Rows seen from the right = 4
 			Rows filtered = 0
 			Rows returned = 4
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 6
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=6
 					Number of rows visited=6
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (6):
 				Number of opens = 6
 				Rows seen = 16
 				Rows filtered = 12
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Row to Base Row ResultSet for COLLS:
 					Number of opens = 6
 					Rows seen = 16
 					Columns accessed from heap = {0}
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						Index Scan ResultSet for COLLS using index NEW_INDEX3 at serializable isolation level using share row locking chosen by the optimizer
 						Number of opens = 9
 						Rows seen = 16
 						Rows filtered = 0
 						Fetch Size = 1
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information:
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of deleted rows visited=0
 							Number of pages visited=9
 							Number of rows qualified=16
 							Number of rows visited=21
 							Scan type=btree
 							Tree height=1
 							start position:
 								>= on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							stop position:
 								> on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							qualifiers:
 								None
 ij> -- ALL should NOT be flattened, but subquery should be materialized
 SELECT count(ID) FROM DOCS WHERE ID <> ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 0          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ALL should NOT be flattened, but subquery should be materialized
 SELECT count(ID) FROM DOCS WHERE ID <> ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (9):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 0
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Attached subqueries:
 			Begin Subquery Number 0
 			Any ResultSet  (Attached to 3):
 			Number of opens = 6
 			Rows seen = 6
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Source result set:
 				Project-Restrict ResultSet (7):
 				Number of opens = 6
 				Rows seen = 12
 				Rows filtered = 6
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Union ResultSet:
 					Number of opens = 6
 					Rows seen from the left = 12
 					Rows seen from the right = 0
 					Rows returned = 12
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 					Left result set:
 						Union ResultSet:
 						Number of opens = 6
 						Rows seen from the left = 12
 						Rows seen from the right = 0
 						Rows returned = 12
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 						Left result set:
 							Union ResultSet:
 							Number of opens = 6
 							Rows seen from the left = 12
 							Rows seen from the right = 0
 							Rows returned = 12
 								constructor time (milliseconds) = 0
 								open time (milliseconds) = 0
 								next time (milliseconds) = 0
 								close time (milliseconds) = 0
 							Left result set:
 								Union ResultSet:
 								Number of opens = 6
 								Rows seen from the left = 6
 								Rows seen from the right = 6
 								Rows returned = 12
 									constructor time (milliseconds) = 0
 									open time (milliseconds) = 0
 									next time (milliseconds) = 0
 									close time (milliseconds) = 0
 								Left result set:
 									Row ResultSet:
 									Number of opens = 6
 									Rows returned = 6
 										constructor time (milliseconds) = 0
 										open time (milliseconds) = 0
 										next time (milliseconds) = 0
 										close time (milliseconds) = 0
 								Right result set:
 									Row ResultSet:
 									Number of opens = 6
 									Rows returned = 6
 										constructor time (milliseconds) = 0
 										open time (milliseconds) = 0
 										next time (milliseconds) = 0
 										close time (milliseconds) = 0
 							Right result set:
 								Row ResultSet:
 								Number of opens = 0
 								Rows returned = 0
 									constructor time (milliseconds) = 0
 									open time (milliseconds) = 0
 									next time (milliseconds) = 0
 									close time (milliseconds) = 0
 						Right result set:
 							Row ResultSet:
 							Number of opens = 0
 							Rows returned = 0
 								constructor time (milliseconds) = 0
 								open time (milliseconds) = 0
 								next time (milliseconds) = 0
 								close time (milliseconds) = 0
 					Right result set:
 						Row ResultSet:
 						Number of opens = 0
 						Rows returned = 0
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 			End Subquery Number 0
 		Project-Restrict ResultSet (3):
 		Number of opens = 1
 		Rows seen = 6
 		Rows filtered = 6
 		restriction = true
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 6
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0}
 				Number of columns fetched=1
 				Number of deleted rows visited=0
 				Number of pages visited=1
 				Number of rows qualified=6
 				Number of rows visited=6
 				Scan type=btree
 				Tree height=1
 				start position:
 					None
 				stop position:
 					None
 				qualifiers:
 					None
 ij> -- Now we make the other correlated column also nullable
 drop table docs;
 0 rows inserted/updated/deleted
 ij> CREATE TABLE "APP"."DOCS" ("ID" VARCHAR(128));
 0 rows inserted/updated/deleted
 ij> CREATE INDEX "APP"."NEW_INDEX1" ON "APP"."DOCS" ("ID");
 0 rows inserted/updated/deleted
 ij> insert into docs values '24', '25', '36', '27', '124', '567';
 6 rows inserted/updated/deleted
 ij> insert into docs values null;
 1 row inserted/updated/deleted
 ij> -- NOT EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( NOT EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB;
 1          
 -----------
 5          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- NOT EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( NOT EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 5
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 5
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Project-Restrict ResultSet (6):
 			Number of opens = 1
 			Rows seen = 5
 			Rows filtered = 0
 			restriction = false
 			projection = true
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Hash Exists Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 7
 				Rows seen from the right = 5
 				Rows filtered = 0
 				Rows returned = 5
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 7
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of deleted rows visited=0
 						Number of pages visited=1
 						Number of rows qualified=7
 						Number of rows visited=7
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							None
 						qualifiers:
 							None
 				Right result set:
 					Project-Restrict ResultSet (5):
 					Number of opens = 7
 					Rows seen = 3
 					Rows filtered = 1
 					restriction = true
 					projection = false
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						restriction time (milliseconds) = 0
 						projection time (milliseconds) = 0
 					Source result set:
 						Hash Scan ResultSet for COLLS at serializable isolation level using share table locking: 
 						Number of opens = 7
 						Hash table size = 6
 						Hash key is column number 0
 						Rows seen = 3
 						Rows filtered = 0
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information: 
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of pages visited=1
 							Number of rows qualified=9
 							Number of rows visited=9
 							Scan type=heap
 							start position:
 								null
 							stop position:
 								null
 							scan qualifiers:
 								None
 							next qualifiers:
 								Column[0][0] Id: 0
 								Operator: =
 								Ordered nulls: false
 								Unknown return value: false
 								Negate comparison result: false
 ij> -- EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB;
 1          
 -----------
 2          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- EXISTS should be flattened
 SELECT COUNT(*) FROM
 ( SELECT ID FROM DOCS WHERE
         ( EXISTS  (SELECT ID FROM COLLS WHERE DOCS.ID = COLLS.ID
 AND COLLID IN (-2,1) ) )
 ) AS TAB
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Project-Restrict ResultSet (6):
 			Number of opens = 1
 			Rows seen = 2
 			Rows filtered = 0
 			restriction = false
 			projection = true
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				restriction time (milliseconds) = 0
 				projection time (milliseconds) = 0
 			Source result set:
 				Hash Exists Join ResultSet:
 				Number of opens = 1
 				Rows seen from the left = 7
 				Rows seen from the right = 2
 				Rows filtered = 0
 				Rows returned = 2
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 				Left result set:
 					Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 					Number of opens = 1
 					Rows seen = 7
 					Rows filtered = 0
 					Fetch Size = 1
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information:
 						Bit set of columns fetched={0}
 						Number of columns fetched=1
 						Number of deleted rows visited=0
 						Number of pages visited=1
 						Number of rows qualified=7
 						Number of rows visited=7
 						Scan type=btree
 						Tree height=1
 						start position:
 							None
 						stop position:
 							None
 						qualifiers:
 							None
 				Right result set:
 					Project-Restrict ResultSet (5):
 					Number of opens = 7
 					Rows seen = 3
 					Rows filtered = 1
 					restriction = true
 					projection = false
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						restriction time (milliseconds) = 0
 						projection time (milliseconds) = 0
 					Source result set:
 						Hash Scan ResultSet for COLLS at serializable isolation level using share table locking: 
 						Number of opens = 7
 						Hash table size = 6
 						Hash key is column number 0
 						Rows seen = 3
 						Rows filtered = 0
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information: 
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of pages visited=1
 							Number of rows qualified=9
 							Number of rows visited=9
 							Scan type=heap
 							start position:
 								null
 							stop position:
 								null
 							scan qualifiers:
 								None
 							next qualifiers:
 								Column[0][0] Id: 0
 								Operator: =
 								Ordered nulls: false
 								Unknown return value: false
 								Negate comparison result: false
 ij> -- IN should be flattened
 SELECT count(ID) FROM DOCS WHERE ID IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 2          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- IN should be flattened
 SELECT count(ID) FROM DOCS WHERE ID IN (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (7):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 2
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (6):
 		Number of opens = 1
 		Rows seen = 2
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Hash Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 7
 			Rows seen from the right = 2
 			Rows filtered = 0
 			Rows returned = 2
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 7
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=7
 					Number of rows visited=7
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (5):
 				Number of opens = 7
 				Rows seen = 3
 				Rows filtered = 1
 				restriction = true
 				projection = false
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Hash Scan ResultSet for COLLS at serializable isolation level using share table locking: 
 					Number of opens = 7
 					Hash table size = 6
 					Hash key is column number 0
 					Rows seen = 3
 					Rows filtered = 0
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						next time in milliseconds/row = 0
 					scan information: 
 						Bit set of columns fetched=All
 						Number of columns fetched=2
 						Number of pages visited=1
 						Number of rows qualified=9
 						Number of rows visited=9
 						Scan type=heap
 						start position:
 							null
 						stop position:
 							null
 						scan qualifiers:
 							None
 						next qualifiers:
 							Column[0][0] Id: 0
 							Operator: =
 							Ordered nulls: false
 							Unknown return value: false
 							Negate comparison result: false
 ij> -- ANY should be flattened
 SELECT count(ID) FROM DOCS WHERE ID > ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 4          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ANY should be flattened
 SELECT count(ID) FROM DOCS WHERE ID > ANY (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (8):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 4
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Project-Restrict ResultSet (7):
 		Number of opens = 1
 		Rows seen = 4
 		Rows filtered = 0
 		restriction = false
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Nested Loop Exists Join ResultSet:
 			Number of opens = 1
 			Rows seen from the left = 7
 			Rows seen from the right = 4
 			Rows filtered = 0
 			Rows returned = 4
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Left result set:
 				Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 				Number of opens = 1
 				Rows seen = 7
 				Rows filtered = 0
 				Fetch Size = 16
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					next time in milliseconds/row = 0
 				scan information:
 					Bit set of columns fetched={0}
 					Number of columns fetched=1
 					Number of deleted rows visited=0
 					Number of pages visited=1
 					Number of rows qualified=7
 					Number of rows visited=7
 					Scan type=btree
 					Tree height=1
 					start position:
 						None
 					stop position:
 						None
 					qualifiers:
 						None
 			Right result set:
 				Project-Restrict ResultSet (6):
 				Number of opens = 7
 				Rows seen = 21
 				Rows filtered = 17
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Index Row to Base Row ResultSet for COLLS:
 					Number of opens = 7
 					Rows seen = 21
 					Columns accessed from heap = {0}
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 						Index Scan ResultSet for COLLS using index NEW_INDEX3 at serializable isolation level using share row locking chosen by the optimizer
 						Number of opens = 11
 						Rows seen = 21
 						Rows filtered = 0
 						Fetch Size = 1
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 							next time in milliseconds/row = 0
 						scan information:
 							Bit set of columns fetched=All
 							Number of columns fetched=2
 							Number of deleted rows visited=0
 							Number of pages visited=11
 							Number of rows qualified=21
 							Number of rows visited=28
 							Scan type=btree
 							Tree height=1
 							start position:
 								>= on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							stop position:
 								> on first 1 column(s).
 								Ordered null semantics on the following columns: 
 							qualifiers:
 								None
 ij> -- ALL should NOT be flattened, but subquery should be materialized, watch out results
 SELECT count(ID) FROM DOCS WHERE ID <> ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) );
 1          
 -----------
 0          
 ij> values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- ALL should NOT be flattened, but subquery should be materialized, watch out results
 SELECT count(ID) FROM DOCS WHERE ID <> ALL (SELECT ID FROM COLLS WHERE COLLID IN (-2,1) )
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (9):
 Number of opens = 1
 Rows seen = 1
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Scalar Aggregate ResultSet:
 	Number of opens = 1
 	Rows input = 0
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Index Key Optimization = false
 	Source result set:
 		Attached subqueries:
 			Begin Subquery Number 0
 			Any ResultSet  (Attached to 3):
 			Number of opens = 7
 			Rows seen = 7
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Source result set:
 				Project-Restrict ResultSet (7):
 				Number of opens = 7
 				Rows seen = 13
 				Rows filtered = 6
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Union ResultSet:
 					Number of opens = 7
 					Rows seen from the left = 13
 					Rows seen from the right = 0
 					Rows returned = 13
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 					Left result set:
 						Union ResultSet:
 						Number of opens = 7
 						Rows seen from the left = 13
 						Rows seen from the right = 0
 						Rows returned = 13
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 						Left result set:
 							Union ResultSet:
 							Number of opens = 7
 							Rows seen from the left = 13
 							Rows seen from the right = 0
 							Rows returned = 13
 								constructor time (milliseconds) = 0
 								open time (milliseconds) = 0
 								next time (milliseconds) = 0
 								close time (milliseconds) = 0
 							Left result set:
 								Union ResultSet:
 								Number of opens = 7
 								Rows seen from the left = 7
 								Rows seen from the right = 6
 								Rows returned = 13
 									constructor time (milliseconds) = 0
 									open time (milliseconds) = 0
 									next time (milliseconds) = 0
 									close time (milliseconds) = 0
 								Left result set:
 									Row ResultSet:
 									Number of opens = 7
 									Rows returned = 7
 										constructor time (milliseconds) = 0
 										open time (milliseconds) = 0
 										next time (milliseconds) = 0
 										close time (milliseconds) = 0
 								Right result set:
 									Row ResultSet:
 									Number of opens = 6
 									Rows returned = 6
 										constructor time (milliseconds) = 0
 										open time (milliseconds) = 0
 										next time (milliseconds) = 0
 										close time (milliseconds) = 0
 							Right result set:
 								Row ResultSet:
 								Number of opens = 0
 								Rows returned = 0
 									constructor time (milliseconds) = 0
 									open time (milliseconds) = 0
 									next time (milliseconds) = 0
 									close time (milliseconds) = 0
 						Right result set:
 							Row ResultSet:
 							Number of opens = 0
 							Rows returned = 0
 								constructor time (milliseconds) = 0
 								open time (milliseconds) = 0
 								next time (milliseconds) = 0
 								close time (milliseconds) = 0
 					Right result set:
 						Row ResultSet:
 						Number of opens = 0
 						Rows returned = 0
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 			End Subquery Number 0
 		Project-Restrict ResultSet (3):
 		Number of opens = 1
 		Rows seen = 7
 		Rows filtered = 7
 		restriction = true
 		projection = true
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			restriction time (milliseconds) = 0
 			projection time (milliseconds) = 0
 		Source result set:
 			Index Scan ResultSet for DOCS using index NEW_INDEX1 at serializable isolation level using share table locking chosen by the optimizer
 			Number of opens = 1
 			Rows seen = 7
 			Rows filtered = 0
 			Fetch Size = 16
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 				next time in milliseconds/row = 0
 			scan information:
 				Bit set of columns fetched={0}
 				Number of columns fetched=1
 				Number of deleted rows visited=0
 				Number of pages visited=1
 				Number of rows qualified=7
 				Number of rows visited=7
 				Scan type=btree
 				Tree height=1
 				start position:
 					None
 				stop position:
 					None
 				qualifiers:
 					None
 ij> drop table t1;
 ERROR 42Y55: 'DROP TABLE' cannot be performed on 'T1' because it does not exist.
 ij> drop table t2;
 ERROR 42Y55: 'DROP TABLE' cannot be performed on 'T2' because it does not exist.
 ij> drop table t3;
 ERROR 42Y55: 'DROP TABLE' cannot be performed on 'T3' because it does not exist.
 ij> drop table t4;
 ERROR 42Y55: 'DROP TABLE' cannot be performed on 'T4' because it does not exist.
 ij> create table t1 (c1 int not null);
 0 rows inserted/updated/deleted
 ij> create table t2 (c1 int not null);
 0 rows inserted/updated/deleted
 ij> create table t3 (c1 int not null);
 0 rows inserted/updated/deleted
 ij> create table t4 (c1 int);
 0 rows inserted/updated/deleted
 ij> insert into t1 values 1,2,3,4,5,1,2;
 7 rows inserted/updated/deleted
 ij> insert into t2 values 1,4,5,1,1,5,4;
 7 rows inserted/updated/deleted
 ij> insert into t3 values 4,4,3,3;
 4 rows inserted/updated/deleted
 ij> insert into t4 values 1,1,2,2,3,4,5,5;
 8 rows inserted/updated/deleted
 ij> -- should return 2,3,2
 select * from t1 where not exists (select * from t2 where t1.c1=t2.c1);
 C1         
 -----------
 2          
 3          
 2          
 ij> -- should be flattened
 values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	-- should return 2,3,2
 select * from t1 where not exists (select * from t2 where t1.c1=t2.c1)
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Project-Restrict ResultSet (4):
 Number of opens = 1
 Rows seen = 3
 Rows filtered = 0
 restriction = false
 projection = true
 	constructor time (milliseconds) = 0
 	open time (milliseconds) = 0
 	next time (milliseconds) = 0
 	close time (milliseconds) = 0
 	restriction time (milliseconds) = 0
 	projection time (milliseconds) = 0
 Source result set:
 	Hash Exists Join ResultSet:
 	Number of opens = 1
 	Rows seen from the left = 7
 	Rows seen from the right = 3
 	Rows filtered = 0
 	Rows returned = 3
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Left result set:
 		Table Scan ResultSet for T1 at serializable isolation level using share table locking chosen by the optimizer
 		Number of opens = 1
 		Rows seen = 7
 		Rows filtered = 0
 		Fetch Size = 16
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information:
 			Bit set of columns fetched=All
 			Number of columns fetched=1
 			Number of pages visited=1
 			Number of rows qualified=7
 			Number of rows visited=7
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			qualifiers:
 				None
 	Right result set:
 		Hash Scan ResultSet for T2 at serializable isolation level using share table locking: 
 		Number of opens = 7
 		Hash table size = 3
 		Hash key is column number 0
 		Rows seen = 4
 		Rows filtered = 0
 			constructor time (milliseconds) = 0
 			open time (milliseconds) = 0
 			next time (milliseconds) = 0
 			close time (milliseconds) = 0
 			next time in milliseconds/row = 0
 		scan information: 
 			Bit set of columns fetched=All
 			Number of columns fetched=1
 			Number of pages visited=1
 			Number of rows qualified=7
 			Number of rows visited=7
 			Scan type=heap
 			start position:
 				null
 			stop position:
 				null
 			scan qualifiers:
 				None
 			next qualifiers:
 				Column[0][0] Id: 0
 				Operator: =
 				Ordered nulls: false
 				Unknown return value: false
 				Negate comparison result: false
 ij> select * from t1 where not exists (select * from t2 where t1.c1=t2.c1 and t2.c1 not in (select t3.c1 from t3, t4));
 C1         
 -----------
 2          
 3          
 4          
 2          
 ij> -- watch out result, should return 2,3,4,2
 -- can not be flattened, should be materialized
 values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select * from t1 where not exists (select * from t2 where t1.c1=t2.c1 and t2.c1 not in (select t3.c1 from t3, t4))
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Attached subqueries:
 	Begin Subquery Number 0
 	Any ResultSet  (Attached to 2):
 	Number of opens = 7
 	Rows seen = 7
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Source result set:
 		Attached subqueries:
 			Begin Subquery Number 1
 			Any ResultSet  (Attached to 4):
 			Number of opens = 5
 			Rows seen = 5
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Source result set:
 				Project-Restrict ResultSet (8):
 				Number of opens = 5
 				Rows seen = 98
 				Rows filtered = 96
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Union ResultSet:
 					Number of opens = 5
 					Rows seen from the left = 95
 					Rows seen from the right = 3
 					Rows returned = 98
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 					Left result set:
 						Union ResultSet:
 						Number of opens = 5
 						Rows seen from the left = 92
 						Rows seen from the right = 3
 						Rows returned = 95
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 						Left result set:
 							Union ResultSet:
 							Number of opens = 5
 							Rows seen from the left = 89
 							Rows seen from the right = 3
 							Rows returned = 92
 								constructor time (milliseconds) = 0
 								open time (milliseconds) = 0
 								next time (milliseconds) = 0
 								close time (milliseconds) = 0
 							Left result set:
 								Union ResultSet:
 								Number of opens = 5
 								Rows seen from the left = 86
 								Rows seen from the right = 3
 								Rows returned = 89
 									constructor time (milliseconds) = 0
 									open time (milliseconds) = 0
 									next time (milliseconds) = 0
 									close time (milliseconds) = 0
 								Left result set:
 									Union ResultSet:
 									Number of opens = 5
 									Rows seen from the left = 83
 									Rows seen from the right = 3
 									Rows returned = 86
 										constructor time (milliseconds) = 0
 										open time (milliseconds) = 0
 										next time (milliseconds) = 0
 										close time (milliseconds) = 0
 									Left result set:
 										Union ResultSet:
 										Number of opens = 5
 										Rows seen from the left = 80
 										Rows seen from the right = 3
 										Rows returned = 83
 											constructor time (milliseconds) = 0
 											open time (milliseconds) = 0
 											next time (milliseconds) = 0
 											close time (milliseconds) = 0
 										Left result set:
 											Union ResultSet:
 											Number of opens = 5
 											Rows seen from the left = 77
 											Rows seen from the right = 3
 											Rows returned = 80
 												constructor time (milliseconds) = 0
 												open time (milliseconds) = 0
 												next time (milliseconds) = 0
 												close time (milliseconds) = 0
 											Left result set:
 												Union ResultSet:
 												Number of opens = 5
 												Rows seen from the left = 74
 												Rows seen from the right = 3
 												Rows returned = 77
 													constructor time (milliseconds) = 0
 													open time (milliseconds) = 0
 													next time (milliseconds) = 0
 													close time (milliseconds) = 0
 												Left result set:
 													Union ResultSet:
 													Number of opens = 5
 													Rows seen from the left = 71
 													Rows seen from the right = 3
 													Rows returned = 74
 														constructor time (milliseconds) = 0
 														open time (milliseconds) = 0
 														next time (milliseconds) = 0
 														close time (milliseconds) = 0
 													Left result set:
 														Union ResultSet:
 														Number of opens = 5
 														Rows seen from the left = 68
 														Rows seen from the right = 3
 														Rows returned = 71
 															constructor time (milliseconds) = 0
 															open time (milliseconds) = 0
 															next time (milliseconds) = 0
 															close time (milliseconds) = 0
 														Left result set:
 															Union ResultSet:
 															Number of opens = 5
 															Rows seen from the left = 65
 															Rows seen from the right = 3
 															Rows returned = 68
 																constructor time (milliseconds) = 0
 																open time (milliseconds) = 0
 																next time (milliseconds) = 0
 																close time (milliseconds) = 0
 															Left result set:
 																Union ResultSet:
 																Number of opens = 5
 																Rows seen from the left = 62
 																Rows seen from the right = 3
 																Rows returned = 65
 																	constructor time (milliseconds) = 0
 																	open time (milliseconds) = 0
 																	next time (milliseconds) = 0
 																	close time (milliseconds) = 0
 																Left result set:
 																	Union ResultSet:
 																	Number of opens = 5
 																	Rows seen from the left = 59
 																	Rows seen from the right = 3
 																	Rows returned = 62
 																		constructor time (milliseconds) = 0
 																		open time (milliseconds) = 0
 																		next time (milliseconds) = 0
 																		close time (milliseconds) = 0
 																	Left result set:
 																		Union ResultSet:
 																		Number of opens = 5
 																		Rows seen from the left = 56
 																		Rows seen from the right = 3
 																		Rows returned = 59
 																			constructor time (milliseconds) = 0
 																			open time (milliseconds) = 0
 																			next time (milliseconds) = 0
 																			close time (milliseconds) = 0
 																		Left result set:
 																			Union ResultSet:
 																			Number of opens = 5
 																			Rows seen from the left = 53
 																			Rows seen from the right = 3
 																			Rows returned = 56
 																				constructor time (milliseconds) = 0
 																				open time (milliseconds) = 0
 																				next time (milliseconds) = 0
 																				close time (milliseconds) = 0
 																			Left result set:
 																				Union ResultSet:
 																				Number of opens = 5
 																				Rows seen from the left = 50
 																				Rows seen from the right = 3
 																				Rows returned = 53
 																					constructor time (milliseconds) = 0
 																					open time (milliseconds) = 0
 																					next time (milliseconds) = 0
 																					close time (milliseconds) = 0
 																				Left result set:
 																					Union ResultSet:
 																					Number of opens = 5
 																					Rows seen from the left = 47
 																					Rows seen from the right = 3
 																					Rows returned = 50
 																						constructor time (milliseconds) = 0
 																						open time (milliseconds) = 0
 																						next time (milliseconds) = 0
 																						close time (milliseconds) = 0
 																					Left result set:
 																						Union ResultSet:
 																						Number of opens = 5
 																						Rows seen from the left = 44
 																						Rows seen from the right = 3
 																						Rows returned = 47
 																							constructor time (milliseconds) = 0
 																							open time (milliseconds) = 0
 																							next time (milliseconds) = 0
 																							close time (milliseconds) = 0
 																						Left result set:
 																							Union ResultSet:
 																							Number of opens = 5
 																							Rows seen from the left = 41
 																							Rows seen from the right = 3
 																							Rows returned = 44
 																								constructor time (milliseconds) = 0
 																								open time (milliseconds) = 0
 																								next time (milliseconds) = 0
 																								close time (milliseconds) = 0
 																							Left result set:
 																								Union ResultSet:
 																								Number of opens = 5
 																								Rows seen from the left = 38
 																								Rows seen from the right = 3
 																								Rows returned = 41
 																									constructor time (milliseconds) = 0
 																									open time (milliseconds) = 0
 																									next time (milliseconds) = 0
 																									close time (milliseconds) = 0
 																								Left result set:
 																									Union ResultSet:
 																									Number of opens = 5
 																									Rows seen from the left = 35
 																									Rows seen from the right = 3
 																									Rows returned = 38
 																										constructor time (milliseconds) = 0
 																										open time (milliseconds) = 0
 																										next time (milliseconds) = 0
 																										close time (milliseconds) = 0
 																									Left result set:
 																										Union ResultSet:
 																										Number of opens = 5
 																										Rows seen from the left = 32
 																										Rows seen from the right = 3
 																										Rows returned = 35
 																											constructor time (milliseconds) = 0
 																											open time (milliseconds) = 0
 																											next time (milliseconds) = 0
 																											close time (milliseconds) = 0
 																										Left result set:
 																											Union ResultSet:
 																											Number of opens = 5
 																											Rows seen from the left = 29
 																											Rows seen from the right = 3
 																											Rows returned = 32
 																												constructor time (milliseconds) = 0
 																												open time (milliseconds) = 0
 																												next time (milliseconds) = 0
 																												close time (milliseconds) = 0
 																											Left result set:
 																												Union ResultSet:
 																												Number of opens = 5
 																												Rows seen from the left = 26
 																												Rows seen from the right = 3
 																												Rows returned = 29
 																													constructor time (milliseconds) = 0
 																													open time (milliseconds) = 0
 																													next time (milliseconds) = 0
 																													close time (milliseconds) = 0
 																												Left result set:
 																													Union ResultSet:
 																													Number of opens = 5
 																													Rows seen from the left = 23
 																													Rows seen from the right = 3
 																													Rows returned = 26
 																														constructor time (milliseconds) = 0
 																														open time (milliseconds) = 0
 																														next time (milliseconds) = 0
 																														close time (milliseconds) = 0
 																													Left result set:
 																														Union ResultSet:
 																														Number of opens = 5
 																														Rows seen from the left = 20
 																														Rows seen from the right = 3
 																														Rows returned = 23
 																															constructor time (milliseconds) = 0
 																															open time (milliseconds) = 0
 																															next time (milliseconds) = 0
 																															close time (milliseconds) = 0
 																														Left result set:
 																															Union ResultSet:
 																															Number of opens = 5
 																															Rows seen from the left = 17
 																															Rows seen from the right = 3
 																															Rows returned = 20
 																																constructor time (milliseconds) = 0
 																																open time (milliseconds) = 0
 																																next time (milliseconds) = 0
 																																close time (milliseconds) = 0
 																															Left result set:
 																																Union ResultSet:
 																																Number of opens = 5
 																																Rows seen from the left = 14
 																																Rows seen from the right = 3
 																																Rows returned = 17
 																																	constructor time (milliseconds) = 0
 																																	open time (milliseconds) = 0
 																																	next time (milliseconds) = 0
 																																	close time (milliseconds) = 0
 																																Left result set:
 																																	Union ResultSet:
 																																	Number of opens = 5
 																																	Rows seen from the left = 11
 																																	Rows seen from the right = 3
 																																	Rows returned = 14
 																																		constructor time (milliseconds) = 0
 																																		open time (milliseconds) = 0
 																																		next time (milliseconds) = 0
 																																		close time (milliseconds) = 0
 																																	Left result set:
 																																		Union ResultSet:
 																																		Number of opens = 5
 																																		Rows seen from the left = 8
 																																		Rows seen from the right = 3
 																																		Rows returned = 11
 																																			constructor time (milliseconds) = 0
 																																			open time (milliseconds) = 0
 																																			next time (milliseconds) = 0
 																																			close time (milliseconds) = 0
 																																		Left result set:
 																																			Union ResultSet:
 																																			Number of opens = 5
 																																			Rows seen from the left = 5
 																																			Rows seen from the right = 3
 																																			Rows returned = 8
 																																				constructor time (milliseconds) = 0
 																																				open time (milliseconds) = 0
 																																				next time (milliseconds) = 0
 																																				close time (milliseconds) = 0
 																																			Left result set:
 																																				Row ResultSet:
 																																				Number of opens = 5
 																																				Rows returned = 5
 																																					constructor time (milliseconds) = 0
 																																					open time (milliseconds) = 0
 																																					next time (milliseconds) = 0
 																																					close time (milliseconds) = 0
 																																			Right result set:
 																																				Row ResultSet:
 																																				Number of opens = 3
 																																				Rows returned = 3
 																																					constructor time (milliseconds) = 0
 																																					open time (milliseconds) = 0
 																																					next time (milliseconds) = 0
 																																					close time (milliseconds) = 0
 																																		Right result set:
 																																			Row ResultSet:
 																																			Number of opens = 3
 																																			Rows returned = 3
 																																				constructor time (milliseconds) = 0
 																																				open time (milliseconds) = 0
 																																				next time (milliseconds) = 0
 																																				close time (milliseconds) = 0
 																																	Right result set:
 																																		Row ResultSet:
 																																		Number of opens = 3
 																																		Rows returned = 3
 																																			constructor time (milliseconds) = 0
 																																			open time (milliseconds) = 0
 																																			next time (milliseconds) = 0
 																																			close time (milliseconds) = 0
 																																Right result set:
 																																	Row ResultSet:
 																																	Number of opens = 3
 																																	Rows returned = 3
 																																		constructor time (milliseconds) = 0
 																																		open time (milliseconds) = 0
 																																		next time (milliseconds) = 0
 																																		close time (milliseconds) = 0
 																															Right result set:
 																																Row ResultSet:
 																																Number of opens = 3
 																																Rows returned = 3
 																																	constructor time (milliseconds) = 0
 																																	open time (milliseconds) = 0
 																																	next time (milliseconds) = 0
 																																	close time (milliseconds) = 0
 																														Right result set:
 																															Row ResultSet:
 																															Number of opens = 3
 																															Rows returned = 3
 																																constructor time (milliseconds) = 0
 																																open time (milliseconds) = 0
 																																next time (milliseconds) = 0
 																																close time (milliseconds) = 0
 																													Right result set:
 																														Row ResultSet:
 																														Number of opens = 3
 																														Rows returned = 3
 																															constructor time (milliseconds) = 0
 																															open time (milliseconds) = 0
 																															next time (milliseconds) = 0
 																															close time (milliseconds) = 0
 																												Right result set:
 																													Row ResultSet:
 																													Number of opens = 3
 																													Rows returned = 3
 																														constructor time (milliseconds) = 0
 																														open time (milliseconds) = 0
 																														next time (milliseconds) = 0
 																														close time (milliseconds) = 0
 																											Right result set:
 																												Row ResultSet:
 																												Number of opens = 3
 																												Rows returned = 3
 																													constructor time (milliseconds) = 0
 																													open time (milliseconds) = 0
 																													next time (milliseconds) = 0
 																													close time (milliseconds) = 0
 																										Right result set:
 																											Row ResultSet:
 																											Number of opens = 3
 																											Rows returned = 3
 																												constructor time (milliseconds) = 0
 																												open time (milliseconds) = 0
 																												next time (milliseconds) = 0
 																												close time (milliseconds) = 0
 																									Right result set:
 																										Row ResultSet:
 																										Number of opens = 3
 																										Rows returned = 3
 																											constructor time (milliseconds) = 0
 																											open time (milliseconds) = 0
 																											next time (milliseconds) = 0
 																											close time (milliseconds) = 0
 																								Right result set:
 																									Row ResultSet:
 																									Number of opens = 3
 																									Rows returned = 3
 																										constructor time (milliseconds) = 0
 																										open time (milliseconds) = 0
 																										next time (milliseconds) = 0
 																										close time (milliseconds) = 0
 																							Right result set:
 																								Row ResultSet:
 																								Number of opens = 3
 																								Rows returned = 3
 																									constructor time (milliseconds) = 0
 																									open time (milliseconds) = 0
 																									next time (milliseconds) = 0
 																									close time (milliseconds) = 0
 																						Right result set:
 																							Row ResultSet:
 																							Number of opens = 3
 																							Rows returned = 3
 																								constructor time (milliseconds) = 0
 																								open time (milliseconds) = 0
 																								next time (milliseconds) = 0
 																								close time (milliseconds) = 0
 																					Right result set:
 																						Row ResultSet:
 																						Number of opens = 3
 																						Rows returned = 3
 																							constructor time (milliseconds) = 0
 																							open time (milliseconds) = 0
 																							next time (milliseconds) = 0
 																							close time (milliseconds) = 0
 																				Right result set:
 																					Row ResultSet:
 																					Number of opens = 3
 																					Rows returned = 3
 																						constructor time (milliseconds) = 0
 																						open time (milliseconds) = 0
 																						next time (milliseconds) = 0
 																						close time (milliseconds) = 0
 																			Right result set:
 																				Row ResultSet:
 																				Number of opens = 3
 																				Rows returned = 3
 																					constructor time (milliseconds) = 0
 																					open time (milliseconds) = 0
 																					next time (milliseconds) = 0
 																					close time (milliseconds) = 0
 																		Right result set:
 																			Row ResultSet:
 																			Number of opens = 3
 																			Rows returned = 3
 																				constructor time (milliseconds) = 0
 																				open time (milliseconds) = 0
 																				next time (milliseconds) = 0
 																				close time (milliseconds) = 0
 																	Right result set:
 																		Row ResultSet:
 																		Number of opens = 3
 																		Rows returned = 3
 																			constructor time (milliseconds) = 0
 																			open time (milliseconds) = 0
 																			next time (milliseconds) = 0
 																			close time (milliseconds) = 0
 																Right result set:
 																	Row ResultSet:
 																	Number of opens = 3
 																	Rows returned = 3
 																		constructor time (milliseconds) = 0
 																		open time (milliseconds) = 0
 																		next time (milliseconds) = 0
 																		close time (milliseconds) = 0
 															Right result set:
 																Row ResultSet:
 																Number of opens = 3
 																Rows returned = 3
 																	constructor time (milliseconds) = 0
 																	open time (milliseconds) = 0
 																	next time (milliseconds) = 0
 																	close time (milliseconds) = 0
 														Right result set:
 															Row ResultSet:
 															Number of opens = 3
 															Rows returned = 3
 																constructor time (milliseconds) = 0
 																open time (milliseconds) = 0
 																next time (milliseconds) = 0
 																close time (milliseconds) = 0
 													Right result set:
 														Row ResultSet:
														Number of opens = 3
														Rows returned = 3
															constructor time (milliseconds) = 0
															open time (milliseconds) = 0
															next time (milliseconds) = 0
															close time (milliseconds) = 0
												Right result set:
													Row ResultSet:
													Number of opens = 3
													Rows returned = 3
														constructor time (milliseconds) = 0
														open time (milliseconds) = 0
														next time (milliseconds) = 0
														close time (milliseconds) = 0
											Right result set:
												Row ResultSet:
												Number of opens = 3
												Rows returned = 3
													constructor time (milliseconds) = 0
													open time (milliseconds) = 0
													next time (milliseconds) = 0
													close time (milliseconds) = 0
										Right result set:
											Row ResultSet:
											Number of opens = 3
											Rows returned = 3
												constructor time (m
 ij> select * from t1 where exists (select * from t2 where t1.c1=t2.c1 and t2.c1 not in (select t3.c1 from t3, t4));
 C1         
 -----------
 1          
 5          
 1          
 ij> -- should return 1,5,1
 -- can not be flattened, should be materialized
 values SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS();
 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Statement Name: 
 	null
 Statement Text: 
 	select * from t1 where exists (select * from t2 where t1.c1=t2.c1 and t2.c1 not in (select t3.c1 from t3, t4))
 Parse Time: 0
 Bind Time: 0
 Optimize Time: 0
 Generate Time: 0
 Compile Time: 0
 Execute Time: 0
 Begin Compilation Timestamp : null
 End Compilation Timestamp : null
 Begin Execution Timestamp : null
 End Execution Timestamp : null
 Statement Execution Plan Text: 
 Attached subqueries:
 	Begin Subquery Number 0
 	Any ResultSet  (Attached to 2):
 	Number of opens = 7
 	Rows seen = 7
 		constructor time (milliseconds) = 0
 		open time (milliseconds) = 0
 		next time (milliseconds) = 0
 		close time (milliseconds) = 0
 	Source result set:
 		Attached subqueries:
 			Begin Subquery Number 1
 			Any ResultSet  (Attached to 4):
 			Number of opens = 5
 			Rows seen = 5
 				constructor time (milliseconds) = 0
 				open time (milliseconds) = 0
 				next time (milliseconds) = 0
 				close time (milliseconds) = 0
 			Source result set:
 				Project-Restrict ResultSet (8):
 				Number of opens = 5
 				Rows seen = 98
 				Rows filtered = 96
 				restriction = true
 				projection = true
 					constructor time (milliseconds) = 0
 					open time (milliseconds) = 0
 					next time (milliseconds) = 0
 					close time (milliseconds) = 0
 					restriction time (milliseconds) = 0
 					projection time (milliseconds) = 0
 				Source result set:
 					Union ResultSet:
 					Number of opens = 5
 					Rows seen from the left = 95
 					Rows seen from the right = 3
 					Rows returned = 98
 						constructor time (milliseconds) = 0
 						open time (milliseconds) = 0
 						next time (milliseconds) = 0
 						close time (milliseconds) = 0
 					Left result set:
 						Union ResultSet:
 						Number of opens = 5
 						Rows seen from the left = 92
 						Rows seen from the right = 3
 						Rows returned = 95
 							constructor time (milliseconds) = 0
 							open time (milliseconds) = 0
 							next time (milliseconds) = 0
 							close time (milliseconds) = 0
 						Left result set:
 							Union ResultSet:
 							Number of opens = 5
 							Rows seen from the left = 89
 							Rows seen from the right = 3
 							Rows returned = 92
 								constructor time (milliseconds) = 0
 								open time (milliseconds) = 0
 								next time (milliseconds) = 0
 								close time (milliseconds) = 0
 							Left result set:
 								Union ResultSet:
 								Number of opens = 5
 								Rows seen from the left = 86
 								Rows seen from the right = 3
 								Rows returned = 89
 									constructor time (milliseconds) = 0
 									open time (milliseconds) = 0
 									next time (milliseconds) = 0
 									close time (milliseconds) = 0
 								Left result set:
 									Union ResultSet:
 									Number of opens = 5
 									Rows seen from the left = 83
 									Rows seen from the right = 3
 									Rows returned = 86
 										constructor time (milliseconds) = 0
 										open time (milliseconds) = 0
 										next time (milliseconds) = 0
 										close time (milliseconds) = 0
 									Left result set:
 										Union ResultSet:
 										Number of opens = 5
 										Rows seen from the left = 80
 										Rows seen from the right = 3
 										Rows returned = 83
 											constructor time (milliseconds) = 0
 											open time (milliseconds) = 0
 											next time (milliseconds) = 0
 											close time (milliseconds) = 0
 										Left result set:
 											Union ResultSet:
 											Number of opens = 5
 											Rows seen from the left = 77
 											Rows seen from the right = 3
 											Rows returned = 80
 												constructor time (milliseconds) = 0
 												open time (milliseconds) = 0
 												next time (milliseconds) = 0
 												close time (milliseconds) = 0
 											Left result set:
 												Union ResultSet:
 												Number of opens = 5
 												Rows seen from the left = 74
 												Rows seen from the right = 3
 												Rows returned = 77
 													constructor time (milliseconds) = 0
 													open time (milliseconds) = 0
 													next time (milliseconds) = 0
 													close time (milliseconds) = 0
 												Left result set:
 													Union ResultSet:
 													Number of opens = 5
 													Rows seen from the left = 71
 													Rows seen from the right = 3
 													Rows returned = 74
 														constructor time (milliseconds) = 0
 														open time (milliseconds) = 0
 														next time (milliseconds) = 0
 														close time (milliseconds) = 0
 													Left result set:
 														Union ResultSet:
 														Number of opens = 5
 														Rows seen from the left = 68
 														Rows seen from the right = 3
 														Rows returned = 71
 															constructor time (milliseconds) = 0
 															open time (milliseconds) = 0
 															next time (milliseconds) = 0
 															close time (milliseconds) = 0
 														Left result set:
 															Union ResultSet:
 															Number of opens = 5
 															Rows seen from the left = 65
 															Rows seen from the right = 3
 															Rows returned = 68
 																constructor time (milliseconds) = 0
 																open time (milliseconds) = 0
 																next time (milliseconds) = 0
 																close time (milliseconds) = 0
 															Left result set:
 																Union ResultSet:
 																Number of opens = 5
 																Rows seen from the left = 62
 																Rows seen from the right = 3
 																Rows returned = 65
 																	constructor time (milliseconds) = 0
 																	open time (milliseconds) = 0
 																	next time (milliseconds) = 0
 																	close time (milliseconds) = 0
 																Left result set:
 																	Union ResultSet:
 																	Number of opens = 5
 																	Rows seen from the left = 59
 																	Rows seen from the right = 3
 																	Rows returned = 62
 																		constructor time (milliseconds) = 0
 																		open time (milliseconds) = 0
 																		next time (milliseconds) = 0
 																		close time (milliseconds) = 0
 																	Left result set:
 																		Union ResultSet:
 																		Number of opens = 5
 																		Rows seen from the left = 56
 																		Rows seen from the right = 3
 																		Rows returned = 59
 																			constructor time (milliseconds) = 0
 																			open time (milliseconds) = 0
 																			next time (milliseconds) = 0
 																			close time (milliseconds) = 0
 																		Left result set:
 																			Union ResultSet:
 																			Number of opens = 5
 																			Rows seen from the left = 53
 																			Rows seen from the right = 3
 																			Rows returned = 56
 																				constructor time (milliseconds) = 0
 																				open time (milliseconds) = 0
 																				next time (milliseconds) = 0
 																				close time (milliseconds) = 0
 																			Left result set:
 																				Union ResultSet:
 																				Number of opens = 5
 																				Rows seen from the left = 50
 																				Rows seen from the right = 3
 																				Rows returned = 53
 																					constructor time (milliseconds) = 0
 																					open time (milliseconds) = 0
 																					next time (milliseconds) = 0
 																					close time (milliseconds) = 0
 																				Left result set:
 																					Union ResultSet:
 																					Number of opens = 5
 																					Rows seen from the left = 47
 																					Rows seen from the right = 3
 																					Rows returned = 50
 																						constructor time (milliseconds) = 0
 																						open time (milliseconds) = 0
 																						next time (milliseconds) = 0
 																						close time (milliseconds) = 0
 																					Left result set:
 																						Union ResultSet:
 																						Number of opens = 5
 																						Rows seen from the left = 44
 																						Rows seen from the right = 3
 																						Rows returned = 47
 																							constructor time (milliseconds) = 0
 																							open time (milliseconds) = 0
 																							next time (milliseconds) = 0
 																							close time (milliseconds) = 0
 																						Left result set:
 																							Union ResultSet:
 																							Number of opens = 5
 																							Rows seen from the left = 41
 																							Rows seen from the right = 3
 																							Rows returned = 44
 																								constructor time (milliseconds) = 0
 																								open time (milliseconds) = 0
 																								next time (milliseconds) = 0
 																								close time (milliseconds) = 0
 																							Left result set:
 																								Union ResultSet:
 																								Number of opens = 5
 																								Rows seen from the left = 38
 																								Rows seen from the right = 3
 																								Rows returned = 41
 																									constructor time (milliseconds) = 0
 																									open time (milliseconds) = 0
 																									next time (milliseconds) = 0
 																									close time (milliseconds) = 0
 																								Left result set:
 																									Union ResultSet:
 																									Number of opens = 5
 																									Rows seen from the left = 35
 																									Rows seen from the right = 3
 																									Rows returned = 38
 																										constructor time (milliseconds) = 0
 																										open time (milliseconds) = 0
 																										next time (milliseconds) = 0
 																										close time (milliseconds) = 0
 																									Left result set:
 																										Union ResultSet:
 																										Number of opens = 5
 																										Rows seen from the left = 32
 																										Rows seen from the right = 3
 																										Rows returned = 35
 																											constructor time (milliseconds) = 0
 																											open time (milliseconds) = 0
 																											next time (milliseconds) = 0
 																											close time (milliseconds) = 0
 																										Left result set:
 																											Union ResultSet:
 																											Number of opens = 5
 																											Rows seen from the left = 29
 																											Rows seen from the right = 3
 																											Rows returned = 32
 																												constructor time (milliseconds) = 0
 																												open time (milliseconds) = 0
 																												next time (milliseconds) = 0
 																												close time (milliseconds) = 0
 																											Left result set:
 																												Union ResultSet:
 																												Number of opens = 5
 																												Rows seen from the left = 26
 																												Rows seen from the right = 3
 																												Rows returned = 29
 																													constructor time (milliseconds) = 0
 																													open time (milliseconds) = 0
 																													next time (milliseconds) = 0
 																													close time (milliseconds) = 0
 																												Left result set:
 																													Union ResultSet:
 																													Number of opens = 5
 																													Rows seen from the left = 23
 																													Rows seen from the right = 3
 																													Rows returned = 26
 																														constructor time (milliseconds) = 0
 																														open time (milliseconds) = 0
 																														next time (milliseconds) = 0
 																														close time (milliseconds) = 0
 																													Left result set:
 																														Union ResultSet:
 																														Number of opens = 5
 																														Rows seen from the left = 20
 																														Rows seen from the right = 3
 																														Rows returned = 23
 																															constructor time (milliseconds) = 0
 																															open time (milliseconds) = 0
 																															next time (milliseconds) = 0
 																															close time (milliseconds) = 0
 																														Left result set:
 																															Union ResultSet:
 																															Number of opens = 5
 																															Rows seen from the left = 17
 																															Rows seen from the right = 3
 																															Rows returned = 20
 																																constructor time (milliseconds) = 0
 																																open time (milliseconds) = 0
 																																next time (milliseconds) = 0
 																																close time (milliseconds) = 0
 																															Left result set:
 																																Union ResultSet:
 																																Number of opens = 5
 																																Rows seen from the left = 14
 																																Rows seen from the right = 3
 																																Rows returned = 17
 																																	constructor time (milliseconds) = 0
 																																	open time (milliseconds) = 0
 																																	next time (milliseconds) = 0
 																																	close time (milliseconds) = 0
 																																Left result set:
 																																	Union ResultSet:
 																																	Number of opens = 5
 																																	Rows seen from the left = 11
 																																	Rows seen from the right = 3
 																																	Rows returned = 14
 																																		constructor time (milliseconds) = 0
 																																		open time (milliseconds) = 0
 																																		next time (milliseconds) = 0
 																																		close time (milliseconds) = 0
 																																	Left result set:
 																																		Union ResultSet:
 																																		Number of opens = 5
 																																		Rows seen from the left = 8
 																																		Rows seen from the right = 3
 																																		Rows returned = 11
 																																			constructor time (milliseconds) = 0
 																																			open time (milliseconds) = 0
 																																			next time (milliseconds) = 0
 																																			close time (milliseconds) = 0
 																																		Left result set:
 																																			Union ResultSet:
 																																			Number of opens = 5
 																																			Rows seen from the left = 5
 																																			Rows seen from the right = 3
 																																			Rows returned = 8
 																																				constructor time (milliseconds) = 0
 																																				open time (milliseconds) = 0
 																																				next time (milliseconds) = 0
 																																				close time (milliseconds) = 0
 																																			Left result set:
 																																				Row ResultSet:
 																																				Number of opens = 5
 																																				Rows returned = 5
 																																					constructor time (milliseconds) = 0
 																																					open time (milliseconds) = 0
 																																					next time (milliseconds) = 0
 																																					close time (milliseconds) = 0
 																																			Right result set:
 																																				Row ResultSet:
 																																				Number of opens = 3
 																																				Rows returned = 3
 																																					constructor time (milliseconds) = 0
 																																					open time (milliseconds) = 0
 																																					next time (milliseconds) = 0
 																																					close time (milliseconds) = 0
 																																		Right result set:
 																																			Row ResultSet:
 																																			Number of opens = 3
 																																			Rows returned = 3
 																																				constructor time (milliseconds) = 0
 																																				open time (milliseconds) = 0
 																																				next time (milliseconds) = 0
 																																				close time (milliseconds) = 0
 																																	Right result set:
 																																		Row ResultSet:
 																																		Number of opens = 3
 																																		Rows returned = 3
 																																			constructor time (milliseconds) = 0
 																																			open time (milliseconds) = 0
 																																			next time (milliseconds) = 0
 																																			close time (milliseconds) = 0
 																																Right result set:
 																																	Row ResultSet:
 																																	Number of opens = 3
 																																	Rows returned = 3
 																																		constructor time (milliseconds) = 0
 																																		open time (milliseconds) = 0
 																																		next time (milliseconds) = 0
 																																		close time (milliseconds) = 0
 																															Right result set:
 																																Row ResultSet:
 																																Number of opens = 3
 																																Rows returned = 3
 																																	constructor time (milliseconds) = 0
 																																	open time (milliseconds) = 0
 																																	next time (milliseconds) = 0
 																																	close time (milliseconds) = 0
 																														Right result set:
 																															Row ResultSet:
 																															Number of opens = 3
 																															Rows returned = 3
 																																constructor time (milliseconds) = 0
 																																open time (milliseconds) = 0
 																																next time (milliseconds) = 0
 																																close time (milliseconds) = 0
 																													Right result set:
 																														Row ResultSet:
 																														Number of opens = 3
 																														Rows returned = 3
 																															constructor time (milliseconds) = 0
 																															open time (milliseconds) = 0
 																															next time (milliseconds) = 0
 																															close time (milliseconds) = 0
 																												Right result set:
 																													Row ResultSet:
 																													Number of opens = 3
 																													Rows returned = 3
 																														constructor time (milliseconds) = 0
 																														open time (milliseconds) = 0
 																														next time (milliseconds) = 0
 																														close time (milliseconds) = 0
 																											Right result set:
 																												Row ResultSet:
 																												Number of opens = 3
 																												Rows returned = 3
 																													constructor time (milliseconds) = 0
 																													open time (milliseconds) = 0
 																													next time (milliseconds) = 0
 																													close time (milliseconds) = 0
 																										Right result set:
 																											Row ResultSet:
 																											Number of opens = 3
 																											Rows returned = 3
 																												constructor time (milliseconds) = 0
 																												open time (milliseconds) = 0
 																												next time (milliseconds) = 0
 																												close time (milliseconds) = 0
 																									Right result set:
 																										Row ResultSet:
 																										Number of opens = 3
 																										Rows returned = 3
 																											constructor time (milliseconds) = 0
 																											open time (milliseconds) = 0
 																											next time (milliseconds) = 0
 																											close time (milliseconds) = 0
 																								Right result set:
 																									Row ResultSet:
 																									Number of opens = 3
 																									Rows returned = 3
 																										constructor time (milliseconds) = 0
 																										open time (milliseconds) = 0
 																										next time (milliseconds) = 0
 																										close time (milliseconds) = 0
 																							Right result set:
 																								Row ResultSet:
 																								Number of opens = 3
 																								Rows returned = 3
 																									constructor time (milliseconds) = 0
 																									open time (milliseconds) = 0
 																									next time (milliseconds) = 0
 																									close time (milliseconds) = 0
 																						Right result set:
 																							Row ResultSet:
 																							Number of opens = 3
 																							Rows returned = 3
 																								constructor time (milliseconds) = 0
 																								open time (milliseconds) = 0
 																								next time (milliseconds) = 0
 																								close time (milliseconds) = 0
 																					Right result set:
 																						Row ResultSet:
 																						Number of opens = 3
 																						Rows returned = 3
 																							constructor time (milliseconds) = 0
 																							open time (milliseconds) = 0
 																							next time (milliseconds) = 0
 																							close time (milliseconds) = 0
 																				Right result set:
 																					Row ResultSet:
 																					Number of opens = 3
 																					Rows returned = 3
 																						constructor time (milliseconds) = 0
 																						open time (milliseconds) = 0
 																						next time (milliseconds) = 0
 																						close time (milliseconds) = 0
 																			Right result set:
 																				Row ResultSet:
 																				Number of opens = 3
 																				Rows returned = 3
 																					constructor time (milliseconds) = 0
 																					open time (milliseconds) = 0
 																					next time (milliseconds) = 0
 																					close time (milliseconds) = 0
 																		Right result set:
 																			Row ResultSet:
 																			Number of opens = 3
 																			Rows returned = 3
 																				constructor time (milliseconds) = 0
 																				open time (milliseconds) = 0
 																				next time (milliseconds) = 0
 																				close time (milliseconds) = 0
 																	Right result set:
 																		Row ResultSet:
 																		Number of opens = 3
 																		Rows returned = 3
 																			constructor time (milliseconds) = 0
 																			open time (milliseconds) = 0
 																			next time (milliseconds) = 0
 																			close time (milliseconds) = 0
 																Right result set:
 																	Row ResultSet:
 																	Number of opens = 3
 																	Rows returned = 3
 																		constructor time (milliseconds) = 0
 																		open time (milliseconds) = 0
 																		next time (milliseconds) = 0
 																		close time (milliseconds) = 0
 															Right result set:
 																Row ResultSet:
 																Number of opens = 3
 																Rows returned = 3
 																	constructor time (milliseconds) = 0
 																	open time (milliseconds) = 0
 																	next time (milliseconds) = 0
 																	close time (milliseconds) = 0
 														Right result set:
 															Row ResultSet:
 															Number of opens = 3
 															Rows returned = 3
 																constructor time (milliseconds) = 0
 																open time (milliseconds) = 0
 																next time (milliseconds) = 0
 																close time (milliseconds) = 0
 													Right result set:
 														Row ResultSet:
														Number of opens = 3
														Rows returned = 3
															constructor time (milliseconds) = 0
															open time (milliseconds) = 0
															next time (milliseconds) = 0
															close time (milliseconds) = 0
												Right result set:
													Row ResultSet:
													Number of opens = 3
													Rows returned = 3
														constructor time (milliseconds) = 0
														open time (milliseconds) = 0
														next time (milliseconds) = 0
														close time (milliseconds) = 0
											Right result set:
												Row ResultSet:
												Number of opens = 3
												Rows returned = 3
													constructor time (milliseconds) = 0
													open time (milliseconds) = 0
													next time (milliseconds) = 0
													close time (milliseconds) = 0
										Right result set:
											Row ResultSet:
											Number of opens = 3
											Rows returned = 3
												constructor time (milli
 ij> drop table colls;
 0 rows inserted/updated/deleted
 ij> drop table docs;
 0 rows inserted/updated/deleted
 ij> drop table t1;
 0 rows inserted/updated/deleted
 ij> drop table t2;
 0 rows inserted/updated/deleted
 ij> drop table t3;
 0 rows inserted/updated/deleted
 ij> drop table t4;
 0 rows inserted/updated/deleted
 ij> -- Test case for DERBY-558: optimizer hangs in rare cases where
 -- multiple subqueries flattened to EXISTS put multiple restrictions
 -- on legal join orders.
 create table digits (d int);
 0 rows inserted/updated/deleted
 ij> insert into digits values 1, 2, 3, 4, 5, 6, 7, 8, 9, 0;
 10 rows inserted/updated/deleted
 ij> create table odd (o int);
 0 rows inserted/updated/deleted
 ij> insert into odd values 1, 3, 5, 7, 9;
 5 rows inserted/updated/deleted
 ij> commit;
 ij> -- In order to test this, "noTimeout" must be true so that
 -- the optimizer will run through all of the possible join
 -- orders before it quits.  In the case of DERBY-558 the
 -- optimizer was getting stuck in a logic loop and thus never
 -- quit, causing the hang.  NOTE: The "noTimeout" property
 -- is set in the subqueryFlattening_derby.properties file.
 select distinct temp_t0.d from 
 	(select d from digits where d > 3) temp_t0,
 	(select o from odd) temp_t1,
 	odd temp_t4,
 	(select o from odd) temp_t3
 	where temp_t0.d = temp_t1.o
 		and temp_t0.d = temp_t3.o
 		and temp_t0.d in (select o from odd where o = temp_t1.o)
  		and exists (
 			select d from digits
 				where d = temp_t0.d)
 -- Before fix for DERBY-558, we would HANG (loop indefinitely) here;
 -- after fix, we should see three rows returned.
 ;
 D          
 -----------
 5          
 7          
 9          
 ij> -- clean-up.
 drop table digits;
 0 rows inserted/updated/deleted
 ij> drop table odd;
 0 rows inserted/updated/deleted
 ij> -- regression test from old Cloudscape bug 4736 which demonstrates that
 -- the fix for DERBY-3097 is correct. This query used to cause a NPE in
 -- BaseActivation.getColumnFromRow, but the optimizer data structures are
 -- now generated correctly and the NPE no longer occurs.
 create table a (a1 int not null primary key, a2 int, a3 int, a4
 int, a5 int, a6 int);
 0 rows inserted/updated/deleted
 ij> create table b (b1 int not null primary key, b2 int, b3 int, b4
 int, b5 int, b6 int);
 0 rows inserted/updated/deleted
 ij> create table c (c1 int not null, c2 int, c3 int not null, c4
 int, c5 int, c6 int);
 0 rows inserted/updated/deleted
 ij> create table d (d1 int not null, d2 int, d3 int not null, d4
 int, d5 int, d6 int);
 0 rows inserted/updated/deleted
 ij> alter table c add primary key (c1,c3);
 0 rows inserted/updated/deleted
 ij> alter table d add primary key (d1,d3);
 0 rows inserted/updated/deleted
 ij> insert into a values
 (1,1,3,6,NULL,2),(2,3,2,4,2,2),(3,4,2,NULL,NULL,NULL),
 (4,NULL,4,2,5,2),(5,2,3,5,7,4),(7,1,4,2,3,4),
                      (8,8,8,8,8,8),(6,7,3,2,3,4);
 8 rows inserted/updated/deleted
 ij> insert into b values
 (6,7,2,3,NULL,1),(4,5,9,6,3,2),(1,4,2,NULL,NULL,NULL),
 (5,NULL,2,2,5,2),(3,2,3,3,1,4),(7,3,3,3,3,3),(9,3,3,3,3,3);
 7 rows inserted/updated/deleted
 ij> insert into c values
 (3,7,7,3,NULL,1),(8,3,9,1,3,2),(1,4,1,NULL,NULL,NULL),
 (3,NULL,1,2,4,2),(2,2,5,3,2,4),(1,7,2,3,1,1),(3,8,4,2,4,6);
 7 rows inserted/updated/deleted
 ij> insert into d values
 (1,7,2,3,NULL,3),(2,3,9,1,1,2),(2,2,2,NULL,3,2),
 (1,NULL,3,2,2,1),(2,2,5,3,2,3),(2,5,6,3,7,2);
 6 rows inserted/updated/deleted
 ij> select a1,b1,c1,c3,d1,d3
   from D join (A left outer join (B join C on b2=c2) on a1=b1)
 on d3=b3 and d1=a2;
 A1         |B1         |C1         |C3         |D1         |D3         
 -----------------------------------------------------------------------
 1          |1          |1          |1          |1          |2          
 7          |7          |8          |9          |1          |3          
 ij> drop table a;
 0 rows inserted/updated/deleted
 ij> drop table b;
 0 rows inserted/updated/deleted
 ij> drop table c;
 0 rows inserted/updated/deleted
 ij> drop table d;
 0 rows inserted/updated/deleted
 ij> -- DERBY-3288: Optimizer does not correctly enforce EXISTS join order
 -- dependencies in the face of "short-circuited" plans.  In this test
 -- an EXISTS subquery is flattened into the outer query and thus has
 -- has a dependency on another FromTable (esp. HalfOuterJoinNode).
 -- The tables are such that the optimizer will attempt to short-
 -- circuit some relatively bad plans (namely, any join order that
 -- starts with { TAB_V, HOJ, ...}), and needs to correctly enforce
 -- join order dependencies in the process.
 CREATE TABLE tab_a (PId BIGINT NOT NULL);
 0 rows inserted/updated/deleted
 ij> CREATE TABLE tab_c (Id BIGINT NOT NULL PRIMARY KEY,
   PAId BIGINT NOT NULL, PBId BIGINT NOT NULL);
 0 rows inserted/updated/deleted
 ij> INSERT INTO tab_c VALUES (91, 81, 82);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_c VALUES (92, 81, 84);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_c VALUES (93, 81, 88);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_c VALUES (96, 81, 83);
 1 row inserted/updated/deleted
 ij> CREATE TABLE tab_v (OId BIGINT NOT NULL,
   UGId BIGINT NOT NULL, val CHAR(1) NOT NULL);
 0 rows inserted/updated/deleted
 ij> CREATE UNIQUE INDEX tab_v_i1 ON tab_v (OId, UGId, val);
 0 rows inserted/updated/deleted
 ij> CREATE INDEX tab_v_i2 ON tab_v (UGId, val, OId);
 0 rows inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (81, 31, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (82, 31, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (83, 31, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (84, 31, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (85, 31, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (86, 31, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (87, 31, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (81, 32, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (82, 32, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (83, 32, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (84, 32, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (85, 32, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (86, 32, 'A');
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_v VALUES (87, 32, 'A');
 1 row inserted/updated/deleted
 ij> CREATE TABLE tab_b (Id BIGINT NOT NULL PRIMARY KEY, OId BIGINT NOT NULL);
 0 rows inserted/updated/deleted
 ij> INSERT INTO tab_b VALUES (141, 81);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_b VALUES (142, 82);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_b VALUES (143, 84);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_b VALUES (144, 88);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_b VALUES (151, 81);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_b VALUES (152, 83);
 1 row inserted/updated/deleted
 ij> CREATE TABLE tab_d (Id BIGINT NOT NULL PRIMARY KEY,
   PAId BIGINT NOT NULL, PBId BIGINT NOT NULL);
 0 rows inserted/updated/deleted
 ij> INSERT INTO tab_d VALUES (181, 141, 142);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_d VALUES (182, 141, 143);
 1 row inserted/updated/deleted
 ij> INSERT INTO tab_d VALUES (186, 151, 152);
 1 row inserted/updated/deleted
 ij> -- Query should return 2 rows; before DERBY-3288 was fixed, it would
 -- only return a single row due to violation of join order dependencies.
 SELECT tab_b.Id
 FROM tab_b JOIN tab_c ON (tab_b.OId = tab_c.PAId OR tab_b.OId = tab_c.PBId)
 LEFT OUTER JOIN tab_a ON tab_b.OId = PId
 WHERE EXISTS
   (SELECT 'X' FROM tab_d
     WHERE (PAId = 141 AND PBId = tab_b.Id)
         OR (PBId = 141 AND PAId = tab_b.Id))
   AND EXISTS
     (SELECT 'X' FROM tab_v
       WHERE OId = tab_b.OId AND UGId = 31 AND val = 'A');
 ID                  
 --------------------
 142                 
 143                 
 ij> drop table tab_d;
 0 rows inserted/updated/deleted
 ij> drop table tab_b;
 0 rows inserted/updated/deleted
 ij> drop table tab_v;
 0 rows inserted/updated/deleted
 ij> drop table tab_c;
 0 rows inserted/updated/deleted
 ij> 
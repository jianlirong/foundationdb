/*
 * DirectoryTest.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2013-2018 Apple Inc. and the FoundationDB project authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apple.cie.foundationdb.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.function.Function;

import com.apple.cie.foundationdb.Cluster;
import com.apple.cie.foundationdb.Database;
import com.apple.cie.foundationdb.FDB;
import com.apple.cie.foundationdb.Transaction;
import com.apple.cie.foundationdb.TransactionContext;
import com.apple.cie.foundationdb.directory.DirectoryLayer;
import com.apple.cie.foundationdb.directory.DirectorySubspace;

public class DirectoryTest {
	private static final String CLUSTER_FILE = "/home/ajb/fdb.cluster";

	public static void main(String[] args) throws Exception {
		try {
			Cluster c = FDB.selectAPIVersion(510).createCluster(CLUSTER_FILE);
			Database db = c.openDatabase();
			runTests(db);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	private static void runTests(TransactionContext db) throws Exception {
		System.out.println("Running tests...");
		final DirectoryLayer dir = new DirectoryLayer();

		try {
			db.run(new Function<Transaction, Void>() {
				@Override
				public Void apply(Transaction tr) {
					List<String> path = new ArrayList<>();
					path.add("foo");
					DirectorySubspace foo = dir.create(tr, path).join();//, "partition".getBytes("UTF-8")).get();
					System.out.println(foo.getPath());
					path.add("bar");
					DirectorySubspace bar = dir.create(tr, path).join();//, "partition".getBytes("UTF-8")).get();
					System.out.println(foo.getPath());
					path.add("baz");
					DirectorySubspace baz = dir.create(tr, path).join();
					System.out.println(foo.getPath());
					System.out.println("Created foo: " + foo.exists(tr).join());
					System.out.println("Created bar: " + bar.exists(tr).join());
					System.out.println("Created baz: " + baz.exists(tr).join());

					DirectorySubspace bat = baz.moveTo(tr, Arrays.asList("foo", "bar", "bat")).join();

					System.out.println("Moved baz to bat: " + bat.exists(tr).join());

					foo.removeIfExists(tr).join();

					System.out.println("Removed foo: " + foo.exists(tr).join());

					return null;
				}
			});
		} catch (Throwable e) {
			e.printStackTrace();
		}

		System.exit(0);
	}
}

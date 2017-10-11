/*
 * WatchTest.java
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

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.apple.cie.foundationdb.Database;
import com.apple.cie.foundationdb.FDB;
import com.apple.cie.foundationdb.FDBException;
import com.apple.cie.foundationdb.Transaction;

public class WatchTest {

	public static void main(String[] args) {
		FDB fdb = FDB.selectAPIVersion(510);
		Database database = fdb.open(args[0]);
		database.options().setLocationCacheSize(42);
		Transaction tr = database.createTransaction();
		byte[] bs = tr.get("a".getBytes()).join();
		System.out.println("`a' -> " + (bs == null ? "<null>" : new String(bs)));
		final CompletableFuture<Void> watch = tr.watch("a".getBytes());
		System.err.println("Watch started...");
		//System.exit(0);
		tr.commit().join();
		watch.cancel(true);
		try {
			watch.join();
			System.out.println("`a' changed");
		} catch(FDBException e) {
			System.out.println("`a' watch error -> " + e.getMessage());
			if(e.getCode() != 1101)
				throw e;
		}

		raceTest(database);
	}

	public static void raceTest(Database db) {
		ExecutorService e = Executors.newCachedThreadPool(); // Executors.newFixedThreadPool(2);
		Random r = new Random();

		Transaction tr = db.createTransaction();
		byte[] key = "hello".getBytes();

		for(int i = 0; i < 10000; i++) {
			final CompletableFuture<Void> f = tr.watch(key);
			final AtomicInteger a = new AtomicInteger();
			Runnable cancel = new Runnable() {
				@Override
				public void run() {
					System.err.println("`f' cancel()...");
					f.cancel(true);
					a.incrementAndGet();
				}
			};
			Runnable get = new Runnable() {
				@Override
				public void run() {
					try {
						System.err.println("`f' get()...");
						f.join();
						System.err.println("`f' changed");
					} catch(FDBException e) {
						System.err.println("`f' watch error -> " + e.getMessage());
						if(e.getCode() != 1101)
							throw e;
					} finally {
						a.incrementAndGet();
					}
				}
			};
			if(r.nextBoolean()) {
				e.execute(cancel);
				e.execute(get);
			} else {
				e.execute(get);
				e.execute(cancel);
			}

			while(a.get() != 2); {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			//if(i % 1000 == 0) {
				System.out.println("Done with " + i);
			//}
		}
	}
}

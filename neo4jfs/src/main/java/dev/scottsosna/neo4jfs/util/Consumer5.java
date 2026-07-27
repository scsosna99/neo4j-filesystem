/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.util;

/**
 * Similar to Java's Consumer/BiConsumer but for 5 parameters.
 */
@FunctionalInterface
public interface Consumer5<T1,T2,T3,T4,T5> {
    void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
}

/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.util;

import java.io.IOException;

/**
 * Similar to Java's Consumer/BiConsumer but for 8 parameters AND IOExceptions may be thrown.
 */
@FunctionalInterface
public interface Consumer8<T1,T2,T3,T4,T5,T6,T7,T8> {
    void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) throws IOException;
}

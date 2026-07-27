/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.exception;

import java.net.URI;

/**
 * Neo4Jfs exception signalling unknown entry type.
 */
public class Neo4jfsUnknownEntryException extends Neo4jfsException {
    public Neo4jfsUnknownEntryException() {
        super();
    }

    /**
     * Constructor for composing exception message.
     * @param uri Neo4Jfs URI for entry
     * @param entryType unknown/unexpected/unsupported entry type
     */
    public Neo4jfsUnknownEntryException(URI uri, String entryType) {
        super(String.format("Unknown entry type '%s' for URI '%s'", entryType, uri));
    }

    public Neo4jfsUnknownEntryException(String message) {
        super(message);
    }

    public Neo4jfsUnknownEntryException(String message, Throwable cause) {
        super(message, cause);
    }

    public Neo4jfsUnknownEntryException(Throwable cause) {
        super(cause);
    }
}

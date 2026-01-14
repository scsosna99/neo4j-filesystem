This web application is intended to be a demo front-end for Neo4Jfs and **IS NOT** complete nor secure.  Use at your own risk.

Endpoints all require basic authentication, but any credentials work: no checking an IDP, no password validation, etc.  However, for internal security purposes, users may be mapped to a "role" or "user group" in `applications.properties`.  This helps for Posix group-based access and security checking.

The user `root` is an super-user and has all privileges.  Other users by default are read-only.  Permissions are Posix-based.  By default, the root `\` directory has `rwxrw----`.

One example work flow to create user-specific directories might look like this: 
* As root, create a directory `/alice` with the root directory.
* As root, Change the owner of `/alice` to `alice`.
* As Alice, create a directory `/alice/work` and upload Alice's work files.
* As Bob, attempt to access files `/alices/work`: when things work, Bob should not be able to see files.

The web application can be used as a basis for creating your own front-end service leveraging Neo4Jfs or this demo app could be extended over time.

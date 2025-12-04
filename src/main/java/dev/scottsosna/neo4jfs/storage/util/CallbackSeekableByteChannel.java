package dev.scottsosna.neo4jfs.storage.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies a SeekableByteChanngel to allow post-close actions.
 */
public class CallbackSeekableByteChannel implements SeekableByteChannel {

    /**
     * SeekableByteChannel being proxied.
     */
    private final SeekableByteChannel delegate;

    /**
     * List of callbacks to be executed after channel closed.
     */
    private final List<Runnable> callbacks = new ArrayList<>();

    /**
     * Constructor
     * @param delegate SeekableByteChannel to be proxied.
     */
    public CallbackSeekableByteChannel(SeekableByteChannel delegate) {
        this.delegate = delegate;
    }
    /**
     * Add callback to be executed upon close.
     * @param callback Runnable to be executed.
     */
    public void registerCallback(Runnable callback) {
        callbacks.add(callback);
    }

    /**
     * CLose output stream and execute registtered callbacks.
     * @throws IOException thrown when proxied stream cannot be closed.
     */
    @Override
    public void close() throws IOException {
        delegate.close();

        //  Execute all callbacks registered to this stream.
        callbacks.forEach(Runnable::run);
    }

    /* ---------------------------------------------------------------------------
       Remaining methods are proxied with no side effects.
       --------------------------------------------------------------------------- */

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return delegate.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return delegate.position(newPosition);
    }

    @Override
    public long size() throws IOException {
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return delegate.truncate(size);
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }
}

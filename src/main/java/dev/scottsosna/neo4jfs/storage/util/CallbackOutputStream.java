package dev.scottsosna.neo4jfs.storage.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies an output stream to allow post-close actions.
 */
public class CallbackOutputStream extends OutputStream {

    //  Output stream being proxied.
    private final OutputStream delegate;

    //  Callbacks to be executed upon close.
    private final List<Runnable> callbacks = new ArrayList<>();

    /**
     * Constructor
     * @param os OutputStream to be proxied.
     */
    public CallbackOutputStream(OutputStream os) {
        this.delegate = os;
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
    ------------------------------------------------------------------------------ */

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }
}

/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Licensed under the MIT license for non-commercial use.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 *
 * Licensed under the GPLv3 license for commercial use.  Please refer to LICENSE-GPL.md or
 * https://www.gnu.org/licenses/gpl-3.0.html for terms and conditions.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * expressed or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.scottsosna.neo4jfs.service.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Output stream proxy so interested parties can register callbacks to be executed upon close.
 */
public class CallbackOutputStream extends OutputStream {

    /**
     * OutputStream being proxied.
     */
    private final OutputStream delegate;

    /**
     * List of callbacks to be executed ad\fter stream closed.
     */
    private final List<Runnable> callbacks = new ArrayList<>();

    /**
     * Constructor
     * @param os OutputStream to be proxied.
     */
    public CallbackOutputStream(final OutputStream os) {
        this.delegate = os;
    }

    /**
     * Add callback to be executed upon close.
     * @param callback Runnable to be executed.
     */
    public void registerCallback(final Runnable callback) {
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

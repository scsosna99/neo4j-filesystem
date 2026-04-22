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
package dev.scottsosna.neo4jfs.storage.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Random;

/**
 * Do-nothing SeekableByteChannel that acts like there's a backing file but in fact does nothing usage with physical
 * files.  Used in conjunction with DummyStorageManager as a development aid.
 */
public class NullSeekableByteChannel implements SeekableByteChannel {

    private boolean closed = false;
    private long position = 0;
    private final Random random = new Random();

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int size = dst.remaining();
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        dst.put(bytes);
        position += size;
        return size;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        position += src.remaining();
        return src.remaining();
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return this;
    }

    @Override
    public long size() throws IOException {
        return 0;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return null;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}

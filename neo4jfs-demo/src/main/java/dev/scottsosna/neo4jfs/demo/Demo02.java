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
package dev.scottsosna.neo4jfs.demo;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.Map;

/**
 * Demo02: Create a new Neo4Jfs file system, create directories, load files.  Again, nothing too fancy.
 */
@Service("demo02")
public class Demo02 extends Demo {

    @Override
    public void demo() {
        streams();
        channels();
    }

    /**
     * Use Input/Output streams to create new Neo4Jfs file and then save it to local disk.
     */
    private static void streams() {
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            //  Stream web page and write it to Neo4Jfs.
            URLConnection connection = new URL("https://github.com").openConnection();
            Files.copy(connection.getInputStream(), fs.getPath("/text/github.html"));

            //  Copy Neo4Jfs file to local file
            Path downloads = Path.of("./data/downloads");
            if (!Files.exists(downloads)) {
                Files.createDirectory(downloads);
            }
            try (InputStream is = Files.newInputStream(fs.getPath("/text/github.html"));
                OutputStream os = Files.newOutputStream(Path.of("./data/downloads/github.html"))) {
                is.transferTo(os);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read, update, re-read a file via SeekableByteChannel
     */
    private static void channels() {
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            //  Read file via channel.
            try (SeekableByteChannel channel = Files.newByteChannel(fs.getPath("/text/github.html"), StandardOpenOption.READ)) {
                ByteBuffer buffer = ByteBuffer.allocate(256);
                int read = channel.read(buffer);
                System.out.println("ORIGINAL: " + new String(buffer.array()));
            }

            //  Open the file to write garbage..
            System.out.println ("Open file to write garbage");
            try (SeekableByteChannel channel = Files.newByteChannel(fs.getPath("/text/github.html"), StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.allocate(128);
                buffer.clear();
                buffer.put(RandomStringUtils.randomAscii(128).getBytes());
                buffer.flip();
                int write = channel.position(64).write(buffer);
                System.out.println("WRITTEN: " + new String(buffer.array()));
            }

            //  Read the updated file and delete upon close.
            try (SeekableByteChannel channel = Files.newByteChannel(fs.getPath("/text/github.html"), StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE)) {
                ByteBuffer buffer = ByteBuffer.allocate(256);
                int read = channel.read(buffer);
                System.out.println("UPDATED: " + new String(buffer.array()));
            }

            //  File should no longer exist
            System.out.println("Does file exist? " + Files.exists(fs.getPath("/text/github.html")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

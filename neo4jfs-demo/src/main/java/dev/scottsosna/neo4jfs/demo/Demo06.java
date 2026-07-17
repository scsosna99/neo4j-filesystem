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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Demo06: copies user's home into Neo4Jfs, useful for perf testing Neo4Jfs side of things.
 * NOTE: Highly recommended that you run this with the dummy storage manager which doesn't actually store anything; otherwise
 * you'll be making a complete copy of all your files and likely filling up your disk.  Be Careful!
 */
@Slf4j
@Service("demo06")
public class Demo06 extends Demo  {

    @Override
    public void demo() {
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            Path data = fs.getPath("/data");
            if (!Files.exists(data)) {
                Files.createDirectory(data);
            }
            FileSystemUtils.copyRecursively(Path.of(System.getProperty("user.home")), data);
        } catch (IOException e) {
            System.out.println("Something bad happened: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

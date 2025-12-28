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

import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Demo01: Create a new Neo4Jfs file system, create directories, load files.  Nothing too fancy.
 */
@Service("demo01")
public class Demo01 implements Demo  {

    @Override
    public void demo() {
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            //  Create directories for different media types.
            Files.createDirectory(fs.getPath("/songs"));
            Files.createDirectory(fs.getPath("/videos"));
            Files.createDirectory(fs.getPath("/images"));
            Files.createDirectory(fs.getPath("/text"));
            Files.createDirectory(fs.getPath("/copies"));

            //  Upload images: note that cross-filesystem copies check for existence of target unless
            //  {@code StandardCopyOption.REPLACE_EXISTING} is specified.  However, the existence check does not
            //  know whether the target is a file or directory, therefore must explicitly state target name.
            Files.copy(Path.of("data/apples1.jpg"), fs.getPath("/images/apples1.jpg"));
            Files.copy(Path.of("data/apples2.jpg"), fs.getPath("/images/apples2.jpg"));
            Files.copy(Path.of("data/apples3.jpg"), fs.getPath("/images/apples3.jpg"));
            Files.copy(Path.of("data/apples4.jpg"), fs.getPath("/images/apples4.jpg"));

            //  Make copies of original images in the "copies" directory"  Within a file system, the move
            //  can happen into a directory correctly.
            Files.copy(fs.getPath("images/apples1.jpg"), fs.getPath("copies"));
            Files.copy(fs.getPath("images/apples2.jpg"), fs.getPath("copies"));
            Files.copy(fs.getPath("images/apples3.jpg"), fs.getPath("copies"));
            Files.copy(fs.getPath("images/apples4.jpg"), fs.getPath("copies"));

            //  Move the original directories into a "data" directory.
            Files.createDirectory(fs.getPath("/data"));
            Files.move(fs.getPath("/songs"), fs.getPath("/data"));
            Files.move(fs.getPath("/videos"), fs.getPath("/data"));
            Files.move(fs.getPath("/images"), fs.getPath("/data"));
            Files.move(fs.getPath("/copies"), fs.getPath("/data"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

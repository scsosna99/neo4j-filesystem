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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

/**
 * Demo03: Posix permissions demo where permissions are inherited where they aren't defined on directory/file/.
 */
@Service("demo03")
public class Demo03 implements Demo  {

    @Override
    public void demo() {

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            System.out.println ("'/' permissions: " + Files.getPosixFilePermissions(fs.getPath("/")));
            System.out.println ("'/data/images' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images")));
            System.out.println ("'/data/images/apples1.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples1.jpg")));
            System.out.println ("'/data/images/apples2.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples2.jpg")));
            System.out.println ("'/data/images/apples3.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples3.jpg")));
            System.out.println ("'/data/images/apples4.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples4.jpg")));

            System.out.println("Changing '/data/images' permissions to rwx---rwx");
            Files.setPosixFilePermissions(fs.getPath("/data/images"), Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)
            );

            System.out.println("Changing '/data/images/apples3.jpg' permissions to ---rwxrwx");
            Files.setPosixFilePermissions(fs.getPath("/data/images/apples3.jpg"), Set.of(
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)
            );

            System.out.println ("'/' permissions: " + Files.getPosixFilePermissions(fs.getPath("/")));
            System.out.println ("'/data/images' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images")));
            System.out.println ("'/data/images/apples1.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples1.jpg")));
            System.out.println ("'/data/images/apples2.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples2.jpg")));
            System.out.println ("'/data/images/apples3.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples3.jpg")));
            System.out.println ("'/data/images/apples4.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples4.jpg")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

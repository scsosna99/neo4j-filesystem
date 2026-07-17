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

import dev.scottsosna.neo4jfs.filesystem.attribute.GroupPrincipalImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.UserPrincipalImpl;
import dev.scottsosna.neo4jfs.filesystem.option.Neo4jfsDeleteOption;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;

@Slf4j
@Service("demo07")
public class Demo07 extends Demo  {

    DirectoryService service;

    public Demo07 (DirectoryService directoryService) {
        this.service = directoryService;
    }

    @Override
    public void demo() {

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of("read-only", false))) {
            createHome(fs);
            setupAlice(fs);
            workByBob(fs);
//            copyByCarol(fs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createHome(final FileSystem fs) throws IOException {
        try {
            //  Permissions for each home directory.
            EnumSet<PosixFilePermission> perms = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.OTHERS_READ
            );

            //  Base directory under which each user's home directory is created.
            Path home = fs.getPath("home");
            if (!Files.exists(home)) {
                Files.createDirectory(home);
                Files.setPosixFilePermissions(home, perms);

            }

            //  Iterate through users to create and configure their home directories.
            String[] users = {"alice", "bob", "carol"};
            for (String user : users) {
                //  Create home directory.
                Path path = fs.getPath("/home/%s".formatted(user));
                if (!Files.exists(path)) {
                    Files.createDirectory(path);
                }

                //  Set the owner to the user
                Files.setOwner(path, new UserPrincipalImpl(user));
                Files.setPosixFilePermissions(path, perms);

                //  Set group to the user
                Files.getFileAttributeView(path, PosixFileAttributeView.class).setGroup(new GroupPrincipalImpl(user));
            }
        } catch (IOException e) {
            System.out.println("Error creating home directories: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void setupAlice(final FileSystem fs) throws IOException {
        //  Current user is Alice.
        setSecurityContext("alice", "alice");

        try {
            //  Create shared directory and set permissions so anyone can read/write
            Path path = fs.getPath("/home/alice/shared");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            Files.setPosixFilePermissions(path, EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE
            ));

            path = fs.getPath("/home/alice");
            service.delete(path.toUri(), Neo4jfsDeleteOption.DELETE_RECURSIVELY);
        } catch (IOException e) {
            System.out.println("Error setting up alice directory: " + e.getMessage());
            throw e;
        }
    }

    private void workByBob(final FileSystem fs) throws IOException {
        //  Current user is Bob.
        setSecurityContext("bob", "bob");

        //  Bob attempts to delete Alice's private file, exception should be thrown.
        try {
            //  Bob doesn't have access so expect exception thrown.
            Files.walk(fs.getPath("/home/alice/shared")).forEach(System.out::println);
        } catch (IOException e) {
            System.out.println("Bob unable to access Alice private file, expected." + e.getMessage());
        }
    }

    private void copyByCarol(final FileSystem fs) throws IOException {
        //  Current user is Carol.
        setSecurityContext("carol", "carol");

        //  Carol wants her own copy of shared files.
        try {
            Files.copy(fs.getPath("/home/alice/shared/apples5.jpg"), fs.getPath("/home/carol"));
            Files.copy(fs.getPath("/home/bob/shared_apples.jpg"), fs.getPath("/home/carol"));
        } catch (IOException e) {
            System.out.println("Carol unable to copy files: " + e.getMessage());
            throw e;
        }
    }
}

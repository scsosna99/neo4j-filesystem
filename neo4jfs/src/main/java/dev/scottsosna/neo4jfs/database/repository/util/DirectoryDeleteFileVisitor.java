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
package dev.scottsosna.neo4jfs.database.repository.util;

import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.service.FileService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@Component
public class DirectoryDeleteFileVisitor extends SimpleFileVisitor<Neo4jfsTreeWalker.NeofjfsWalkerEvent> {

    private DirectoryService directoryService;
    private FileService fileService;
    static public final String VISITOR_KEY = "directoryDelete";

    public DirectoryDeleteFileVisitor(DirectoryService directoryService,
                                      FileService fileService) {
        this.directoryService = directoryService;
        this.fileService = fileService;
    }

    /**
     * Delete every file found in the directory
     * @param file file being visited
     * @param attrs file attributes (ignored)
     * @return when successful, FileVisitResult.CONTINUE
     * @throws IOException thrown when file cannot be deleted
     */
    @Override
    public FileVisitResult visitFile(Neo4jfsTreeWalker.NeofjfsWalkerEvent file, BasicFileAttributes attrs)
        throws IOException
    {
        fileService.delete(file.uri());
        return FileVisitResult.CONTINUE;
    }

    /**
     * Deletes the directory itself which must occur after all contents (files and subdirectories) have been deleted.
     * @param dir directory being visited
     * @param e when present, exception that prevented a file or subdirectory from being deleted.
     * @return when successful, FileVisitResult.CONTINUE
     * @throws IOException thrown when directory cannot be deleted
     */
    @Override
    public FileVisitResult postVisitDirectory(Neo4jfsTreeWalker.NeofjfsWalkerEvent dir, IOException e)
        throws IOException
    {
        if (e == null) {
            directoryService.delete(dir.uri());
            return FileVisitResult.CONTINUE;
        } else {
            // directory iteration failed
            throw e;
        }
    }

    @PostConstruct
    private void init() {
        directoryService.registerVisitor(VISITOR_KEY, this);
    }
}

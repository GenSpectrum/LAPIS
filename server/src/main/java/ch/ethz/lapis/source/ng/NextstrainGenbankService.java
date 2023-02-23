package ch.ethz.lapis.source.ng;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Slf4j
public class NextstrainGenbankService {

    private final NextstrainDownloadService nextstrainDownloadService = new NextstrainDownloadService();
    private final NextstrainFileToDatabaseService nextstrainFileToDatabaseService;
    private final Path workdir;

    public NextstrainGenbankService(
        ComboPooledDataSource databasePool,
        String workdir,
        int maxNumberWorkers,
        String nextalignPath
    ) {
        this.workdir = Path.of(workdir);
        nextstrainFileToDatabaseService = new NextstrainFileToDatabaseService(
            databasePool,
            this.workdir,
            maxNumberWorkers,
            Path.of(nextalignPath)
        );
    }

    public void updateData() throws IOException, SQLException, InterruptedException {
        LocalDateTime startTime = LocalDateTime.now();

        nextstrainDownloadService.downloadFilesFromNextstrain(workdir);
        nextstrainFileToDatabaseService.insertFilesIntoDatabase(startTime);
    }
}

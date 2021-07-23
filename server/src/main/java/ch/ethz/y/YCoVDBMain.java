package ch.ethz.y;

import ch.ethz.y.core.DatabaseService;
import ch.ethz.y.core.SubProgram;
import ch.ethz.y.source.NextstrainGenbankService;
import ch.ethz.y.transform.TransformService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.Collections;

@SpringBootApplication
public class YCoVDBMain extends SubProgram<YCoVDBConfig> {

    public static YCoVDBConfig globalConfig;

    public YCoVDBMain() {
        super("YCoVDB", YCoVDBConfig.class);
    }

    @Override
    public void run(String[] args, YCoVDBConfig config) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("TODO: write help page");
        }
        globalConfig = config;
        if ("--api".equals(args[0])) {
            String[] argsForSpring = Arrays.copyOfRange(args, 1, args.length);
            SpringApplication app = new SpringApplication(YCoVDBMain.class);
            app.setDefaultProperties(Collections.singletonMap("server.port", "2345"));
            app.run(argsForSpring);
            return;
        }
        if ("--update-nextstrain-genbank".equals(args[0])) {
            ComboPooledDataSource dbPool = DatabaseService.createDatabaseConnectionPool(config.getVineyard());
            NextstrainGenbankService nextstrainGenBankService = new NextstrainGenbankService(
                    dbPool, config.getWorkdir(), config.getMaxNumberWorkers(), config.getNextalignPath());
            nextstrainGenBankService.updateData();
            return;
        }
        if ("--update-main-tables".equals(args[0])) {
            ComboPooledDataSource dbPool = DatabaseService.createDatabaseConnectionPool(config.getVineyard());
            TransformService transformService = new TransformService(dbPool);
            transformService.mergeAndTransform();
            return;
        }
        if ("--update-all".equals(args[0])) {
            ComboPooledDataSource dbPool = DatabaseService.createDatabaseConnectionPool(config.getVineyard());
            NextstrainGenbankService nextstrainGenBankService = new NextstrainGenbankService(
                    dbPool, config.getWorkdir(), config.getMaxNumberWorkers(), config.getNextalignPath());
            nextstrainGenBankService.updateData();
            TransformService transformService = new TransformService(dbPool);
            transformService.mergeAndTransform();
            return;
        }
    }

}

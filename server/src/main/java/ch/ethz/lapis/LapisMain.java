package ch.ethz.lapis;

import ch.ethz.lapis.core.DatabaseService;
import ch.ethz.lapis.core.GlobalProxyManager;
import ch.ethz.lapis.core.SubProgram;
import ch.ethz.lapis.source.ng.NextstrainGenbankService;
import ch.ethz.lapis.transform.TransformService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.Collections;

@SpringBootApplication
public class LapisMain extends SubProgram<LapisConfig> {

    public static LapisConfig globalConfig;

    public LapisMain() {
        super("Lapis", LapisConfig.class);
    }

    @Override
    public void run(String[] args, LapisConfig config) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("TODO: write help page");
        }
        globalConfig = config;
        GlobalProxyManager.setProxyFromConfig(config.getHttpProxy());
        if ("--api".equals(args[0])) {
            String[] argsForSpring = Arrays.copyOfRange(args, 1, args.length);
            SpringApplication app = new SpringApplication(LapisMain.class);
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
            TransformService transformService = new TransformService(dbPool, config.getMaxNumberWorkers());
            transformService.mergeAndTransform();
            return;
        }
        if ("--update-all".equals(args[0])) {
            ComboPooledDataSource dbPool = DatabaseService.createDatabaseConnectionPool(config.getVineyard());
            NextstrainGenbankService nextstrainGenBankService = new NextstrainGenbankService(
                    dbPool, config.getWorkdir(), config.getMaxNumberWorkers(), config.getNextalignPath());
            nextstrainGenBankService.updateData();
            TransformService transformService = new TransformService(dbPool, config.getMaxNumberWorkers());
            transformService.mergeAndTransform();
            return;
        }
    }

}

package ch.ethz.lapis.source.ng;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NextstrainDownloadService {

    public void downloadFilesFromNextstrain(Path targetDir) throws IOException {
        String urlPrefix = "https://data.nextstrain.org/files/ncov/open/";
        List<String> files = new ArrayList<>() {{
            add("metadata.tsv.gz");
            add("sequences.fasta.xz");
            add("aligned.fasta.xz");
            add("nextclade.tsv.gz");
        }};

        for (String file : files) {
            URL url = new URL(urlPrefix + file);
            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
            try (FileOutputStream fileOutputStream = new FileOutputStream(targetDir.resolve(file).toFile())) {
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
        }
    }
}

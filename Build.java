import javax.tools.*;
import java.nio.file.*;
import java.util.*;
public class Build {
    public static void main(String[] a) throws Exception {
        Files.createDirectories(Path.of("out"));
        List<String> files = new ArrayList<>();
        try (var s = Files.list(Path.of("src/smartmail"))) { s.forEach(p -> files.add(p.toString())); }
        List<String> args = new ArrayList<>(List.of("-d", "out"));
        args.addAll(files);
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0]));
        System.exit(rc);
    }
}

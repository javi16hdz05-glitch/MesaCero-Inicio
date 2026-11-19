import com.sun.source.util.JavacTask;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Revisa PARSEO Java con JDK 17+ sin requerir dependencias Maven. NO valida tipos. */
public class CheckJavaSyntax {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Indica carpeta src/main/java");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Se requiere JDK, no solo JRE");
        try (Stream<Path> paths = Files.walk(Path.of(args[0]));
             StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null)) {
            List<java.io.File> files = paths.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile).toList();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            var units = manager.getJavaFileObjectsFromFiles(files);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-source", "17", "-proc:none"), null, units);
            task.parse();
            long errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                    .peek(d -> System.err.println(d.toString())).count();
            System.out.println("Archivos Java revisados: " + files.size() + "; errores sintácticos: " + errors);
            if (errors > 0) System.exit(1);
        }
    }
}

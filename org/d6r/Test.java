package org.d6r;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleTreeVisitor;
public class Test {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects = fileManager
            .getJavaFileObjects("test/Foo.java");
        JavacTask javac = (JavacTask) compiler.getTask(null, fileManager, null, null, null,
            fileObjects);
        Iterable<? extends CompilationUnitTree> trees = javac.parse();
        for (CompilationUnitTree tree : trees) {
            tree.accept(new CompilationUnitVisitor(), null);
        }
    }
    static class CompilationUnitVisitor extends SimpleTreeVisitor<Void, Void> {
        @Override
        public Void visitCompilationUnit(CompilationUnitTree cut, Void p) {
            System.out.println("Package name: " + cut.getPackageName());
            for (Tree t : cut.getTypeDecls()) {
                if (t instanceof ClassTree) {
                ClassTree ct = (ClassTree) t;
                    ct.accept(new ClassVisitor(), null);
                }
            }
            return super.visitCompilationUnit(cut, p);
        }
    }
    static class ClassVisitor extends SimpleTreeVisitor<Void, Void> {
        @Override
        public Void visitClass(ClassTree ct, Void p) {
            System.out.println("Class name: " + ct.getSimpleName());
            for (Tree t : ct.getMembers()) {
                MethodTree mt = (MethodTree) t;
                mt.accept(new MethodVisitor(), null);
            }
            return super.visitClass(ct, p);
        }
    }
    static class MethodVisitor extends SimpleTreeVisitor<Void, Void> {
        @Override
        public Void visitMethod(MethodTree mt, Void p) {
            System.out.println("Method name: " + mt.getName());
            for (StatementTree st : mt.getBody().getStatements()) {
                if (st instanceof ReturnTree) {
                ReturnTree rt = (ReturnTree) st;
                    rt.accept(new ReturnTreeVisitor(), null);
                }
            }
            return super.visitMethod(mt, p);
        }
    }
    static class ReturnTreeVisitor extends SimpleTreeVisitor<Void, Void> {
        @Override
        public Void visitReturn(ReturnTree rt, Void p) {
            System.out.println("Return statement: " + rt.getExpression());
            return super.visitReturn(rt, p);
        }
    }
}
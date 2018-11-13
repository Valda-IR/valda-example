package at.yawk.valda.example;

import at.yawk.valda.ir.Classpath;
import at.yawk.valda.ir.ExternalTypeMirror;
import at.yawk.valda.ir.FieldMirror;
import at.yawk.valda.ir.LocalClassMirror;
import at.yawk.valda.ir.MethodMirror;
import at.yawk.valda.ir.MethodReference;
import at.yawk.valda.ir.TriState;
import at.yawk.valda.ir.TypeMirror;
import at.yawk.valda.ir.Types;
import at.yawk.valda.ir.code.Const;
import at.yawk.valda.ir.code.Invoke;
import at.yawk.valda.ir.code.LoadStore;
import at.yawk.valda.ir.code.LocalVariable;
import at.yawk.valda.ir.dex.compiler.DexCompiler;
import at.yawk.valda.ir.dex.parser.DexParser;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.DexFile;
import org.objectweb.asm.Type;

/**
 * @author yawkat
 */
public class Main {
    public static void main(String[] args) throws Exception {
        DexFile dexFile = DexFileFactory.loadDexFile("input.dex", Opcodes.getDefault());

        DexParser dexParser = new DexParser();
        dexParser.add(dexFile);
        Classpath classpath = dexParser.parse();

        TypeMirror urlType = classpath.getTypeMirror(Type.getType(URL.class));

        MethodMirror openStream = urlType.method("openStream", Type.getMethodType(Type.getType(InputStream.class)),
                                                 TriState.FALSE);

        Iterable<MethodReference.Invoke> invokes =
                openStream.getReferences().listReferences(MethodReference.Invoke.class);
        for (MethodReference.Invoke invokeReference : invokes) {
            Invoke invoke = invokeReference.getInstruction();

            // prepare the methods we need
            Type stringBuilderType = Type.getType(StringBuilder.class);
            TypeMirror sbTypeMirror = classpath.getTypeMirror(stringBuilderType);
            ((ExternalTypeMirror) sbTypeMirror).setInterface(false);
            // StringBuilder.<init>()V
            MethodMirror sbConstructor = sbTypeMirror
                    .method("<init>", Type.getMethodType(Type.VOID_TYPE), TriState.FALSE);
            // StringBuilder.append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
            MethodMirror sbAppend = sbTypeMirror
                    .method("append", Type.getMethodType(stringBuilderType, Types.OBJECT), TriState.FALSE);
            // StringBuilder.toString()Ljava/lang/String;
            MethodMirror sbToString = sbTypeMirror
                    .method("toString", Type.getMethodType(Type.getType(String.class)), TriState.FALSE);

            // stringBuilder = new StringBuilder();
            LocalVariable stringBuilder = LocalVariable.reference();
            invoke.addBefore(Invoke.builder().newInstance().method(sbConstructor).returnValue(stringBuilder).build());

            // stringBuilder.append("openStream() called: ");
            LocalVariable stringConst = LocalVariable.reference();
            invoke.addBefore(Const.createString(stringConst, "openStream() called: "));
            invoke.addBefore(Invoke.builder().method(sbAppend).parameter(stringBuilder).parameter(stringConst).build());

            // stringBuilder.append(url);
            LocalVariable url = invoke.getParameters().get(0);
            invoke.addBefore(Invoke.builder().method(sbAppend).parameter(stringBuilder).parameter(url).build());

            // string = stringBuilder.toString();
            LocalVariable string = LocalVariable.reference();
            invoke.addBefore(Invoke.builder().method(sbToString).parameter(stringBuilder).returnValue(string).build());

            // systemOut = System.out;
            FieldMirror systemOutField = classpath.getTypeMirror(Type.getType(System.class))
                    .field("out", Type.getType(PrintStream.class), TriState.TRUE);
            LocalVariable systemOut = LocalVariable.reference();
            invoke.addBefore(LoadStore.load().field(systemOutField).value(systemOut).build());

            // systemOut.println(string);
            MethodMirror println = classpath.getTypeMirror(Type.getType(PrintStream.class))
                    .method("println", Type.getMethodType(Type.VOID_TYPE, Type.getType(String.class)), TriState.FALSE);
            invoke.addBefore(Invoke.builder().method(println).parameter(systemOut).parameter(string).build());
        }

        DexCompiler dexCompiler = new DexCompiler();
        DexFile outputFile = dexCompiler.compile(classpath);

        DexFileFactory.writeDexFile("output.dex", outputFile);
    }
}

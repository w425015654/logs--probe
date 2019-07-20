package com.travelsky.airline.trp.ops.telassa.probe;

import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * 代理类，注入 Telassa 探针埋点信息
 *
 * @see ClassFileTransformer
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html">The mechanism for instrumentation</a>
 * @since 0.9.0
 *
 */
public class TelassaTransformer implements ClassFileTransformer {
    private static final Logger logger = Logger.getLogger(TelassaTransformer.class);

    private static final byte[] EMPTY_BYTE_ARRAY = {};

    private final List<Transformlet> transformletList = new ArrayList<Transformlet>();

    @SuppressWarnings("unchecked")
    TelassaTransformer(Class<? extends Transformlet>... transformletClasses) throws Exception {
        for (Class<? extends Transformlet> transformletClass : transformletClasses) {
            final Transformlet transformlet = transformletClass.getConstructor().newInstance();
            transformletList.add(transformlet);

            logger.info("[TelassaTransformer] add Transformlet " + transformletClass + " success");
        }
    }

    @Override
    public final byte[] transform(final ClassLoader loader, final String classFile, final Class<?> classBeingRedefined,
                                  final ProtectionDomain protectionDomain, final byte[] classFileBuffer) {
        try {
            // Lambda has no class file, no need to transform, just return.
            if (classFile == null) {
                return EMPTY_BYTE_ARRAY;
            }

            // 代码注入
            final String className = toClassName(classFile);


            if ("com.openjaw.travelsky.utils.SetCharacterEncodingFilter".equalsIgnoreCase(className)) {
                System.out.println("SetCharacterEncodingFilterLoader: " + loader.getClass().getName());
            }


            if ("org.apache.log4j.Logger".equalsIgnoreCase(className)) {
                if (loader == null) {
                    System.out.println("log4j.Logger: null");
                } else if (loader.getClass() == null) {
                    System.out.println("log4j.Logger getClass: null.");
                } else {
                    System.out.println("log4j.Logger: " + loader.getClass().getName());
                }
            }

            for (Transformlet transformlet : transformletList) {
                if (transformlet.needTransform(className)) {
                    logger.info("Transforming class " + className);
                    final CtClass clazz = getCtClass(classFileBuffer, loader, className, classFile);
                    transformlet.doTransform(clazz);
                    return clazz.toBytecode();
                }
            }
        } catch (Throwable t) {
            String msg = "Fail to transform class " + classFile + ", cause: " + t.toString();
            logger.log(Level.SEVERE, msg, t);
            throw new IllegalStateException(msg, t);
        }

        return EMPTY_BYTE_ARRAY;
    }

    private static String toClassName(final String classFile) {
        return classFile.replace('/', '.');
    }

    private static CtClass getCtClass(final byte[] classFileBuffer, final ClassLoader classLoader, String className, String classFile) throws IOException, NotFoundException {
        final ClassPool classPool = new ClassPool(true);
        if (classLoader == null) {
            classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
        } else {
            classPool.appendClassPath(new LoaderClassPath(classLoader));
        }

        final CtClass clazz = classPool.makeClass(new ByteArrayInputStream(classFileBuffer), false);
        clazz.defrost();
        return clazz;
    }
}

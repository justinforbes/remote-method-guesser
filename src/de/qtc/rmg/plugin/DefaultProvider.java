package de.qtc.rmg.plugin;

import java.lang.reflect.Method;

import de.qtc.rmg.internal.ExceptionHandler;
import de.qtc.rmg.io.Logger;
import de.qtc.rmg.operations.RegistryClient;
import de.qtc.rmg.utils.RMGUtils;
import de.qtc.rmg.utils.YsoIntegration;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

public class DefaultProvider implements IArgumentProvider, IPayloadProvider {

    @Override
    public Object getPayloadObject(String action, String name, String args)
    {
        if(name.equalsIgnoreCase("jmx")) {
            String[] split = RMGUtils.splitListener(args);
            return RegistryClient.prepareRMIServerImpl(split[0], Integer.valueOf(split[1]));
        }

        return YsoIntegration.getPayloadObject(name, args);
    }

    @Override
    public Object[] getArgumentArray(String argumentString)
    {
        Object[] result = null;
        ClassPool pool = ClassPool.getDefault();

        try {
            CtClass evaluator = pool.makeClass("de.qtc.rmg.plugin.DefaultProviderEval");
            String evalFunction = "public static Object[] eval() {"
                                + "        return new Object[] { " + argumentString + "};"
                                + "}";

            CtMethod me = CtNewMethod.make(evalFunction, evaluator);
            evaluator.addMethod(me);
            Class<?> evalClass = evaluator.toClass();

            Method m = evalClass.getDeclaredMethods()[0];
            result = (Object[]) m.invoke(evalClass, (Object[])null);

        } catch(VerifyError | CannotCompileException e) {
            Logger.eprintlnMixedYellow("Specified argument string", argumentString, "is invalid.");
            Logger.eprintlnMixedBlue("Make sure to cast all argument to valid", "Object", "types.");
            RMGUtils.exit();

        } catch (Exception e) {
            ExceptionHandler.unexpectedException(e, "argument array", "generation", true);
        }

        return result;
    }
}
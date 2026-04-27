package com.raf.framework.dubbo;

import com.raf.framework.dubbo.core.DubboFilterOrders;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;
import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.constants.CommonConstants;
import static org.apache.dubbo.common.constants.CommonConstants.*;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FILTER_VALIDATION_EXCEPTION;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.io.UnsafeByteArrayInputStream;
import org.apache.dubbo.common.json.GsonUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;
import org.slf4j.Logger;

/**
 * dubbo提供者：泛化调用处理
 * <p>
 * 覆盖 Dubbo 原生 GenericFilter 的原因：
 * 原生实现要求 types 与 args 长度一致，但泛化调用客户端可能不传 types（自动类型推导场景）。
 * 本实现通过 findClazzMethodByMethodSignature 支持 types 为空时按方法名匹配，
 * 解决 types.length == 0 时原生实现抛出 "args.length != types.length" 的问题。
 * </p>
 */
@Activate(group = CommonConstants.PROVIDER, order = DubboFilterOrders.CUST_GENERIC_FILTER)
public class CustGenericFilter implements Filter, ScopeModelAware {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CustGenericFilter.class);
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(org.apache.dubbo.rpc.filter.GenericFilter.class);

    private ApplicationModel applicationModel;

    // C3 修复：缓存 key 包含参数类型，防止重载方法冲突
    private static final ConcurrentMap<String, Method> SIGNATURE_METHODS_CACHE = new ConcurrentHashMap<>();

    // 类加载器级别的类型缓存，static 与 SIGNATURE_METHODS_CACHE 生命周期一致
    private static final Map<ClassLoader, Map<String, Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        if ((inv.getMethodName().equals($INVOKE) || inv.getMethodName().equals($INVOKE_ASYNC))
                && inv.getArguments() != null
                && inv.getArguments().length == 3
                && !GenericService.class.isAssignableFrom(invoker.getInterface())) {

            String name = ((String) inv.getArguments()[0]).trim();
            String[] types = (String[]) inv.getArguments()[1];
            Object[] args = (Object[]) inv.getArguments()[2];

            try {
                Method method = findClazzMethodByMethodSignature(invoker.getInterface(), name, types);
                Class<?>[] params = method.getParameterTypes();
                if (args == null) {
                    args = new Object[params.length];
                }

                String generic = inv.getAttachment(GENERIC_KEY);
                if (StringUtils.isBlank(generic)) {
                    generic = getGenericValueFromRpcContext();
                }

                if (StringUtils.isEmpty(generic)
                        || ProtocolUtils.isDefaultGenericSerialization(generic)
                        || ProtocolUtils.isGenericReturnRawResult(generic)) {
                    try {
                        args = PojoUtils.realize(args, params, method.getGenericParameterTypes());
                    } catch (Exception e) {
                        logger.error(LoggerCodeConstants.PROTOCOL_ERROR_DESERIALIZE, "", "",
                                "Deserialize generic invocation failed. ServiceKey: " + inv.getTargetServiceUniqueName(), e);
                        throw new RpcException(e);
                    }
                } else if (ProtocolUtils.isGsonGenericSerialization(generic)) {
                    args = getGsonGenericArgs(args, method.getGenericParameterTypes());
                } else if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                    Configuration configuration = ApplicationModel.ofNullable(applicationModel)
                            .modelEnvironment().getConfiguration();
                    if (!configuration.getBoolean(CommonConstants.ENABLE_NATIVE_JAVA_GENERIC_SERIALIZE, false)) {
                        String notice = "Trigger the safety barrier! "
                                + "Native Java Serializer is not allowed by default."
                                + "This means currently maybe being attacking by others. "
                                + "If you are sure this is a mistake, "
                                + "please set `" + CommonConstants.ENABLE_NATIVE_JAVA_GENERIC_SERIALIZE + "` enable in configuration! "
                                + "Before doing so, please make sure you have configure JEP290 to prevent serialization attack.";
                        logger.error(CONFIG_FILTER_VALIDATION_EXCEPTION, "", "", notice);
                        throw new RpcException(new IllegalStateException(notice));
                    }
                    for (int i = 0; i < args.length; i++) {
                        if (byte[].class == args[i].getClass()) {
                            try (UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream((byte[]) args[i])) {
                                args[i] = applicationModel.getExtensionLoader(Serialization.class)
                                        .getExtension(GENERIC_SERIALIZATION_NATIVE_JAVA)
                                        .deserialize(null, is).readObject();
                            } catch (Exception e) {
                                throw new RpcException("Deserialize argument [" + (i + 1) + "] failed.", e);
                            }
                        } else {
                            throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_NATIVE_JAVA
                                    + "] only support message type " + byte[].class
                                    + " and your message type is " + args[i].getClass());
                        }
                    }
                } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            if (args[i] instanceof JavaBeanDescriptor) {
                                args[i] = JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) args[i]);
                            } else {
                                throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_BEAN
                                        + "] only support message type " + JavaBeanDescriptor.class.getName()
                                        + " and your message type is " + args[i].getClass().getName());
                            }
                        }
                    }
                } else if (ProtocolUtils.isProtobufGenericSerialization(generic)) {
                    if (args.length == 1 && args[0] instanceof String) {
                        try (UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream(((String) args[0]).getBytes())) {
                            args[0] = applicationModel.getExtensionLoader(Serialization.class)
                                    .getExtension(GENERIC_SERIALIZATION_PROTOBUF)
                                    .deserialize(null, is).readObject(method.getParameterTypes()[0]);
                        } catch (Exception e) {
                            throw new RpcException("Deserialize argument failed.", e);
                        }
                    } else {
                        throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_PROTOBUF
                                + "] only support one " + String.class.getName()
                                + " argument and your message size is " + args.length
                                + " and type is" + args[0].getClass().getName());
                    }
                }

                RpcInvocation rpcInvocation = new RpcInvocation(
                        inv.getTargetServiceUniqueName(),
                        invoker.getUrl().getServiceModel(),
                        method.getName(),
                        invoker.getInterface().getName(),
                        invoker.getUrl().getProtocolServiceKey(),
                        method.getParameterTypes(),
                        args,
                        inv.getObjectAttachments(),
                        inv.getInvoker(),
                        inv.getAttributes(),
                        inv instanceof RpcInvocation ? ((RpcInvocation) inv).getInvokeMode() : null);
                return invoker.invoke(rpcInvocation);
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                throw new RpcException(e.getMessage(), e);
            }
        }
        return invoker.invoke(inv);
    }

    private Object[] getGsonGenericArgs(final Object[] args, Type[] types) {
        return IntStream.range(0, args.length).mapToObj(i -> {
            if (args[i] == null) {
                return null;
            }
            if (!(args[i] instanceof String)) {
                throw new RpcException("When using GSON to deserialize generic dubbo request arguments, the arguments must be of type String");
            }
            try {
                return GsonUtils.fromJson(args[i].toString(), types[i]);
            } catch (RuntimeException ex) {
                throw new RpcException(ex.getMessage());
            }
        }).toArray();
    }

    private String getGenericValueFromRpcContext() {
        String generic = RpcContext.getServerAttachment().getAttachment(GENERIC_KEY);
        if (StringUtils.isBlank(generic)) {
            generic = RpcContext.getClientAttachment().getAttachment(GENERIC_KEY);
        }
        return generic;
    }

    /**
     * 根据方法名和参数类型查找方法，支持 types 为空时的自动类型推导。
     * <p>
     * C3 修复：缓存 key 包含参数类型字符串，防止同名重载方法缓存冲突。
     * </p>
     *
     * @param clazz          接口类
     * @param methodName     方法名
     * @param parameterTypes 参数类型名称数组（可为 null 或空数组，表示自动推导）
     * @return 匹配的方法
     * @throws NoSuchMethodException  方法不存在
     * @throws ClassNotFoundException 参数类型无法加载
     */
    public static Method findClazzMethodByMethodSignature(Class<?> clazz, String methodName, String[] parameterTypes)
            throws NoSuchMethodException, ClassNotFoundException {
        // C3 修复：key 包含参数类型，防止重载方法冲突
        String signature = clazz.getName() + "." + methodName + Arrays.toString(parameterTypes);

        Method method = SIGNATURE_METHODS_CACHE.get(signature);
        if (method != null) {
            return method;
        }

        if (parameterTypes != null && parameterTypes.length > 0) {
            Class<?>[] types = new Class<?>[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                types[i] = ReflectUtils.name2class(parameterTypes[i]);
            }
            method = clazz.getMethod(methodName, types);
        } else {
            // types 为空：自动推导，要求方法名唯一
            List<Method> found = new ArrayList<>();
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName)) {
                    found.add(m);
                }
            }
            if (found.isEmpty()) {
                throw new NoSuchMethodException("No such method " + methodName + " in class " + clazz);
            }
            if (found.size() > 1) {
                throw new IllegalStateException(String.format(
                        "Not unique method for method name(%s) in class(%s), find %d methods.",
                        methodName, clazz.getName(), found.size()));
            }
            method = found.get(0);
        }

        SIGNATURE_METHODS_CACHE.put(signature, method);
        return method;
    }
}

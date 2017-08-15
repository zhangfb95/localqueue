package com.zhangfb95.localqueue.extra.async.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangfb
 */
public class UserTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test() throws Exception {
        Method method = User.class.getDeclaredMethod("call", List.class, String.class, Age.class);
        for (Type type : method.getGenericParameterTypes()) {
            TypeIn typeIn = new TypeIn();
            typeIn.setType(type);
            typeIn.setTypeClass(type.getClass());
            //GenericType genericType = objectMapper.readValue(objectMapper.writeValueAsString(type), GenericType.class);
            System.out.println(objectMapper.writeValueAsString(type));

            System.out.println(type.getTypeName().startsWith("class "));

            /*JavaType javaType2 = TypeFactory.defaultInstance().constructFromCanonical(type.toString());

            JavaType javaType =
                    TypeFactory.defaultInstance().constructSimpleType(Class.forName("java.util.List"),
                                                                      new JavaType[]{
                                                                              TypeFactory
                                                                                      .defaultInstance().constructSimpleType(
                                                                                      Class.forName("java.util.Map"),
                                                                                      new JavaType[]{TypeFactory
                                                                                                             .defaultInstance().constructType(
                                                                                              Class.forName(
                                                                                                      "java.lang.String")),
                                                                                                     TypeFactory
                                                                                                             .defaultInstance().constructType(
                                                                                                             Class.forName(
                                                                                                                     "com.learn.testapi.microservice.UserTest$Age")),
                                                                                                     })});
            System.out.println(javaType);

            List<Map<String, Age>> list = new ArrayList<>();
            list.add(new HashMap<>());
            list.get(0).put("zhangfb", new Age(233L));

            String json = objectMapper.writeValueAsString(list);

            List<Map<String, Age>> thisType = objectMapper.readValue(json, javaType);
            System.out.println(thisType);*/
        }
    }

    @Test
    public void test2() throws Exception {
        Method method = User.class.getDeclaredMethod("call", List.class, String.class, Age.class);
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setMethodName(method.getName());
        ParamInfo paramInfo = genParams(method);
        methodInfo.setParamInfo(paramInfo);
        ReturnInfo returnInfo = genReturn(method);
        methodInfo.setReturnInfo(returnInfo);

        List<Map<String, Age>> firstParam = new ArrayList<>();
        firstParam.add(new HashMap<>());
        firstParam.get(0).put("zhangfb", new Age(233L));
        paramInfo.getParamDataList().add(firstParam);

        String secondParam = "wangw";
        paramInfo.getParamDataList().add(secondParam);
        Age thirdParam = new Age(3233L);
        paramInfo.getParamDataList().add(thirdParam);

        System.out.println(objectMapper.writeValueAsString(methodInfo));
    }

    private ReturnInfo genReturn(Method method) {
        ReturnInfo returnInfo = new ReturnInfo();
        returnInfo.setGenericType(genGenericType(method.getGenericReturnType()));
        return returnInfo;
    }

    private ParamInfo genParams(Method method) {
        ParamInfo paramInfo = new ParamInfo();
        for (Type type : method.getGenericParameterTypes()) {
            Param param = new Param();
            param.setGenericType(genGenericType(type));
            paramInfo.getParams().add(param);
        }

        return paramInfo;
    }

    private GenericType genGenericType(Type type) {
        GenericType genericType = new GenericType();
        if (!type.getTypeName().contains("<")) {
            genericType.setRawType(type.getTypeName());
        } else {
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) type;

            genericType.setRawType(parameterizedType.getRawType().getName());
            genericType.setActualTypeArguments(new ArrayList<>());
            for (Type argType : parameterizedType.getActualTypeArguments()) {
                genericType.getActualTypeArguments().add(genGenericType(argType));
            }
        }
        return genericType;
    }

    public static class User {

        public static void call(List<Map<String, Age>> list, String str, Age age) {
        }
    }

    @Data
    public static class TypeIn {

        private Type type;
        private Class<?> typeClass;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Age {

        private Long idd;
    }
}

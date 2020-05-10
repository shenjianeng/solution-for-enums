# 一站式解决使用枚举的各种痛点
如果变量值仅有有限的可选值，那么用枚举类来定义常量是一个很常规的操作。

但是在业务代码中，**我们不希望依赖 `ordinary()` 进行业务运算，而是自定义数字属性，避免枚举值的增减调序造成影响。**

```java
@Getter
@AllArgsConstructor
public enum CourseType {

    PICTURE(102, "图文"),
    AUDIO(103, "音频"),
    VIDEO(104, "视频"),
    ;

    private final int index;
    private final String name;
}
```

但也正是因为使用了自定义的数字属性，很多框架自带的枚举转化功能也就不再适用了。因此，我们需要自己来扩展相应的转化机制，这其中包括：

1. SpringMVC 枚举转换器
2. ORM 枚举映射
3. JSON 序列化和反序列化

## 自定义 SpringMVC 枚举转换器

### 明确需求
以上文的 `CourseType` 为例，我们希望达到的效果是：

**前端传参时给我们枚举的 `index` 值，在 controller 中，我们可以直接使用 `CourseType` 来接收，由框架负责完成 `index` 到 `CourseType` 的转换。**

```java
@GetMapping("/list")
public void list(@RequestParam CourseType courseType) {
    // do something
}
```

### SpringMVC 自带枚举转换器

SpringMVC 自带了两个和枚举相关的转换器：

- org.springframework.core.convert.support.StringToEnumConverterFactory
- org.springframework.boot.convert.StringToEnumIgnoringCaseConverterFactory

这两个转换器是通过调用枚举的 `valueOf` 方法来进行转换的，感兴趣的同学可以自行查阅源码。

### 实现自定义枚举转换器

虽然这两个转换器不能满足我们的需求，但它也给我们带来了思路，我们可以通过模仿这两个转换器来实现我们的需求：

1. **实现 ConverterFactory 接口，该接口要求我们返回 Converter，这是一个典型的工厂设计模式**
2. **实现 Converter 接口，完成自定义数字属性到枚举类的转化**

废话不多说，上源码：

```java
/**
 * springMVC 枚举类的转换器
 * 如果枚举类中有工厂方法(静态方法)被标记为{@link EnumConvertMethod },则调用该方法转为枚举对象
 */
@SuppressWarnings("all")
public class EnumMvcConverterFactory implements ConverterFactory<String, Enum<?>> {

    private final ConcurrentMap<Class<? extends Enum<?>>, EnumMvcConverterHolder> holderMapper = new ConcurrentHashMap<>();


    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        EnumMvcConverterHolder holder = holderMapper.computeIfAbsent(targetType, EnumMvcConverterHolder::createHolder);
        return (Converter<String, T>) holder.converter;
    }


    @AllArgsConstructor
    static class EnumMvcConverterHolder {
        @Nullable
        final EnumMvcConverter<?> converter;

        static EnumMvcConverterHolder createHolder(Class<?> targetType) {
            List<Method> methodList = MethodUtils.getMethodsListWithAnnotation(targetType, EnumConvertMethod.class, false, true);
            if (CollectionUtils.isEmpty(methodList)) {
                return new EnumMvcConverterHolder(null);
            }
            Assert.isTrue(methodList.size() == 1, "@EnumConvertMethod 只能标记在一个工厂方法(静态方法)上");
            Method method = methodList.get(0);
            Assert.isTrue(Modifier.isStatic(method.getModifiers()), "@EnumConvertMethod 只能标记在工厂方法(静态方法)上");
            return new EnumMvcConverterHolder(new EnumMvcConverter<>(method));
        }

    }

    static class EnumMvcConverter<T extends Enum<T>> implements Converter<String, T> {

        private final Method method;

        public EnumMvcConverter(Method method) {
            this.method = method;
            this.method.setAccessible(true);
        }

        @Override
        public T convert(String source) {
            if (source.isEmpty()) {
                // reset the enum value to null.
                return null;
            }
            try {
                return (T) method.invoke(null, Integer.valueOf(source));
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

    }


}

```

- EnumMvcConverterFactory ：工厂类，用于创建 EnumMvcConverter

- EnumMvcConverter：自定义枚举转换器，完成自定义数字属性到枚举类的转化

- EnumConvertMethod：自定义注解，在自定义枚举类的工厂方法上标记该注解，用于 EnumMvcConverter 来进行枚举转换

EnumConvertMethod 的具体源码如下：

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnumConvertMethod {
}
```

### 怎么使用

1、注册 EnumMvcConverterFactory

```java
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {

    @Bean
    public EnumMvcConverterFactory enumMvcConverterFactory() {
        return new EnumMvcConverterFactory();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // org.springframework.core.convert.support.GenericConversionService.ConvertersForPair.add
        // this.converters.addFirst(converter);
        // 所以我们自定义的会放在前面
        registry.addConverterFactory(enumMvcConverterFactory());
    }
}

```

2、在自定义枚举中提供一个工厂方法，完成自定义数字属性到枚举类的转化，同时在该工厂方法上添加 @EnumConvertMethod 注解

```java
@Getter
@AllArgsConstructor
public enum CourseType {

    PICTURE(102, "图文"),
    AUDIO(103, "音频"),
    VIDEO(104, "视频"),
    ;

    private final int index;
    private final String name;

    private static final Map<Integer, CourseType> mappings;

    static {
        Map<Integer, CourseType> temp = new HashMap<>();
        for (CourseType courseType : values()) {
            temp.put(courseType.index, courseType);
        }
        mappings = Collections.unmodifiableMap(temp);
    }

    @EnumConvertMethod
    @Nullable
    public static CourseType resolve(int index) {
        return mappings.get(index);
    }
}
```

## 自定义 ORM 枚举映射

### 遇到什么问题

还是以上述的 CourseType 枚举为例，一般业务代码的数据都要持久化到 DB 中的。假设，现在有一张课程元数据表，用于记录当前课程所属的类型，我们的 entity 对象可能是这样的：

```java
@Getter
@Setter
@Entity
@Table(name = "course_meta")
public class CourseMeta {
    private Integer id;

    /**
     * 课程类型,{@link CourseType}
     */
    private Integer type;
}
```

上述做法是通过 javadoc 注释的方式来告诉使用方 type 的取值类型是被关联到了 CourseType。

但是，我们希望**通过更清晰的代码来避免注释，让代码不言自明**。

因此，能不能让 ORM 在映射的时候，直接把 Integer 类型的 type 映射成 CourseType 枚举呢？答案是可行的。

### AttributeConverter

我们当前系统使用的是 Spring Data JPA 框架，是对 JPA 的进一步封装。因此，本文只提供在 JPA 环境下的解决方案。

**在 JPA 规范中，提供了 javax.persistence.AttributeConverter 接口，用于扩展对象属性和数据库字段类型的映射。**

```java
public class CourseTypeEnumConverter implements AttributeConverter<CourseType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(CourseType attribute) {
        return attribute.getIndex();
    }

    @Override
    public CourseType convertToEntityAttribute(Integer dbData) {
        return CourseType.resolve(dbData);
    }
}
```

怎么生效呢？有两种方式

1. 将 AttributeConverter 注册到全局 JPA 容器中，此时需要与 javax.persistence.Converter 配合使用
2. 第二种方式是配合 javax.persistence.Convert 使用，在需要的地方指定 AttributeConverter，此时不会全局生效

本文选择的是第二种方式，在需要的地方指定 AttributeConverter，具体代码如下：

```java
@Getter
@Setter
@Entity
@Table(name = "ourse_meta")
public class CourseMeta {
    private Integer id;

    @Convert(converter = CourseTypeEnumConverter.class)
    private CourseType type;
}
```

## JSON 序列化

到这里，我们已经解决了 SpringMVC 和 ORM 对自定义枚举的支持，那是不是这样就足够了呢？还有什么问题呢？

SpringMVC 的枚举转化器只能支持 GET 请求的参数转化，如果前端提交 JSON 格式的 POST 请求，那还是不支持的。

另外，在给前端输出 VO 时，默认情况下，还是要手动把枚举类型映射成 Integer 类型，并不能在 VO 中直接使用枚举输出。

```java
@Data
public class CourseMetaShowVO {
    private Integer id;
    private Integer type;

    public static CourseMetaShowVO of(CourseMeta courseMeta) {
        if (courseMeta == null) {
            return null;
        }
        CourseMetaShowVO vo = new CourseMetaShowVO();
        vo.setId(courseMeta.getId());
        // 手动转化枚举
        vo.setType(courseMeta.getType().getIndex());
        return vo;
    }
}
```

### @JsonValue 和 @JsonCreator

Jackson 是一个非常强大的 JSON 序列化工具，SpringMVC 默认也是使用 Jackson 作为其 JSON 转换器。

Jackson 为我们提供了两个注解，刚好可以解决这个问题。

- **@JsonValue： 在序列化时，只序列化 @JsonValue 注解标注的值**
- **@JsonCreator：在反序列化时，调用 @JsonCreator 标注的构造器或者工厂方法来创建对象**

最后的代码如下：

```java
@Getter
@AllArgsConstructor
public enum CourseType {

    PICTURE(102, "图文"),
    AUDIO(103, "音频"),
    VIDEO(104, "视频"),
    ;

    @JsonValue
    private final int index;
    private final String name;

    private static final Map<Integer, CourseType> mappings;

    static {
        Map<Integer, CourseType> temp = new HashMap<>();
        for (CourseType courseType : values()) {
            temp.put(courseType.index, courseType);
        }
        mappings = Collections.unmodifiableMap(temp);
    }

    @EnumConvertMethod
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    @Nullable
    public static CourseType resolve(int index) {
        return mappings.get(index);
    }
}
```




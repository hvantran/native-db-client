package com.hvantran.sqlnative.repository.proxy;

import com.hvantran.sqlnative.annotations.Set;
import com.hvantran.sqlnative.annotations.*;
import com.hvantran.sqlnative.interfaces.CheckedConsumer;
import com.hvantran.sqlnative.interfaces.CheckedSupplier;
import com.hvantran.sqlnative.interfaces.GenericRepository;
import com.hvantran.sqlnative.utils.InstanceUtils;
import com.hvantran.sqlnative.utils.ObjectUtils;
import com.hvantran.sqlnative.utils.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class RepoProxyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoProxyFactory.class);

    private RepoProxyFactory() {

    }

    @SuppressWarnings("unused")
    public static <T extends GenericRepository> T getRepositoryProxyInstance(Class<T> kInterface) {
        return getRepositoryProxyInstance(kInterface, new Properties());
    }

    @SuppressWarnings("unchecked")
    public static <T extends GenericRepository> T getRepositoryProxyInstance(Class<T> kInterface, Properties properties) {
        ClassLoader classLoader = kInterface.getClassLoader();
        ConnectionManager connectionManager = new ConnectionManager();
        return (T) Proxy.newProxyInstance(
                classLoader, new Class[]{kInterface}, new DefaultInvocationHandler(connectionManager, properties, kInterface));
    }

    private static class DefaultInvocationHandler implements InvocationHandler {
        private final Properties configuration;
        private final ConnectionManager connectionManager;
        private final Class<? extends GenericRepository> genericRepository;

        public DefaultInvocationHandler(ConnectionManager connectionManager, Properties configuration, Class<? extends GenericRepository> genericRepository) {
            this.connectionManager = connectionManager;
            this.configuration = configuration;
            this.genericRepository = genericRepository;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] objects) throws Throwable {
            LOGGER.info("Invoke method name: {}", method.getName());

            if ("close".equals(method.getName())) {
                ObjectUtils.checkThenThrow(Objects.isNull(connectionManager.getConnection()), "Cannot close connection because it is not open");
                connectionManager.getConnection().close();
                LOGGER.info("Connection is closed");
                return null;
            }

            Connection connection = Optional.ofNullable(connectionManager.getConnection())
                    .orElseGet(() -> {
                        Database database = genericRepository.getAnnotation(Database.class);
                        return connectionManager.initialConnection(database, configuration);
                    });


            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            List<Pair<Param, Object>> paramValueMap = IntStream.range(0, objects.length).mapToObj(index -> {
                Optional<Param> paramOptional = Arrays.stream(parameterAnnotations[index])
                        .filter(Param.class::isInstance)
                        .map(Param.class::cast)
                        .findFirst();
                return paramOptional.map(param -> Pair.of(param, objects[index])).orElse(null);
            }).filter(Objects::nonNull).toList();

            Class mappingToClass = IntStream.range(0, objects.length)
                    .filter(parameterIndex -> objects[parameterIndex] instanceof Class<?>)
                    .mapToObj(parameterIndex -> (Class) objects[parameterIndex])
                    .findFirst().orElse(null);

            QueryInfo queryInfo = QueryInfo.builder()
                    .select(method.getAnnotation(Select.class))
                    .from(method.getAnnotation(From.class))
                    .where(method.getAnnotation(Where.class))
                    .orderBy(method.getAnnotation(OrderBy.class))
                    .insert(method.getAnnotation(Insert.class))
                    .values(method.getAnnotation(Values.class))
                    .update(method.getAnnotation(Update.class))
                    .set(method.getAnnotation(Set.class))
                    .delete(method.getAnnotation(Delete.class))
                    .nativeQuery(method.getAnnotation(NativeQuery.class))
                    .mappingToClass(mappingToClass)
                    .paramPairs(paramValueMap)
                    .build();
            QuerySelection querySelection = queryInfo.getQuerySelection();
            Pair<ResultSet, PreparedStatement> result = querySelection.execute(queryInfo, connection);
            return processResponseHandler(queryInfo, result.getKey(), result.getValue());
        }

        private static Object processResponseHandler(QueryInfo queryInfo, ResultSet resultSet, PreparedStatement preparedStatement) throws SQLException {
            if (queryInfo.getQuerySelection() != QuerySelection.SELECT) {
                int updateCount = preparedStatement.getUpdateCount();
                LOGGER.info("Number of affect records: {}", updateCount);
                return updateCount;
            }
            try (resultSet; preparedStatement){
                LOGGER.info("Mapping SELECT query response to DTO classes");
                List<Object> outputDtoInstances = new ArrayList<>();
                while (resultSet.next()) {
                    Class<?> mappingToClass = queryInfo.getMappingToClass();
                    if (Objects.nonNull(mappingToClass)) {
                        Object dto = InstanceUtils.newInstance(mappingToClass);
                        Field[] fields = mappingToClass.getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                        }

                        for (Field field : fields) {
                            Column column = field.getAnnotation(Column.class);
                            if (Objects.nonNull(column)) {
                                String name = column.name();
                                CheckedSupplier<String> valueSupplier = () -> resultSet.getString(name);
                                String value = valueSupplier.get();
                                CheckedConsumer<Object> objectCheckedConsumer = input -> field.set(input, value);
                                objectCheckedConsumer.accept(dto);
                            }
                        }
                        outputDtoInstances.add(dto);
                    }
                }
                return outputDtoInstances;
            }
        }
    }

    @Setter
    @Getter
    private static class ConnectionManager {

        private Connection connection;

        public Connection initialConnection(Database database, Properties configuration) {
            if (Objects.isNull(connection)) {
                String databaseURL = checkThenGetFromProperties(database.url(), configuration);
                ObjectUtils.checkThenThrow(StringUtils.isEmpty(databaseURL), "Database URL cannot be empty");

                LOGGER.info("Initial connection to database: {}", databaseURL);
                String username = checkThenGetFromProperties(database.username(), configuration);
                String password = checkThenGetFromProperties(database.password(), configuration);
                ObjectUtils.checkThenThrow(StringUtils.isEmpty(username) && StringUtils.isNotEmpty(password),
                        "Missing database username");
                ObjectUtils.checkThenThrow(StringUtils.isEmpty(password) && StringUtils.isNotEmpty(username),
                        "Missing database password");
                CheckedSupplier<Connection> connectionCheckedSupplier = () -> {
                    if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                        return DriverManager.getConnection(databaseURL, username, password);
                    }
                    return DriverManager.getConnection(databaseURL);
                };
                setConnection(connectionCheckedSupplier.get());
            }
            return getConnection();
        }

        private String checkThenGetFromProperties(String input, Properties properties) {
            Pattern pattern = Pattern.compile("^(\\{)([\\w \\.]+)(\\})$");
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                String propertyName = matcher.group(2);
                return input.replace("{".concat(propertyName).concat("}"), properties.getProperty(propertyName));
            }
            return input;
        }
    }
}

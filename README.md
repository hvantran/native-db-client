# native-db-client


## 1. Intro
An simple java libary to executing SQL queries, with this libary we can

- Easy to create and execute SQL query to database with supported annotations
- Mapping response of select clause to DTO classes
- Parameterize the SQL queries with method arguments
- Lazy init connection to database util the first method in repository is called
- Only one database connection for each repository instance, that mean executing next method in repository will use the existing connection

**Simple example**: Selecting some columns from specific tables
### Define repositories
It is very simple by using annotations, **it required to extend from GenericRepository**. 
```java
@Database(url = "{spring.datasource.url}", username = "{spring.datasource.username}", password = "{spring.datasource.password}")
public interface EndpointSettingRepository extends GenericRepository {

    @Select("ID, COLUMN1, COLUMN2, COLUMN3")
    @From("endpoint_response")
    @Where("endpoint_config_id = {endpoint_config_id} AND COLUMN8 is NULL AND COLUMN10 LIKE {column10}")
    List<EndpointResponseDTO> getEndpointResponses(Class<EndpointResponseDTO> responseHandler, @Param("endpoint_config_id") String endpointSettingId, @Param("column10")String filterCol10);
    
    @NativeQuery("SELECT response.ID, setting.application " +
            "FROM endpoint_response as response, endpoint_setting as setting " +
            "WHERE response.endpoint_config_id = setting.id")
    List<EndpointResponseV1> selectFields(Class<EndpointResponseV1> klass);
```
### Define DTO classes
```java
@Getter
@Setter
@ToString
public class EndpointResponseDTO {

    private String id;

    private String column1;

    private String column3;
    private String column2;
}
```
### Using repositories
Using **RepoProxyFactory** to create instance from interfaces
```java
EndpointSettingRepository endpointSettingRepository = RepoProxyFactory.getRepositoryProxyInstance(EndpointSettingRepository.class, properties);
```
Now, you can call the methods to executing SQL query to database
```java
    List<EndpointResponseVO> monkeyEndpointResponse = endpointSettingRepository
                .getMonkeyEndpointResponse(EndpointResponseVO.class, endpointSettings.get(0).getId(), "'A%'");
    System.out.println(monkeyEndpointResponse);
    
    //Output: [EndpointResponseVO(id=2, column1=DSA, column3=null, column2=ABCD)]
```

**Notes: Don't forget to call the close method on the repository when you done all the operations**


## 2. Install dependency from Maven Central repository
```maven
<dependency>
    <groupId>io.github.hvantran</groupId>
    <artifactId>native-db-client</artifactId>
    <version>1.1.1</version>
</dependency>
```
## 3. Annotations
### a. Database
**Database** annotation respresent for database connection.
It supports to input connection directly or binding from properties by using **{}** syntax

### b. Select columns
**Select** annotation respresent for an select statement.
It supports prammeterize by using **{}** syntax, and can combine with below annotations:
- From (required)
- Where (optional)
- OrderBy (optional)

### c. Update record
**Update** annotation respresent for an update statement.
It supports prammeterize by using **{}** syntax, and can combine with below annotations:
- Set (required)
- Where (optional)

### d. Delete records
**Delete** annotation respresent for an delete statement.
It supports prammeterize by using **{}** syntax, and can combine with below annotations:
- Set (required)
- Where (optional)

### e. Insert records
**Insert** annotation respresent for an insert statement.
It supports prammeterize by using **{}** syntax, and can combine with below annotations:
- Values (required)

### f. Generic query with NativeQuery
**NativeQuery** annotation is a generic query. It can support prammeterize by using **{}** syntax

### h. Param
**Param** annotation will matching a method argument with SQL query param

## 4. Fully example with INSERT, UPDATE, DELETE, SELECT queries

### DTO classes
```java
@Getter
@Setter
@ToString
public class EndpointResponseVO {

    private String id;

    private String column1;

    private String column3;
    private String column2;
}

@Getter
@Setter
@ToString
public class EndpointResponseV1 {

    private String application;

    private String id;
}

@Getter
@Setter
@ToString
public class EndpointSettingIdVO {
    private String id;
}
```
### Create Repository
```java
@Database(url = "{spring.datasource.url}", username = "{spring.datasource.username}", password = "{spring.datasource.password}")
public interface EndpointSettingRepository extends GenericRepository {

    @Select("ID")
    @From("endpoint_setting")
    @Where("application LIKE '{application}'")
    @OrderBy("application ASC")
    List<EndpointSettingIdVO> getEndpointSetting(@Param("application") String applicationName, Class<EndpointSettingIdVO> responseHandler);


    @Select("ID, COLUMN1, COLUMN2, COLUMN3")
    @From("endpoint_response")
    @Where("endpoint_config_id = {endpoint_config_id} AND COLUMN8 is NULL AND COLUMN10 LIKE {column10}")
    List<EndpointResponseVO> getEndpointResponse(Class<EndpointResponseVO> responseHandler, @Param("endpoint_config_id") String endpointSettingId, @Param("column10")String filterCol10);

    @Update("endpoint_response")
    @Set("COLUMN8={new_password}")
    @Where("ID={endpoint_response_id}")
    int update(@Param("new_password") String newPassword, @Param("endpoint_response_id") String id);

    @Delete("endpoint_response")
    @Where("ID={endpoint_response_id}")
    int delete(@Param("endpoint_response_id") String id);

    @Insert("endpoint_response (ID, column1, column2, endpoint_config_id)")
    @Values("(2, 'DSA', 'ABCD', {endpoint_response_id})")
    int insert(@Param("endpoint_response_id") String id);

    @NativeQuery("SELECT response.ID, setting.application " +
            "FROM endpoint_response as response, endpoint_setting as setting " +
            "WHERE response.endpoint_config_id = setting.id")
    List<EndpointResponseV1> getEndpointResponseByNativeQuery(Class<EndpointResponseV1> klass);
}
```

### 5. Using repositories

Step 1: Create repository instances by using **RepoProxyFactory**
```java
    // Load properties file (optional)
    InputStream resource = MOnkeyAccountCheckerV1.class.getClassLoader().getResourceAsStream("application.properties");
    Properties properties = new Properties();
    properties.load(resource);

    //** Create repository isntance by using Proxy factory**
    EndpointSettingRepository endpointSettingRepository = RepoProxyFactory.getRepositoryProxyInstance(EndpointSettingRepository.class, properties);
```

Step 2: **Execute method in repository**
```java
    List<EndpointSettingIdVO> endpointSettings = endpointSettingRepository.getEndpointSetting("MOnkey%", EndpointSettingIdVO.class);
    System.out.println(endpointSettings);

    List<EndpointResponseVO> endpointResponses = endpointSettingRepository
            .getEndpointResponse(EndpointResponseVO.class, endpointSettings.get(0).getId(), "'A%'");
    System.out.println(endpointResponses);

    int numberOfAffectRecords = endpointSettingRepository.update("'abcdsaefasd'", endpointResponses.get(0).getId());
    System.out.println("Number of updated records: "+ numberOfAffectRecords);

    numberOfAffectRecords = endpointSettingRepository.delete(endpointResponses.get(0).getId());
    System.out.println("Number of deleted records: "+ numberOfAffectRecords);

    numberOfAffectRecords = endpointSettingRepository.insert("1");
    System.out.println("Number of insert records: "+ numberOfAffectRecords);

    List<EndpointResponseV1> monkeyEndpointSettingId1s= endpointSettingRepository.getEndpointResponseByNativeQuery(EndpointResponseV1.class);
    System.out.println(monkeyEndpointSettingId1s);

    endpointSettingRepository.close();
```

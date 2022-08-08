# native-db-client


## 1. Intro
An simple java libary to executing SQL queries, with this libary we can

- Easy to create and execute SQL query to database with supported annotations
- Mapping response of select clause to DTO classes
- Parameterize the SQL queries with method arguments
- Lazy init connection to database util the first method in repository is called
- Only one database connection for each repository instance, that mean executing next method in repository will use the existing connection

## 2. Install dependency from Maven Central repository
```
<dependency>
    <groupId>io.github.hvantran</groupId>
    <artifactId>native-db-client</artifactId>
    <version>1.0.2</version>
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

### g. Column
**Column** annotation represent for a column in database, it will be mapped to a property in instances

### h. Param
**Param** annotation will matching a method argument with SQL query param

## 4. Using

### DTO classes
**DTO** classes are using to mapping the collumns in response of SELECT command to DTO instances
```
public class EndpointResponseVO {

    @Column(name = "ID")
    private String id;

    @Column(name = "COLUMN1")
    private String column1;

    @Column(name = "COLUMN3")
    private String column3;

    @Column(name = "COLUMN2")
    private String column2;
}

```
### Repositories
Define repositories very simple by using annotations, **it required to extend from GenericRepository**. 

Look at below example:
```
@Database(url = "{spring.datasource.url}", username = "{spring.datasource.username}", password = "{spring.datasource.password}")
public interface EndpointSettingRepository extends GenericRepository {

    @Select("ID")
    @From("endpoint_setting")
    @Where("application LIKE '{application}'")
    @OrderBy("application ASC")
    List<EndpointSettingIdVO> getEndpointSettings(@Param("application") String applicationName, Class<EndpointSettingIdVO> responseHandler);


    @Select("ID, COLUMN1, COLUMN2, COLUMN3")
    @From("endpoint_response")
    @Where("endpoint_config_id = {endpoint_config_id} AND COLUMN8 is NULL AND COLUMN10 LIKE {column10}")
    List<EndpointResponseVO> getEndpointResponses(Class<EndpointResponseVO> responseHandler, @Param("endpoint_config_id") String endpointSettingId, @Param("column10")String filterCol10);

    @Update("endpoint_response")
    @Set("COLUMN8={new_password}")
    @Where("ID={endpoint_response_id}")
    int updatePassword(@Param("new_password") String newPassword, @Param("endpoint_response_id") String id);

    @Delete("endpoint_response")
    @Where("ID={endpoint_response_id}")
    int deleteEndpointResponse(@Param("endpoint_response_id") String id);

    @Insert("endpoint_response (ID, column1, column2, endpoint_config_id)")
    @Values("(2, 'DSA', 'ABCD', {endpoint_response_id})")
    int insertEndpointResponse(@Param("endpoint_response_id") String id);
    
    @NativeQuery("SELECT ID FROM endpoint_response WHERE endpoint_config_id = {endpoint_response_id}")
    List<EndpointSettingIdVO> getEndpointResponseByNativeQuery(@Param("endpoint_response_id") String id, Class<EndpointSettingIdVO> klass);

```

**That all what we need, now we can use the repositories**

### 5. Using repositories

Step 1: Create repository instances by using **RepoProxyFactory**
```
    // Load properties file (optional)
    InputStream resource = MOnkeyAccountCheckerV1.class.getClassLoader().getResourceAsStream("application.properties");
    Properties properties = new Properties();
    properties.load(resource);

    //** Create repository isntance by using Proxy factory**
    EndpointSettingRepository endpointSettingRepository = RepoProxyFactory.getRepositoryProxyInstance(EndpointSettingRepository.class, properties);
```

Step 2: **Execute queries**
```
    List<EndpointSettingIdVO> endpointSettings = endpointSettingRepository.getEndpointSettings("MOnkey%", EndpointSettingIdVO.class);
    System.out.println(endpointSettings);

    List<EndpointResponseVO> endpointResponses = endpointSettingRepository
            .getEndpointResponses(EndpointResponseVO.class, endpointSettings.get(0).getId(), "'A%'");
    System.out.println(endpointResponses);
    
    endpointSettingRepository.updateEndpointResponse("'abcdsaefasd'", endpointSettings.get(0).getId());
    
    endpointSettingRepository.deleteEndpointResponse(endpointSettings.get(0).getId());
    endpointSettingRepository.insertEndpointResponse("1");

    endpointResponses= endpointSettingRepository.getEndpointResponseByNativeQuery("1", EndpointSettingIdVO.class);
    System.out.println(endpointResponses);
```

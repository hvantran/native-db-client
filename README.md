# native-db-client

An application is used to executing native database query based on annotations, then mapping the response to DTO classes


# Using

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
Define repositories very simple by using annotations
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

That all what we need, now we can use the repositories
### Using repositories
```
    // Load properties file
    InputStream resource = MOnkeyAccountCheckerV1.class.getClassLoader().getResourceAsStream("application.properties");
    Properties properties = new Properties();
    properties.load(resource);

    // Create repository isntance by using Proxy factory
    EndpointSettingRepository endpointSettingRepository = RepoProxyFactory.getRepositoryProxyInstance(EndpointSettingRepository.class, properties);

    List<EndpointSettingIdVO> endpointSettings = endpointSettingRepository.getEndpointSettings("MOnkey%", EndpointSettingIdVO.class);
    System.out.println(endpointSettings);

    List<EndpointResponseVO> endpointResponses = endpointSettingRepository
            .getEndpointResponses(EndpointResponseVO.class, monkeyEndpointSettingIds.get(0).getId(), "'A%'");
    System.out.println(endpointResponses);
    
    endpointSettingRepository.updateEndpointResponse("'abcdsaefasd'", monkeyEndpointResponse.get(0).getId());
    
    endpointSettingRepository.deleteEndpointResponse(monkeyEndpointResponse.get(0).getId());
    endpointSettingRepository.insertEndpointResponse("1");

    endpointResponses= endpointSettingRepository.getEndpointResponseByNativeQuery("1", EndpointSettingIdVO.class);
    System.out.println(endpointResponses);
```

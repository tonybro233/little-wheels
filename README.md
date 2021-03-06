# Little Wheels

以SpringBoot为载体，一些Java后端的轮子代码。不依赖除了Spring（包括一些Spring子项目）之外的内容。

## common

- PageWrapper

  集合分页包装器，将java集合包装为分页对象(`org.springframework.data.domain.Page`)

- SnowFlake

  Twitter开源分布式id生成器实现

- SSLContextFactory

  SSLContext样板代码

- RandomSubListUtils

  List随机子集工具

## web

- JSON请求体绑定到多个参数

  为了接收application/json格式的请求体，往往需要在接口方法中使用`@RequestBody`并为此定义大量POJO作为载体，也许你希望像使用`@RequestParam`一样来接收请求体中的内容，`@RequestBodyParam`正源于此。`@RequestParam`有的（defaultValue、required、spel expression），`@RequestBodyParam`基本都有，此外同时支持`@Valid`校验（如果是个POJO）。

  Example：
  
  ```java
  @Validated
  @RestController
  @RequestMapping("/api/test")
  public class TestController {
  
      // {
      // 	  "name": "Tony",
      // 	  "phone": 123456789
      // }
      
      @PostMapping("/basic")
      public ResponseEntity basic(@RequestBodyParam(defaultValue = "defaultName") String name,
                                  @RequestBodyParam(required = false) String phone) {
          return ResponseEntity.ok("name is " + name + ", phone is" + phone);
      }
      
      // {
      // 	  "operator": "Tony",
      // 	  "timestamp": "1598371219",
      // 	  "order" : {
      //        "id": 123,
      //        "amount" : 999.99,
      //        "buyer": ["Jack", "Tom", "Alice"]
      // 	  }
      // } 
      
      @PostMapping("/complex")
      public ResponseEntity complex(@NotEmpty @RequestBodyParam("operator") String op, 
                                    @Positive @RequestBodyParam Long timestamp, 
                                    @Valid @RequestBodyParam OrderDto order) {
          System.out.println("Operator:" + op);
          System.out.println("Timestamp:" + timestamp);
          System.out.println("Order buyer size:" + order.getBuyers().size());
          return ResponseEntity.ok("good");
      }
      
      static class OrderDto {
  
          private Integer id;
  
          private Double amount;
  
          @NotEmpty
          private List<String> buyers;
  
          // getter, setter, toString
          // ...
      }      
  
      // {
      // 	  "a": {
      // 	     "b": {
      //          "c": "That's it"
      // 	     }
      // 	  }
      // }
      
      @PostMapping("/pointer")
      public ResponseEntity originForm(@RequestBodyParam("/a/b/c") String c) {
          return ResponseEntity.ok("c value: " + c);
      }
  
  }
  ```
  
  - RequestBodyParam
  - RequestBodyParamArgumentResolver
  - RequestBodyParamConfiguration
  
- 自定义参数校验注解示例

  Web接口通常需要进行参数校验，使用validation注解是一个很方便的方式，尤其是在Spring环境下。这部分java已经提供了规范（JSR303），规范原生提供了一些校验注解如`@NotEmpty`、`@Size`等等，但是实际应用中的参数校验可能需要一些额外的规则，此时可以考虑使用自定义校验注解，项目中给出了一个示例：校验字符串的字节长度。

  - @ByteSize

## jpa

- Java8 Time Converter

  用于在JPA Entity中支持使用LocalDate与LocalDateTime

  - LocalDateAttributeConverter
  - LocalDateTimeAttributeConverter
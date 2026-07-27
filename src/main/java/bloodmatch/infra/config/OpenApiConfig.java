package bloodmatch.infra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI bloodmatchOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("BloodMatch API")
            .version("v1")
            .description("API for managing donors, donations, and blood donation requests."))
        .addTagsItem(new Tag().name("Authentication").description("Authentication and access-token operations."))
        .addTagsItem(new Tag().name("Donations").description("Operations for creating, completing, and viewing donations."))
        .addTagsItem(new Tag().name("Donation Requests").description("Operations for creating and managing blood donation requests."))
        .addTagsItem(new Tag().name("Donation Request Recommendations").description("Operations for finding recommended donation requests for donors."))
        .addTagsItem(new Tag().name("Parties").description("Operations for registering and managing people and organizations."))
        .addTagsItem(new Tag().name("Donors").description("Operations for registering donors and managing donor profiles."))
        .addTagsItem(new Tag().name("Blood Centers").description("Operations for registering blood centers."))
        .addTagsItem(new Tag().name("Requesters").description("Operations for registering blood requesters."))
        .components(new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide the JWT returned by POST /auth/login.")));
  }
}

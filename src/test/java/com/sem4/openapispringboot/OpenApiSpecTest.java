package com.sem4.openapispringboot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSpecTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("D1 & D10: Kiểm tra đặc tả OpenAPI 3.1 cho nhóm public-v1")
    void specDescribesPublicV1() throws Exception {
        mvc.perform(get("/v3/api-docs/public-v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", startsWith("3.1")))
            .andExpect(jsonPath("$.info.title").value("TechShop Order & Catalog API"));

        exportYamlSpec("public-v1");
    }

    @Test
    @DisplayName("D1 & D10: Kiểm tra đặc tả OpenAPI 3.1 cho nhóm public-v2")
    void specDescribesPublicV2() throws Exception {
        mvc.perform(get("/v3/api-docs/public-v2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", startsWith("3.1")))
            .andExpect(jsonPath("$.paths['/api/orders'].post.responses['201']").exists())
            .andExpect(jsonPath("$.paths['/api/orders'].post.responses['409']").exists());

        exportYamlSpec("public-v2");
    }

    @Test
    @DisplayName("D1 & D10: Kiểm tra đặc tả OpenAPI 3.1 cho nhóm admin")
    void specDescribesAdmin() throws Exception {
        mvc.perform(get("/v3/api-docs/admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", startsWith("3.1")))
            .andExpect(jsonPath("$.paths['/api/admin/reports/daily-revenue'].get.responses['200']").exists());

        exportYamlSpec("admin");
    }

    @Test
    @DisplayName("D1 & D10: Kiểm tra đặc tả OpenAPI 3.1 cho nhóm partner")
    void specDescribesPartner() throws Exception {
        mvc.perform(get("/v3/api-docs/partner"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", startsWith("3.1")))
            .andExpect(jsonPath("$.paths['/api/webhooks/shipping'].post.responses['200']").exists());

        exportYamlSpec("partner");
    }

    private void exportYamlSpec(String group) {
        try {
            String json = mvc.perform(get("/v3/api-docs/" + group))
                .andReturn().getResponse().getContentAsString();

            com.fasterxml.jackson.databind.JsonNode tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            com.fasterxml.jackson.dataformat.yaml.YAMLMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
            byte[] yamlBytes = yamlMapper.writeValueAsBytes(tree);

            Path docsDir = Paths.get("docs", "api");
            Files.createDirectories(docsDir);
            Path targetPath = docsDir.resolve("openapi-" + group + ".yaml");
            Files.write(targetPath, yamlBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

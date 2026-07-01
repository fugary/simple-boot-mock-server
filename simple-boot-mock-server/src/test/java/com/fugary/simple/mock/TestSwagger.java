package com.fugary.simple.mock;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.core.util.Json;

public class TestSwagger {
    public static void main(String[] args) throws Exception {
        String json = "{\n" +
                "  \"openapi\": \"3.0.1\",\n" +
                "  \"info\": {\"title\": \"test\", \"version\": \"1.0\"},\n" +
                "  \"paths\": {\n" +
                "    \"/test\": {\n" +
                "      \"get\": {\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"description\": \"ok\",\n" +
                "            \"content\": {\n" +
                "              \"application/json\": {\n" +
                "                \"schema\": {\"$ref\": \"#/components/schemas/MyModel\"}\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"components\": {\n" +
                "    \"schemas\": {\n" +
                "      \"MyModel\": {\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": {\n" +
                "          \"id\": {\"type\": \"integer\"}\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(false);
        SwaggerParseResult result = new OpenAPIParser().readContents(json, null, parseOptions);
        OpenAPI openAPI = result.getOpenAPI();
        Schema schema = openAPI.getPaths().get("/test").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        System.out.println("resolveFully=false Schema JSON: " + Json.mapper().writeValueAsString(schema));
        System.out.println("resolveFully=false components empty: " + (openAPI.getComponents() == null));

        ParseOptions parseOptions2 = new ParseOptions();
        parseOptions2.setResolve(true);
        parseOptions2.setResolveFully(true);
        SwaggerParseResult result2 = new OpenAPIParser().readContents(json, null, parseOptions2);
        OpenAPI openAPI2 = result2.getOpenAPI();
        Schema schema2 = openAPI2.getPaths().get("/test").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        System.out.println("resolveFully=true Schema JSON: " + Json.mapper().writeValueAsString(schema2));
        System.out.println("resolveFully=true components empty: " + (openAPI2.getComponents() == null));
    }
}

package server.executions.internal;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.springframework.stereotype.Component;
import server.executions.InvalidCommandArgsException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
class CommandArgsValidator {

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;

    CommandArgsValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    void validate(String argsSchema, String argsJson) {
        JsonNode args = readTree(argsJson);
        Schema schema = schemaRegistry.getSchema(readTree(argsSchema));

        List<Error> errors = schema.validate(args);
        if (!errors.isEmpty()) {
            Map<String, String> errorsByField = new LinkedHashMap<>();
            for (Error error : errors) {
                String field = fieldNameOf(error);
                errorsByField.merge(field, error.getMessage(), (first, second) -> first + "; " + second);
            }
            throw new InvalidCommandArgsException(errorsByField);
        }
    }

    private String fieldNameOf(Error error) {
        if (error.getProperty() != null && !error.getProperty().isEmpty()) {
            return error.getProperty();
        }
        String instanceLocation = error.getInstanceLocation().toString();
        return instanceLocation.startsWith("/") ? instanceLocation.substring(1) : instanceLocation;
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException e) {
            throw InvalidCommandArgsException.malformedJson();
        }
    }
}

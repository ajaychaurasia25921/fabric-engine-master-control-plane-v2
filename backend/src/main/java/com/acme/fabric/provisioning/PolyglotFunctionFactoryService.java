package com.acme.fabric.provisioning;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.acme.fabric.domain.FabricModels.GeneratedSourceFile;
import com.acme.fabric.domain.FabricModels.PolyglotFunctionBlueprintRequest;
import com.acme.fabric.domain.FabricModels.PolyglotFunctionBlueprintResponse;
import com.acme.fabric.domain.FabricModels.PolyglotScriptRequest;
import com.acme.fabric.domain.FabricModels.PolyglotScriptResponse;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class PolyglotFunctionFactoryService {
    public Mono<PolyglotFunctionBlueprintResponse> generateBlueprint(PolyglotFunctionBlueprintRequest request) {
        return Mono.fromSupplier(() -> switch (normalize(request.targetPlatform())) {
            case "AWS_LAMBDA", "AWS" -> awsLambdaBlueprint(request);
            case "AZURE_FUNCTIONS", "AZURE" -> azureFunctionsBlueprint(request);
            default -> throw new IllegalArgumentException("Unsupported targetPlatform: " + request.targetPlatform());
        });
    }

    public Mono<PolyglotScriptResponse> evaluate(PolyglotScriptRequest request) {
        return Mono.fromCallable(() -> {
            String language = normalizeLanguage(request.language());
            try (Context context = Context.newBuilder(language)
                    .allowAllAccess(false)
                    .allowCreateThread(false)
                    .allowCreateProcess(false)
                    .allowEnvironmentAccess(org.graalvm.polyglot.EnvironmentAccess.NONE)
                    .allowIO(false)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build()) {
                context.getBindings(language).putMember("input", request.bindings() == null ? Map.of() : request.bindings());
                Value value = context.eval(language, request.source());
                return new PolyglotScriptResponse(
                        UUID.randomUUID().toString(),
                        language,
                        toJavaValue(value),
                        Instant.now()
                );
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private PolyglotFunctionBlueprintResponse awsLambdaBlueprint(PolyglotFunctionBlueprintRequest request) {
        String packageName = "com.acme.fabric.generated." + safeJavaSegment(request.functionName());
        String className = "PolyglotLambdaHandler";
        return new PolyglotFunctionBlueprintResponse(
                "fn-" + safeName(request.functionName()) + "-" + UUID.randomUUID().toString().substring(0, 8),
                "AWS_LAMBDA",
                "java17-graalvm-polyglot-js",
                List.of(
                        new GeneratedSourceFile("build.gradle", awsGradleBuild(packageName)),
                        new GeneratedSourceFile("src/main/java/" + packageName.replace('.', '/') + "/" + className + ".java",
                                awsHandler(packageName, className)),
                        new GeneratedSourceFile("src/main/resources/function.js", request.handlerSource()),
                        new GeneratedSourceFile("template.yaml", awsSamTemplate(packageName, className, request.functionName()))
                ),
                List.of(
                        "Build with: gradle clean build",
                        "Package for Lambda with AWS SAM or your internal deployment pipeline.",
                        "The handler evaluates src/main/resources/function.js with GraalVM and passes the Lambda event as input."
                )
        );
    }

    private PolyglotFunctionBlueprintResponse azureFunctionsBlueprint(PolyglotFunctionBlueprintRequest request) {
        String packageName = "com.acme.fabric.generated." + safeJavaSegment(request.functionName());
        String className = "PolyglotAzureFunction";
        return new PolyglotFunctionBlueprintResponse(
                "fn-" + safeName(request.functionName()) + "-" + UUID.randomUUID().toString().substring(0, 8),
                "AZURE_FUNCTIONS",
                "java17-graalvm-polyglot-js",
                List.of(
                        new GeneratedSourceFile("build.gradle", azureGradleBuild(packageName)),
                        new GeneratedSourceFile("src/main/java/" + packageName.replace('.', '/') + "/" + className + ".java",
                                azureHandler(packageName, className, request.functionName())),
                        new GeneratedSourceFile("src/main/resources/function.js", request.handlerSource()),
                        new GeneratedSourceFile("host.json", "{\n  \"version\": \"2.0\"\n}\n")
                ),
                List.of(
                        "Build with: gradle clean build",
                        "Deploy with Azure Functions Core Tools or your internal release pipeline.",
                        "The function evaluates src/main/resources/function.js with GraalVM and passes the HTTP body as input."
                )
        );
    }

    private String awsGradleBuild(String packageName) {
        return """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'com.amazonaws:aws-lambda-java-core:1.2.3'
                    implementation 'org.graalvm.polyglot:polyglot:25.0.3'
                    implementation 'org.graalvm.polyglot:js:25.0.3'
                }

                java {
                    toolchain {
                        languageVersion = JavaLanguageVersion.of(17)
                    }
                }
                """;
    }

    private String azureGradleBuild(String packageName) {
        return """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'com.microsoft.azure.functions:azure-functions-java-library:3.1.0'
                    implementation 'org.graalvm.polyglot:polyglot:25.0.3'
                    implementation 'org.graalvm.polyglot:js:25.0.3'
                }

                java {
                    toolchain {
                        languageVersion = JavaLanguageVersion.of(17)
                    }
                }
                """;
    }

    private String awsHandler(String packageName, String className) {
        return """
                package %s;

                import java.io.InputStreamReader;
                import java.nio.charset.StandardCharsets;
                import java.util.Map;
                import java.util.stream.Collectors;

                import com.amazonaws.services.lambda.runtime.Context;
                import com.amazonaws.services.lambda.runtime.RequestHandler;
                import org.graalvm.polyglot.Value;

                public class %s implements RequestHandler<Map<String, Object>, Object> {
                    @Override
                    public Object handleRequest(Map<String, Object> event, Context lambdaContext) {
                        String source = new InputStreamReader(
                                getClass().getResourceAsStream("/function.js"),
                                StandardCharsets.UTF_8
                        ).lines().collect(Collectors.joining("\\n"));
                        try (org.graalvm.polyglot.Context context = org.graalvm.polyglot.Context.newBuilder("js")
                                .allowAllAccess(false)
                                .allowIO(false)
                                .build()) {
                            context.getBindings("js").putMember("input", event);
                            Value value = context.eval("js", source);
                            return value.isString() ? value.asString() : value.toString();
                        }
                    }
                }
                """.formatted(packageName, className);
    }

    private String azureHandler(String packageName, String className, String functionName) {
        return """
                package %s;

                import java.io.InputStreamReader;
                import java.nio.charset.StandardCharsets;
                import java.util.Optional;
                import java.util.stream.Collectors;

                import com.microsoft.azure.functions.ExecutionContext;
                import com.microsoft.azure.functions.HttpMethod;
                import com.microsoft.azure.functions.HttpRequestMessage;
                import com.microsoft.azure.functions.HttpResponseMessage;
                import com.microsoft.azure.functions.HttpStatus;
                import com.microsoft.azure.functions.annotation.AuthorizationLevel;
                import com.microsoft.azure.functions.annotation.FunctionName;
                import com.microsoft.azure.functions.annotation.HttpTrigger;
                import org.graalvm.polyglot.Value;

                public class %s {
                    @FunctionName("%s")
                    public HttpResponseMessage run(
                            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION)
                            HttpRequestMessage<Optional<String>> request,
                            ExecutionContext context) {
                        String source = new InputStreamReader(
                                getClass().getResourceAsStream("/function.js"),
                                StandardCharsets.UTF_8
                        ).lines().collect(Collectors.joining("\\n"));
                        try (org.graalvm.polyglot.Context polyglot = org.graalvm.polyglot.Context.newBuilder("js")
                                .allowAllAccess(false)
                                .allowIO(false)
                                .build()) {
                            polyglot.getBindings("js").putMember("input", request.getBody().orElse("{}"));
                            Value value = polyglot.eval("js", source);
                            return request.createResponseBuilder(HttpStatus.OK)
                                    .body(value.isString() ? value.asString() : value.toString())
                                    .build();
                        }
                    }
                }
                """.formatted(packageName, className, safeFunctionName(functionName));
    }

    private String awsSamTemplate(String packageName, String className, String functionName) {
        return """
                AWSTemplateFormatVersion: '2010-09-09'
                Transform: AWS::Serverless-2016-10-31
                Resources:
                  %s:
                    Type: AWS::Serverless::Function
                    Properties:
                      Runtime: java17
                      Handler: %s.%s::handleRequest
                      MemorySize: 512
                      Timeout: 20
                """.formatted(safeLogicalId(functionName), packageName, className);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String normalizeLanguage(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        if (!"js".equals(normalized) && !"javascript".equals(normalized)) {
            throw new IllegalArgumentException("Only GraalJS is enabled in this runtime. Add more GraalVM language artifacts to enable " + language);
        }
        return "js";
    }

    private String safeName(String raw) {
        String cleaned = raw == null ? "function" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        cleaned = cleaned.replaceAll("(^-+|-+$)", "");
        return cleaned.isBlank() ? "function" : cleaned;
    }

    private String safeJavaSegment(String raw) {
        String cleaned = raw == null ? "function" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        cleaned = cleaned.replaceAll("(^_+|_+$)", "");
        if (cleaned.isBlank()) {
            return "function";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            return "fn_" + cleaned;
        }
        return cleaned;
    }

    private String safeFunctionName(String raw) {
        return safeJavaSegment(raw).replace('_', '-');
    }

    private String safeLogicalId(String raw) {
        String cleaned = raw == null ? "Function" : raw.replaceAll("[^A-Za-z0-9]+", " ");
        StringBuilder builder = new StringBuilder();
        for (String part : cleaned.trim().split("\\s+")) {
            if (!part.isBlank()) {
                builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        if (builder.length() == 0) {
            return "Function";
        }
        if (Character.isDigit(builder.charAt(0))) {
            builder.insert(0, "Fn");
        }
        return builder.toString();
    }

    private Object toJavaValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.fitsInLong() ? value.asLong() : value.asDouble();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.hasArrayElements()) {
            java.util.ArrayList<Object> result = new java.util.ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                result.add(toJavaValue(value.getArrayElement(i)));
            }
            return result;
        }
        if (value.hasMembers()) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                result.put(key, toJavaValue(value.getMember(key)));
            }
            return result;
        }
        return value.toString();
    }
}

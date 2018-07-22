package uk.co.compendiumdev.thingifier.application;

import org.json.JSONObject;
import org.json.XML;
import spark.Request;
import spark.Response;
import uk.co.compendiumdev.thingifier.Thingifier;
import uk.co.compendiumdev.thingifier.api.response.ApiResponse;
import uk.co.compendiumdev.thingifier.api.response.ApiResponseError;
import uk.co.compendiumdev.thingifier.api.routings.ApiRoutingDefinition;
import uk.co.compendiumdev.thingifier.api.routings.ApiRoutingDefinitionGenerator;
import uk.co.compendiumdev.thingifier.api.routings.RoutingDefinition;
import uk.co.compendiumdev.thingifier.reporting.ThingReporter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static spark.Spark.*;

public class ThingifierRestServer {


    // todo : we should be able to configure the API routing for authorisation and support logging


    public ThingifierRestServer(final String[] args, final String path, final Thingifier thingifier) {

        ThingifierHttpApiBridge apiBridge = new ThingifierHttpApiBridge(thingifier);

        before((request, response) -> {

            // TODO: wrap this in a --verbose option
            try {
                System.out.println("**REQUEST**");
                System.out.println(request.url());
                System.out.println(request.pathInfo());
                System.out.println(request.body());
            } catch (Exception e) {
                System.out.println(e);
            }


            // TODO: wrap this in a --verbose option
            System.out.println("**PROCESSING**");
        });

        after((request, response) -> {
            // TODO: wrap this in a --verbose option
            try {
                System.out.println("**RESPONSE**");
                System.out.println(response.status());
                System.out.println(response.body());
            } catch (Exception e) {
                System.out.println(e);
            }
        });

        // TODO : this now needs HTTP level automated coverage

        // configure it based on a thingifier
        ApiRoutingDefinition routingDefinitions = new ApiRoutingDefinitionGenerator(thingifier).generate();


        // / - default for documentation
        get("/", (request, response) -> {
            response.type("text/html");
            response.status(200);
            return new ThingReporter(thingifier).getApiDocumentation(routingDefinitions);
        });


        for (RoutingDefinition defn : routingDefinitions.definitions()) {
            switch (defn.verb()) {
                case GET:
                    if (defn.status().isReturnedFromCall()) {
                        get(defn.url(), (request, response) -> {
                            return apiBridge.get(request, response);
                        });
                    }
                    break;
                case POST:
                    if (defn.status().isReturnedFromCall()) {
                        post(defn.url(), (request, response) -> {
                            return apiBridge.post(request, response);
                            //return apiResponse.getBody();
                        });
                    }
                    break;
                case HEAD:
                    if (!defn.status().isReturnedFromCall()) {
                        head(defn.url(), (request, response) -> {
                            response.status(defn.status().value());
                            return "";
                        });
                    }
                    break;
                case DELETE:
                    if (!defn.status().isReturnedFromCall()) {
                        delete(defn.url(), (request, response) -> {
                            response.status(defn.status().value());
                            return "";
                        });
                    } else {
                        delete(defn.url(), (request, response) -> {
                            return apiBridge.delete(request, response);

                            //return apiResponse.getBody();
                        });
                    }
                    break;
                case PATCH:
                    if (!defn.status().isReturnedFromCall()) {
                        patch(defn.url(), (request, response) -> {
                            response.status(defn.status().value());
                            return "";
                        });
                    }
                    break;
                case PUT:
                    if (!defn.status().isReturnedFromCall()) {
                        put(defn.url(), (request, response) -> {
                            response.status(defn.status().value());
                            return "";
                        });
                    } else {
                        put(defn.url(), (request, response) -> {
                            return apiBridge.put(request, response);
                            //return apiResponse.getBody();
                        });
                    }
                    break;
                case OPTIONS:
                    if (!defn.status().isReturnedFromCall()) {
                        options(defn.url(), (request, response) -> {
                            response.status(defn.status().value());
                            response.header(defn.header(), defn.headerValue());
                            return "";
                        });
                    }
                    break;
            }
        }

        // Undocumented admin interface - this needs to be authentication controlled and toggelable from command line
        get("/admin/query/*", (request, response) -> {
            return apiBridge.query(request, response, request.splat()[0]);
        });

        // TODO : allow this to be overwritten by config
        // nothing else is supported
        head("*", (request, response) -> {
            response.status(404);
            return "";
        });
        get("*", (request, response) -> {
            response.status(404);
            return "";
        });
        options("*", (request, response) -> {
            response.status(404);
            return "";
        });
        put("*", (request, response) -> {
            response.status(404);
            return "";
        });
        post("*", (request, response) -> {
            response.status(404);
            return "";
        });
        patch("*", (request, response) -> {
            response.status(404);
            return "";
        });
        delete("*", (request, response) -> {
            response.status(404);
            return "";
        });

        exception(RuntimeException.class, (e, request, response) -> {
            response.status(400);
            response.body(ApiResponseError.asAppropriate(request.headers("Accept"), e.getMessage()));
        });

        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            response.body(ApiResponseError.asAppropriate(request.headers("Accept"), e.getMessage()));
        });

    }

}

package uk.co.compendiumdev.thingifier.htmlgui;

import com.google.gson.GsonBuilder;
import spark.Request;
import uk.co.compendiumdev.thingifier.api.http.bodyparser.xml.GenericXMLPrettyPrinter;
import uk.co.compendiumdev.thingifier.core.EntityRelModel;
import uk.co.compendiumdev.thingifier.core.domain.instances.EntityInstanceCollection;
import uk.co.compendiumdev.thingifier.Thingifier;
import uk.co.compendiumdev.thingifier.apiconfig.ThingifierApiConfig;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.FieldType;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.Field;
import uk.co.compendiumdev.thingifier.core.domain.definitions.relationship.RelationshipVectorDefinition;
import uk.co.compendiumdev.thingifier.core.domain.definitions.EntityDefinition;
import uk.co.compendiumdev.thingifier.core.domain.instances.EntityInstance;
import uk.co.compendiumdev.thingifier.api.ermodelconversion.JsonThing;
import uk.co.compendiumdev.thingifier.api.ermodelconversion.XmlThing;

import java.util.Collection;

import static spark.Spark.get;

public class DefaultGUI {

    // TODO: templates or tidier way to create the default GUI pages with styling
    // todo: support filters in the GUI Urls



    private final Thingifier thingifier;
    private final JsonThing jsonThing;
    private final DefaultGUIHTML templates;
    private final XmlThing xmlThing;
    private final ThingifierApiConfig apiConfig;
    private final GenericXMLPrettyPrinter XMLPrettyPrinter;

    public DefaultGUI(final Thingifier thingifier, DefaultGUIHTML defaultGui) {
        this.thingifier=thingifier;
        this.apiConfig = thingifier.apiConfig();
        this.jsonThing = new JsonThing(apiConfig.jsonOutput());
        this.xmlThing = new XmlThing(jsonThing);
        this.templates = defaultGui;
        this.XMLPrettyPrinter = new GenericXMLPrettyPrinter();
    }

    // TODO: we should support a top level path e.g. apichallenges, simpleapi which goes before /gui because we should support multiple GUI views with multiple thingifiers
    public DefaultGUI configureRoutes(){

        String tryDefault = " [<a href='/gui/instances?entity=todo&database=__default'>explore default data</a>]";

        // TODO: separate the HTML rendering from the HTTP routing with a class e.g. DefaultEntitesExplorerHTML
        get("/gui", (request, response) -> {
            response.type("text/html");
            response.status(200);
            StringBuilder html = new StringBuilder();
            html.append(templates.getPageStart("GUI", "", "/gui"));
            html.append(templates.getMenuAsHTML());

            html.append(templates.getStartOfMainContentMarker());
            // allow an app level configuration html here
            html.append(templates.getHomePageContent());

            html.append(templates.getEndOfMainContentMarker());
            html.append(templates.getPageFooter());
            html.append(templates.getPageEnd());
            return html.toString();
        });


        // TODO: move out the append menu item from the routes into a different class
        templates.appendMenuItem("Home", "/gui");

        // This is not an API call - don't show it in API documentation
//        publicRoutes.add(new RoutingDefinition(
//                                RoutingVerb.GET,
//                                "/gui",
//                                RoutingStatus.returnedFromCall(),
//                                null
//                            ).addDocumentation("Show the Default GUI"));

        templates.appendMenuItem("Entities Explorer", "/gui/entities");

        get("/gui/entities", (request, response) -> {
            response.type("text/html");
            response.status(200);

            String database = getDatabaseNameFromRequest(request);
            // by default the GUI does not set the cookie, the 'app' does that
            //response.cookie("X-THINGIFIER-DATABASE-NAME", database);

            StringBuilder html = new StringBuilder();
            html.append(templates.getPageStart("Entities Menu",
                    "<meta name='robots' content='noindex'>",
                    "/gui/entities"));

            html.append(templates.getMenuAsHTML());
            html.append("<h1>Entities Explorer</h1>");
            html.append(templates.getStartOfMainContentMarker());
            html.append(getInstancesRootMenuHtml(database));
            //html.append(heading(2, "Choose from the Entities Menu Above"));
            html.append(templates.getEndOfMainContentMarker());
            html.append(templates.getPageFooter());
            html.append(templates.getPageEnd());
            return html.toString();
        });

        get("/gui/instances", (request, response) -> {
            response.type("text/html");
            response.status(200);
            StringBuilder html = new StringBuilder();

            String database = getDatabaseNameFromRequest(request);

            String entityName = request.queryParams("entity");

            html.append(templates.getPageStart(
                    entityName + " Instances",
                    "<meta name='robots' content='noindex'>", "/gui/instances"));

            html.append(templates.getMenuAsHTML());
            html.append(templates.getStartOfMainContentMarker());
            html.append(getInstancesRootMenuHtml(database));



            String htmlErrorMessage = "";

            if(entityName!=null) {

                if (!thingifier.getERmodel().hasEntityNamed(entityName)) {
                    html.append("<h1>Entity Instances Explorer</h1>");
                    htmlErrorMessage = htmlErrorMessage + "<p>Entity Named " + htmlsanitise(entityName) + " not found.</p>";
                }

                if (!thingifier.getERmodel().getDatabaseNames().contains(database)) {
                    html.append("<h1>Entity Instances Explorer</h1>");
                    htmlErrorMessage = htmlErrorMessage + "<p>Database Named " + htmlsanitise(database) + " not found. Have you made any API Calls?" + tryDefault + "</p>";
                }

                EntityInstanceCollection thing = null;

                if (htmlErrorMessage.equals("")) {
                    try {
                        thing = thingifier.getThingInstancesNamed(entityName, database);
                    } catch (Exception e) {
                        //htmlErrorMessage = htmlErrorMessage + "<p>Database Access Error: " + e.getMessage() + ".</p>";
                    }

                    if (thing == null) {
                        htmlErrorMessage = htmlErrorMessage + "<p>Entity instances not found in database, have you made any API calls?" + tryDefault + "</p>";
                    }
                }

                if (htmlErrorMessage.equals("")) {

                    html.append(getExploringDatabaseHtml(database));

                    html.append(heading(1, entityName + " Instances"));

                    try {

                        final EntityDefinition definition = thing.definition();

                        html.append("<h2>" + definition.getPlural() + "</h2>");

                        html.append(startHtmlTableFor(definition));

                        for (EntityInstance instance : thing.getInstances()) {
                            html.append(htmlTableRowFor(instance, database));
                        }

                        html.append("</tbody>");
                        html.append("</table>");
                    } catch (Exception e) {
                        htmlErrorMessage = htmlErrorMessage + "<p>Rendering Error: " + htmlsanitise(e.getMessage()) + "</p>";
                    }
                }
            }

            html.append(htmlErrorMessage);

            html.append(templates.getEndOfMainContentMarker());
            html.append(templates.getPageFooter());
            html.append(templates.getPageEnd());
            return html.toString();
        });

        get("/gui/instance", (request, response) -> {
            response.type("text/html");
            response.status(200);
            StringBuilder html = new StringBuilder();

            String database = getDatabaseNameFromRequest(request);

            String entityName = "";
            for (String queryParam : request.queryParams()) {
                if (queryParam.contentEquals("entity")) {
                    entityName = request.queryParams("entity");
                }
            }

            String htmlErrorMessage = "";

            if(!thingifier.getERmodel().hasEntityNamed(entityName)){
                htmlErrorMessage = htmlErrorMessage + "<p>Entity Named " + htmlsanitise((entityName)) + " not found.</p>";
                entityName = "Unknown";
            }

            if(!thingifier.getERmodel().getDatabaseNames().contains(database)){
                htmlErrorMessage = htmlErrorMessage + "<p>Database Named " + htmlsanitise(database) + " not found. Have you made any API Calls?" + tryDefault + "</p>";
            }

            html.append(templates.getPageStart(entityName + " Instance",
                    "<meta name='robots' content='noindex'>",
                    "/gui/instances"));


            html.append(templates.getMenuAsHTML());
            html.append(templates.getStartOfMainContentMarker());

            EntityInstanceCollection thing = null;

            if(htmlErrorMessage.equals("")){
                try{
                    thing = thingifier.getThingInstancesNamed(entityName, database);
                }catch(Exception e){
                    //htmlErrorMessage = htmlErrorMessage + "<p>Database Access Error: " + e.getMessage() + ".</p>";
                }

                if(thing == null){
                    htmlErrorMessage = htmlErrorMessage + "<p>Entity instances not found in database, have you made any API calls?" + tryDefault + "</p>";
                }
            }

            EntityInstance instance = null;

            if(htmlErrorMessage.equals("")) {

                String keyName = "";
                String keyValue = "";
                for (String queryParam : request.queryParams()) {
                    Field field = thing.definition().getField(queryParam);
                    if (field != null) {
                        if (field.getType() == FieldType.AUTO_GUID || field.getType() == FieldType.AUTO_INCREMENT) {
                            keyName = field.getName();
                            keyValue = request.queryParams(queryParam);
                            break;
                        }
                    }
                }

                try {
                    instance = thing.findInstanceByFieldNameAndValue(keyName, keyValue);
                }catch(Exception e){
                    htmlErrorMessage = htmlErrorMessage + "<p>Instances not found in database, have you made any API calls?" + tryDefault + "</p>";
                }

                if (instance == null) {
                    htmlErrorMessage = htmlErrorMessage +
                            String.format("<p>Could not find instance with %s, %s",htmlsanitise(keyName), htmlsanitise(keyValue));
                }
            }

            if(htmlErrorMessage.equals("")) {

                html.append(getExploringDatabaseHtml(database));

                try {
                    final EntityDefinition definition = thing.definition();

                    html.append(getInstancesRootMenuHtml(database));

                    html.append(heading(1, entityName + " Instance"));

                    html.append("<h2>" + definition.getName() + "</h2>");

                    html.append(getInstanceAsTable(instance));

                    html.append("<details style='padding:1em'><summary>As List</summary>");
                    html.append(getInstanceAsUl(instance));
                    html.append("</details>");


                    if (instance.getRelationships().hasAnyRelationshipInstances()) {

                        html.append("<h2>Relationships</h2>");

                        for (RelationshipVectorDefinition relationship : definition.related().getRelationships()) {
                            final Collection<EntityInstance> relatedItems = instance.getRelationships().getConnectedItems(relationship.getName());
                            html.append("<h3>" + relationship.getName() + "</h3>");
                            if (relatedItems.size() > 0) {
                                boolean header = true;

                                for (EntityInstance relatedInstance : relatedItems) {
                                    if (header) {
                                        html.append(startHtmlTableFor(relatedInstance.getEntity()));
                                        header = false;
                                    }
                                    html.append(htmlTableRowFor(relatedInstance, database));

                                }
                                html.append("</tbody>");
                                html.append("</table>");

                            } else {
                                html.append("<ul><li>none</li></ul>");
                            }
                        }
                    }

                    if (thingifier.apiConfig().willApiAllowJsonForResponses()) {
                        html.append("<h2>JSON Example</h2>");
                        html.append("<pre class='json'>");
                        html.append("<code class='json'>");
                        // pretty print the json
                        html.append(new GsonBuilder().setPrettyPrinting()
                                .create().toJson(jsonThing.asJsonObject(instance)));
                        html.append("</code>");
                        html.append("</pre>");
                    }

                    if (thingifier.apiConfig().willApiAllowXmlForResponses()) {
                        html.append("<h2>XML Example</h2>");
                        html.append("<pre class='xml'>");
                        html.append("<code class='xml'>");
                        // pretty print the json
                        html.append(this.XMLPrettyPrinter.prettyPrintHtml(xmlThing.getSingleObjectXml(instance)));
                        html.append("</code>");
                        html.append("</pre>");
                    }
                }catch (Exception e){
                    htmlErrorMessage = htmlErrorMessage + "<p>Error rendering instance details: " + htmlsanitise(e.getMessage()) + "</p>";
                }
            }

            html.append(htmlErrorMessage);

            html.append(templates.getEndOfMainContentMarker());
            html.append(templates.getPageFooter());
            html.append(templates.getPageEnd());
            return html.toString();
        });

        return this;
    }

    private String getExploringDatabaseHtml(String database) {
        return String.format("<p>Exploring Entities in %s session database.</p>",htmlsanitise(database));
    }

    // TODO: multiple thingifiers would require different cookie names - give Thingifier a name and include in cookie
    // e.g. X-APICHALLENGES-THINGIFIER-DATABASE-NAME, X-SIMPLEAPI-THINGIFIER-DATABASE-NAME
    private String getDatabaseNameFromRequest(Request request) {

        String xdatabasename = "";

        if(request.cookie("X-THINGIFIER-DATABASE-NAME")!=null){
            xdatabasename=request.cookie("X-THINGIFIER-DATABASE-NAME");
        }

        if(request.queryParams("database")!=null){
            xdatabasename = request.queryParams("database");
        }

        if(xdatabasename==""){
            xdatabasename = EntityRelModel.DEFAULT_DATABASE_NAME;
        }

        return xdatabasename;
    }

    private String getInstanceAsUl(final EntityInstance instance) {
        final EntityDefinition definition = instance.getEntity();
        StringBuilder html = new StringBuilder();
        html.append("<ul>");
        for(String field : definition.getFieldNames()) {
                html.append(String.format("<li>%s<ul><li>%s</li></ul></li>",
                        field, htmlsanitise(instance.getFieldValue(field).asString())));
        }
        html.append("</ul>");
        return html.toString();
    }

    private String htmlsanitise(final String value) {

        if(value==null) return "null";

        // todo - add a appconfig to allow XSS vulnerabilities in the GUI

        return value.replace("&","&amp;").
                     replace("<", "&lt;").
                     replace(">", "&gt;").
                     replace(" ", "&nbsp;");
    }

    private String getInstanceAsTable(EntityInstance instance) {

        final EntityDefinition definition = instance.getEntity();

        StringBuilder html = new StringBuilder();
        html.append("<p id='instancetabledescription'>All the fields and values for the %s instance are shown in the table below.<p>".formatted(instance.getEntity().getName()));
        html.append("<table  aria-label='%s Instance Details' aria-describedby='instancetabledescription'>".formatted(instance.getEntity().getName().toUpperCase()));
        html.append("<thead><tr>");
        for(String fieldName : definition.getFieldNames()) {
            Field field = definition.getField(fieldName);
            if(field.getType()==FieldType.AUTO_GUID){
                if(apiConfig.willResponsesShowPrimaryKeyHeader()) {
                    html.append(String.format("<th>%s</th>",field.getName()));
                }
            }else{
                html.append(String.format("<th>%s</th>",field.getName()));
            }
        }
        html.append("</tr></thead>");
        html.append("<tbody><tr>");
        for(String fieldName : definition.getFieldNames()) {
            Field field = definition.getField(fieldName);
            if(field.getType()==FieldType.AUTO_GUID){
                if(apiConfig.willResponsesShowPrimaryKeyHeader()) {
                    html.append(String.format("<td>%s</td>", instance.getFieldValue(fieldName).asString()));
                }
            }else{
                html.append(String.format("<td>%s</td>",htmlsanitise(instance.getFieldValue(fieldName).asString())));
            }
        }

        html.append("</tr></tbody>");
        html.append("</table>");
        return html.toString();
    }

    private String getInstancesRootMenuHtml(String database) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='entity-instances-menu menu'>");
        html.append("<ul>");
        for(String thing : thingifier.getThingNames()){
            html.append(String.format("<li><a href='/gui/instances?entity=%1$s%2$s'>%1$s</a></li>",thing, databaseParam(database)));
        }
        html.append("</ul>");
        html.append("</div>");
        return html.toString();
    }

    private String databaseParam(final String database){
        return "&database="+database;
    }

    private String htmlTableRowFor(final EntityInstance instance, String database) {
        StringBuilder html = new StringBuilder();
        final EntityDefinition definition = instance.getEntity();

        html.append("<tr>");
        // show keys first
        if(definition.hasPrimaryKeyField()) {
            html.append(String.format("<td><a href='/gui/instance?entity=%1$s&%2$s=%3$s%4$s'>%3$s</a></td>",
                    definition.getName(), definition.getPrimaryKeyField().getName(), instance.getPrimaryKeyValue(), databaseParam(database)));
        }

        // show any clickable id fields
        for(String field : definition.getFieldNames()) {
            Field theField = definition.getField(field);
            if(theField!= definition.getPrimaryKeyField()){
                if(theField.getType()== FieldType.AUTO_INCREMENT || theField.getType()==FieldType.AUTO_GUID) {
                    // make ids clickable
                    String renderAs = String.format("<a href='/gui/instance?entity=%1$s&%2$s=%3$s%4$s'>%3$s</a>",
                            definition.getName(),
                            theField.getName(),
                            instance.getFieldValue(field).asString(),
                            databaseParam(database)
                    );
                    html.append(String.format("<td>%s</td>", renderAs));
                }
            }
        }

        // show any normal fields
        for(String field : definition.getFieldNames()) {
            Field theField = definition.getField(field);
            if(theField!= definition.getPrimaryKeyField() && theField.getType()!= FieldType.AUTO_INCREMENT && theField.getType()!=FieldType.AUTO_GUID){
                html.append(String.format("<td>%s</td>", htmlsanitise(instance.getFieldValue(field).asString())));
            }
        }
        html.append("</tr>");

        return html.toString();
    }

    private String startHtmlTableFor(final EntityDefinition definition) {
        StringBuilder html = new StringBuilder();


        html.append("<p id='%1$sentitytabledescription'>All instances for the %1$s entity are shown in the table below.<p>".formatted(definition.getPlural()));
        html.append("<table  aria-label='%1$s Instance Details' aria-describedby='%1$sentitytabledescription'>".formatted(definition.getPlural()));
        html.append("<thead>");
        html.append("<tr>");
        // guid first
        if(definition.hasPrimaryKeyField()) {
            html.append(String.format("<th>%s</th>", definition.getPrimaryKeyField().getName()));
        }
        // then any ids
        for(String field : definition.getFieldNames()) {
            Field theField = definition.getField(field);
            if (theField!=definition.getPrimaryKeyField()){
                if (theField.getType()==FieldType.AUTO_INCREMENT || theField.getType()==FieldType.AUTO_GUID){
                    html.append(String.format("<th>%s</th>", field));
                }
            }
        }
        // then the normal fields
        for(String field : definition.getFieldNames()) {
            Field theField = definition.getField(field);
            if (theField!=definition.getPrimaryKeyField() && theField.getType()!=FieldType.AUTO_INCREMENT && theField.getType()!=FieldType.AUTO_GUID) {
                html.append(String.format("<th>%s</th>", field));
            }
        }
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        return html.toString();
    }

    private String heading(final int level, final String text) {
        return String.format("<h%1$d>%2$s</h%1$d>%n", level, text);
    }
}



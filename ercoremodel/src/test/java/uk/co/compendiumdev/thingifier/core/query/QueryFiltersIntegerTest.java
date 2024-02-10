package uk.co.compendiumdev.thingifier.core.query;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.co.compendiumdev.thingifier.core.EntityRelModel;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.Field;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.FieldType;
import uk.co.compendiumdev.thingifier.core.domain.instances.EntityInstance;
import uk.co.compendiumdev.thingifier.core.domain.instances.EntityInstanceCollection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryFiltersIntegerTest {

    // todo: advanced filtering i.e. < > partial text match, etc.
    // e.g https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/
    // https://softwareengineering.stackexchange.com/questions/233164/how-do-searches-fit-into-a-restful-interface

    EntityRelModel erModel;

    @BeforeEach
    public void setupCollectionTestData(){
        erModel = new EntityRelModel();
        erModel.createEntityDefinition("thing", "things")
                .addFields(
                            Field.is("int", FieldType.INTEGER)
                );

        EntityInstanceCollection thing = erModel.getInstanceData().getInstanceCollectionForEntityNamed("thing");

        thing.createManagedInstance().setValue("int", "3");
        thing.createManagedInstance().setValue("int", "1");
        thing.createManagedInstance().setValue("int", "4");
        thing.createManagedInstance().setValue("int", "2");

    }

    @Test
    public void canFilterIntegerEquals() {

        Map<String, String> params = new HashMap<>();
        params.put("int", "1");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").performQuery(params);
        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(1, instances.size(), "expected 1 value");
        Assertions.assertEquals(1, instances.get(0).getFieldValue("int").asInteger());
    }

    @Test
    public void canFilterIntegerNotEquals() {

        Map<String, String> params = new HashMap<>();
        params.put("int", "!1");
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").performQuery(params);
        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(3, instances.size(), "expected 3 value");
        Assertions.assertEquals(2, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(3, instances.get(1).getFieldValue("int").asInteger());
        Assertions.assertEquals(4, instances.get(2).getFieldValue("int").asInteger());
    }

    // Allow params to be supplied as a map for multiple filters
    @Disabled("todo: because params are supplied as a map so we don't receive multiple filters")
    @Test
    public void canFilterIntegerCombinationOfConditions() {

        // TODO: handle multiple conditions
        Map<String, String> params = new HashMap<>();
        params.put("int", ">1");   // greater than 1
        params.put("int", "!3");    // but not equal to 3
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").performQuery(params);
        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(2, instances.size(), "expected 2 value");
        Assertions.assertEquals(2, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(4, instances.get(1).getFieldValue("int").asInteger());
    }

    @Test
    public void canFilterIntegerGreaterThan() {
        Map<String, String> params = new HashMap<>();
        params.put("int", ">1");
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(3, instances.size(), "expected 3 values");
        Assertions.assertEquals(2, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(3, instances.get(1).getFieldValue("int").asInteger());
        Assertions.assertEquals(4, instances.get(2).getFieldValue("int").asInteger());
    }

    @Test
    public void canFilterIntegerLessThan() {
        Map<String, String> params = new HashMap<>();
        params.put("int", "<2");
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(1, instances.size(), "expected 1 values");
        Assertions.assertEquals(1, instances.get(0).getFieldValue("int").asInteger());
    }

    @Test
    public void canFilterIntegerLessThanNotMatching() {
        Map<String, String> params = new HashMap<>();
        params.put("int", "<1");
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(0, instances.size(), "expected 0 values");
    }

    @Test
    public void canFilterIntegerGreaterThanEquals(){
        Map<String, String> params = new HashMap<>();
        params.put("int", ">=3");
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").
                performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(2, instances.size(), "expected 2 values");
        Assertions.assertEquals(3, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(4, instances.get(1).getFieldValue("int").asInteger());
    }

    @Test
    public void canFilterIntegerLessThanEquals(){
        Map<String, String> params = new HashMap<>();
        params.put("int", "<=3");
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").
                performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(3, instances.size(), "expected 3 values");
        Assertions.assertEquals(1, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(2, instances.get(1).getFieldValue("int").asInteger());
        Assertions.assertEquals(3, instances.get(2).getFieldValue("int").asInteger());
    }

    @Test
    public void canFilterIntegerLessThanSortedDesc(){
        Map<String, String> params = new HashMap<>();
        params.put("int", "<3");
        params.put("sortby", "-int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").
                performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(2, instances.size(), "expected 3 values");
        Assertions.assertEquals(2, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(1, instances.get(1).getFieldValue("int").asInteger());
    }


    @Test
    public void canIntegerSortedDesc(){
        Map<String, String> params = new HashMap<>();
        params.put("sortby", "-int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").
                performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(4, instances.size(), "expected 3 values");
        Assertions.assertEquals(4, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(3, instances.get(1).getFieldValue("int").asInteger());
        Assertions.assertEquals(2, instances.get(2).getFieldValue("int").asInteger());
        Assertions.assertEquals(1, instances.get(3).getFieldValue("int").asInteger());
    }

    @Test
    public void canIntegerSortedAsc(){
        Map<String, String> params = new HashMap<>();
        params.put("sortby", "+int");

        SimpleQuery queryResults = new SimpleQuery(erModel.getSchema(), erModel.getInstanceData(), "things").
                performQuery(params);

        Assertions.assertTrue(queryResults.isResultACollection(), "result should be a collection");
        List<EntityInstance> instances = queryResults.getListEntityInstances();
        Assertions.assertEquals(4, instances.size(), "expected 3 values");
        Assertions.assertEquals(1, instances.get(0).getFieldValue("int").asInteger());
        Assertions.assertEquals(2, instances.get(1).getFieldValue("int").asInteger());
        Assertions.assertEquals(3, instances.get(2).getFieldValue("int").asInteger());
        Assertions.assertEquals(4, instances.get(3).getFieldValue("int").asInteger());
    }


}
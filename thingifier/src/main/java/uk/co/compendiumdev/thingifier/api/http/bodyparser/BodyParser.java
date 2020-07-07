package uk.co.compendiumdev.thingifier.api.http.bodyparser;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.json.JSONObject;
import org.json.XML;
import uk.co.compendiumdev.thingifier.api.http.HttpApiRequest;

import java.util.*;

public class BodyParser {

    private final HttpApiRequest request;
    private final List<String> thingNames;
    private Map<String, Object> args = null;

    public BodyParser(final HttpApiRequest aGivenRequest, final List<String> thingNames) {
        this.request = aGivenRequest;
        this.thingNames = thingNames;
    }


    /**
     * getStringMap returns the top level values as a map
     */
    public Map<String, String> getStringMap() {
        return stringMap(getMap());
    }


    private Map<String, String> stringMap(final Map<String, Object> args) {
        Map<String, String> stringsInMap = new HashMap();
        for (String key : args.keySet()) {
            Object theValue = args.get(key);

            if (theValue instanceof String ) {
                stringsInMap.put(key, (String) theValue);
            }
            if(theValue instanceof Double){
                stringsInMap.put(key, String.valueOf(theValue));
            }
        }
        return stringsInMap;
    }

    // since complex keys can be duplicated,
    // we can't use a hashmap, so we are using a list of map entries
    // the map entries could be a custom Key Value Pair implementation if we wanted
    public List<Map.Entry<String,String>> getFlattenedStringMap() {
        List<Map.Entry<String,String>> stringsInMap = flattenToStringMap("", getMap());
        return stringsInMap;
    }


    private List<Map.Entry<String,String>> flattenToStringMap(final String prefixkey, final Object theValue) {
        List<Map.Entry<String,String>> stringsInMap = new ArrayList<>();
        if (theValue instanceof String ) {
            stringsInMap.add(new AbstractMap.SimpleEntry<String,String>(prefixkey, (String)theValue));
        }
        if(theValue instanceof Double){
            stringsInMap.add(new AbstractMap.SimpleEntry<String,String>(prefixkey, (String)theValue));
        }
        String separator = "";
        if(prefixkey!=null && prefixkey.length() > 0 && !prefixkey.endsWith(".")){
            separator = ".";
        }
        if(theValue instanceof Map){
            for (Map.Entry<String,Object> entry : ((Map<String,Object>)theValue).entrySet()) {
                String key = entry.getKey();
                Object aValue = entry.getValue();
                List<Map.Entry<String,String>> nestedValues = flattenToStringMap(prefixkey + separator + key, aValue);
                stringsInMap.addAll(nestedValues);
            }
        }
        if(theValue instanceof ArrayList) {
            for(Object aValue : (ArrayList)theValue){
                List<Map.Entry<String,String>> nestedValues = flattenToStringMap(prefixkey + separator, aValue);
                stringsInMap.addAll(nestedValues);
            }
        }
        return stringsInMap;
    }

    public List<String> getObjectNames(){
        List<String> objectOrCollectionNames = new ArrayList();
        for (String key : args.keySet()) {
            if (!(args.get(key) instanceof String || args.get(key) instanceof Double)) {
                objectOrCollectionNames.add(key);
            }
        }
        return objectOrCollectionNames;
    }

    public Map<String, Object> getMap() {

        parseMap();

        return args;
    }

    /**
     * Only parse it once and then cache the converted map
     */
    private void parseMap() {

        if(args!=null)
            return;

        if(request.getBody().trim().isEmpty()){
            args = new HashMap<>();
            return;
        }

        // because we are using crude XML and JSON parsing
        // <project><title>My posted to do on the project</title></project>
        // would become {"project":{"title":"My posted to do on the project"}}
        // when we want {"title":"My posted to do on the project"}
        // this is just a quick hack to amend it to support XML
        // TODO: try to change this in the future to make it more robust, perhaps the API shouldn't take a String as the body, it should take a parsed class?
        // TODO: BUG - since we remove the wrapper we might send in a POST <project><title>My posted to do on the project</title></project> to /todo and it will work fine if the fields are the same
        if (request.getHeader("Content-Type") != null && request.getHeader("Content-Type").endsWith("/xml")) {

            // PROTOTYPE XML Conversion
            System.out.println(request.getBody());
            System.out.println(XML.toJSONObject(request.getBody()).toString());
            JSONObject conv = XML.toJSONObject(request.getBody());
            if (conv.keySet().size() == 1) {
                // if the key is an entity type then we just want the body
                ArrayList<String> keys = new ArrayList<String>(conv.keySet());

                if (thingNames.contains(keys.get(0))) {
                    // just the body
                    String justTheBody = conv.get(keys.get(0)).toString();
                    System.out.println(justTheBody);
                    args = new Gson().fromJson(justTheBody, Map.class);
                    return;
                }

            }
        }

        args = new Gson().fromJson(request.getBody(), Map.class);
    }


}
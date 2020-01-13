package uk.ac.ox.cs.pdq.rest;

import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ox.cs.pdq.algebra.RelationalTerm;
import uk.ac.ox.cs.pdq.rest.jsonobjects.schema.*;
import uk.ac.ox.cs.pdq.rest.util.*;
import uk.ac.ox.cs.pdq.rest.jsonobjects.plan.JsonPlan;
import uk.ac.ox.cs.pdq.rest.jsonobjects.run.JsonRunResults;
import uk.ac.ox.cs.pdq.ui.io.sql.SQLLikeQueryWriter;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.io.jaxb.IOManager;
import org.springframework.core.io.Resource;

import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

import org.springframework.http.HttpHeaders;

import java.nio.file.Paths;

/**
 * Entry point for Rest application. JsonController defines and computes all the REST calls of the API.
 *
 * @author Camilo Ortiz
 */

@RestController
public class JsonController {
    private HashMap<Integer, String> paths;

    private HashMap<Integer, Schema> schemaList;
    private HashMap<Integer, HashMap<Integer, ConjunctiveQuery>> commonQueries;
    private HashMap<Long, HashMap<Integer, HashMap<Integer, ConjunctiveQuery>>> queryList;
    private HashMap<Integer, File> casePropertyList;
    private HashMap<Integer, HashMap<Integer, JsonPlan>> planList;
    private HashMap<Integer, HashMap<Integer, JsonRunResults>> runResultList;
    private HashMap<Integer, String> catalogPaths;
    private boolean localMode; //change to config file

    public JsonController() {
        this.paths = new HashMap<Integer, String>();
        this.schemaList = new HashMap<Integer, Schema>();
        this.commonQueries = new HashMap<Integer, HashMap<Integer, ConjunctiveQuery>>();
        this.casePropertyList = new HashMap<Integer, File>();


        this.queryList = new HashMap<Long, HashMap<Integer, HashMap<Integer, ConjunctiveQuery>>>();
        this.planList = new HashMap<Integer, HashMap<Integer, JsonPlan>>();
        this.runResultList = new HashMap<Integer, HashMap<Integer, JsonRunResults>>();
        this.catalogPaths = new HashMap<Integer, String>();
        this.localMode = false;

        File testDirectory = new File("demo/");
        File[] examples = testDirectory.listFiles();

        if (examples != null) {
            int i = 0;
            for (File folder : examples) {
                if (folder.isDirectory()) {
                    paths.put(i, folder.getPath());
                    File[] info = folder.listFiles();

                    for (File file : info) {
                        switch (file.getName()) {
                            case "schema.xml":
                                try {

                                    Schema schema = IOManager.importSchema(file);
                                    schemaList.put(i, schema);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case "queries":
                                File[] queries = file.listFiles();
                                HashMap<Integer, ConjunctiveQuery> queryHashMap = new HashMap<Integer, ConjunctiveQuery>();
                                int j = 0;
                                for (File query : queries) {
                                    try {

                                        ConjunctiveQuery CQs = IOManager.importQuery(query);
                                        queryHashMap.put(j, CQs);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    j++;
                                }

                                commonQueries.put(i, queryHashMap);

                                break;

                            case "case.properties":
                                casePropertyList.put(i, file);
                                break;

                            case "catalog.properties":
                                String pathToCatalog = file.getPath();
                                catalogPaths.put(i, pathToCatalog);
                                break;

                            default:
                                break;
                        }

                    }
                    i++;
                }

            }
        }
    }

    /**
     * Returns an initial list of schemas that only contains their id and names.
     *
     * @return SchemaName[]
     */
    @RequestMapping(value = "/initSchemas", method = RequestMethod.GET, produces = "application/json")
    public InitialInfo initSchemas() {

        SchemaName[] jsonSchemaList = new SchemaName[this.schemaList.size()];

        int i = 0;
        for (Integer id : this.schemaList.keySet()) {
            HashMap<Integer, ConjunctiveQuery> CQList = this.commonQueries.get(id);


            ArrayList<JsonQuery> JQList = new ArrayList<JsonQuery>();
            Schema schema = this.schemaList.get(id);

            int n = CQList.size();
            for (int j = 0; j < n; j++) {
                try {

                    String query_string = SQLLikeQueryWriter.convert(CQList.get(j), schema);

                    JsonQuery query = new JsonQuery(j, query_string);

                    JQList.add(query);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            SchemaName jsonSchema = new SchemaName(schema, i, JQList);
            jsonSchemaList[i] = jsonSchema;
            i++;
        }

        return new InitialInfo(jsonSchemaList);
    }

    /**
     * Returns relations associated with specific schema. Schema is identified thanks to the provided id.
     *
     * @param id
     * @return JsonRelationList
     */
    @RequestMapping(value = "/getRelations", method = RequestMethod.GET, produces = "application/json")
    public JsonRelationList getRelations(@RequestParam(value = "id") int id) {

        Schema schema = schemaList.get(id);
        JsonRelationList toReturn = new JsonRelationList(schema, id);

        return toReturn;
    }

    /**
     * Returns dependencies associated with specific schema.
     *
     * @param id
     * @return JsonRelationList
     */
    @RequestMapping(value = "/getDependencies", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<JsonDependencyList> getDependencies(@RequestParam(value = "id") int id) {

        Schema schema = schemaList.get(id);
        JsonDependencyList toReturn = new JsonDependencyList(schema, id);

        String contentType = "application/json";

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(contentType)
                .body(toReturn);
    }

    @GetMapping(value = "/verifyQuery/{schemaID}/{queryID}/{SQL:.+}")
    public boolean verifyQuery(@PathVariable Integer schemaID, @PathVariable Integer queryID, @PathVariable String SQL,
                               HttpServletRequest request) {

        Schema schema = this.schemaList.get(schemaID);

        boolean validQuery = false;

        try {
            SQLQueryReader reader = new SQLQueryReader(schema);
            ConjunctiveQuery newQuery = reader.fromString(SQL);

            //validation goes here
            validQuery = true;

            if (this.localMode) {
                File query = new File(paths.get(schemaID) + "/queries/" + "query" + queryID.toString() + ".xml");
                IOManager.exportQueryToXml(newQuery, query);
                HashMap<Integer, ConjunctiveQuery> updatedList = this.commonQueries.get(schemaID);
                updatedList.put(queryID, newQuery);
            }

            return validQuery;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return validQuery;
    }

    /**
     * Returns Entry<RelationalTerm, Cost> that shows up as a long string
     *
     * @param schemaID
     * @param queryID
     * @param SQL
     * @return
     */
    @GetMapping(value = "/plan/{schemaID}/{queryID}/{SQL}")
    public JsonPlan plan(@PathVariable Integer schemaID, @PathVariable Integer queryID, @PathVariable String SQL) {

        Schema schema = schemaList.get(schemaID);
        File properties = casePropertyList.get(schemaID);
        String pathToCatalog = catalogPaths.get(schemaID);
        ConjunctiveQuery cq = null;
        JsonPlan plan = null;
        try {
            if(!localMode && queryID != 0) {
                SQLQueryReader reader = new SQLQueryReader(schema);
                cq = reader.fromString(SQL);
            }else{
                cq = commonQueries.get(schemaID).get(queryID);
            }

            plan = JsonPlanner.plan(schema, cq, properties, pathToCatalog);

        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }

        if (plan != null) {
            plan.getGraphicalPlan().setType("ORIGIN");
        }

        return plan;
    }

    @GetMapping(value = "/run/{schemaID}/{queryID}/{SQL}")
    public JsonRunResults run(@PathVariable Integer schemaID, @PathVariable Integer queryID, @PathVariable String SQL){


        Schema schema = schemaList.get(schemaID);
        File properties = casePropertyList.get(schemaID);
        String pathToCatalog = catalogPaths.get(schemaID);
        JsonRunResults toReturn = null;

        try {
            ConjunctiveQuery cq = null;
            if (! localMode && queryID != 0){
                SQLQueryReader reader = new SQLQueryReader(schema);
                cq = reader.fromString(SQL);
            }else{
                cq = commonQueries.get(schemaID).get(queryID);
            }

            JsonPlan jsonPlan = JsonPlanner.plan(schema, cq, properties, pathToCatalog);
            RelationalTerm plan = jsonPlan.getPlan();

            toReturn = Runner.runtime(schema, cq, properties, plan);

        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return toReturn;

    }

    /**
     * Write run table to its associated example folder, load it, and send it to the client.
     *
     * @param schemaID
     * @param queryID
     * @param request
     * @return
     */
    @GetMapping(value = "/downloadRun/{schemaID}/{queryID}/{SQL}")
    public ResponseEntity<Resource> downloadRun(@PathVariable int schemaID, @PathVariable int queryID,
                                                @PathVariable String SQL, HttpServletRequest request) {

        try {
            Schema schema = schemaList.get(schemaID);
            File properties = casePropertyList.get(schemaID);
            String pathToCatalog = catalogPaths.get(schemaID);
            ConjunctiveQuery cq = null;

            if (! localMode && queryID != 0){
                SQLQueryReader reader = new SQLQueryReader(schema);
                cq = reader.fromString(SQL);
            }else{
                cq = commonQueries.get(schemaID).get(queryID);
            }

            JsonPlan jsonPlan = JsonPlanner.plan(schema, cq, properties, pathToCatalog);
            RelationalTerm plan = jsonPlan.getPlan();
            JsonRunResults result = Runner.runtime(schema, cq, properties, plan);
            Runner.writeOutput(result.results, paths.get(schemaID) + "/results.csv");

            Resource resource = loadFileAsResource(paths.get(schemaID) + "/results.csv");
            String contentType = "text/csv";

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"results.csv\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

    @GetMapping(value = "/downloadPlan/{schemaID}/{queryID}/{SQL}")
    public ResponseEntity<Resource> downloadPlan(@PathVariable int schemaID, @PathVariable int queryID,
                                                 @PathVariable String SQL, HttpServletRequest request) {

        try {

            Resource resource = loadFileAsResource(paths.get(schemaID) + "/computed-plan.xml");

            String contentType = "application/xml";

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"computed-plan.xml\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = Paths.get(fileName);

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}

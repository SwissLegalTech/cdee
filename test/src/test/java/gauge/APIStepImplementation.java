package gauge;

//static import com.deondigital.cucumber.toAPIClient
//import the helper functions

import com.deondigital.api.client.DeonAPIClient;
import com.deondigital.cucumber.StepdefsKt;
import com.deondigital.cucumber.TestConnection;
import com.thoughtworks.gauge.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class APIStepImplementation {

    private static Map<String, DeonAPIClient> apiClients = new HashMap<>();
    private static Map<String, String> templateIDs = new HashMap<>();
    private static Map<String, String> contractIDs = new HashMap<>();

    public static Map<String, DeonAPIClient> getApiClients() {
        return apiClients;
    }

    public static DeonAPIClient getFirstApiClient() {
        return apiClients.values().iterator().next();
    }

    public static String getContractID(String contractName) {
        String contractID = contractIDs.get(contractName);
        if (contractID == null) {
            throw new RuntimeException("No such contract $contractName");
        }
        return contractID;
    }

    public static String addContractID(String contractName, String contractID){
        return contractIDs.put(contractName, contractID);
    }

    public static Map<String, String> getTemplateIDs(){
        return templateIDs;
    }

    private static String getNameFromFile(String fileName) {
        //extract file name
        int beginning = fileName.lastIndexOf(File.separatorChar);
        beginning = (beginning == -1) ? 0 : beginning + 1;
        return fileName.substring(beginning, fileName.lastIndexOf("."));
    }

    @Step("connect to the peers <table>")
    public static Map<String, DeonAPIClient> connectToThePeers(Table table) {
        apiClients.putAll(
                table.getTableRows().stream().collect(Collectors.<TableRow, String, DeonAPIClient>toMap(
                        row -> row.getCell("name"),
                        row -> StepdefsKt.toAPIClient(new TestConnection(row.getCell("name"), row.getCell("url"), null))
                        )
                )
        );
        return getApiClients();
    }

    @Step("deploy the contract template file <templateFileName>")
    public static void deployContractTemplateFromFile(String templateFileName) throws IOException {
        String csl = new String(Files.readAllBytes(Paths.get(templateFileName)));
        deployContractTemplateFromStringWithName(getNameFromFile(templateFileName), csl);
    }

    @Step("deploy with name <name> the contract template file <templateFileName>")
    public static void deployContractTemplateFromFileWithName(String name, String templateFileName) throws IOException {
        String csl = new String(Files.readAllBytes(Paths.get(templateFileName)));
        deployContractTemplateFromStringWithName(name, csl);
    }

    @Step("deploy with name <name> the contract template <template>")
    public static void deployContractTemplateFromStringWithName(String templateName, String template) throws IOException {
        //the contract template is automatically deployed on all peers
        //TODO: Just in corda?
        String declarationId = getFirstApiClient()
                .addDeclaration(templateName, template);
        templateIDs.put(templateName, declarationId);
    }

    public static DeonAPIClient getApiClient(String clientName) {
        DeonAPIClient client = getApiClients().get(clientName);
        if (client == null) {
            throw new RuntimeException("there is no peer named '" + clientName + "'");
        }
        return client;
    }

    @Step("instantiate the template file <templateFile> as <contractName> on <peers> with entrypoint <entryPoint> with the following arguments <args>")
    public void instatiateContractByTemplateFile(String templateFile, String contractName, String peersList, String entryPoint, Table args) {
        instantiateContractByTemplateName(getNameFromFile(templateFile), contractName, peersList, entryPoint, args);
    }

    @Step("instantiate the template named <templateName> as <contractName> on <peers> with entrypoint <entryPoint> with the following arguments <args>")
    public void instantiateContractByTemplateName(String templateName, String contractName, String peers, String entryPoint, Table args) {
        List<String> peersList = Arrays.stream(peers.split(",")).map(String::trim).collect(Collectors.toList());
        DeonAPIClient client = getApiClient(peersList.get(0));//instanciate the contract from the first peer
        String declarationId = templateIDs.get(templateName);
        if (declarationId == null) {
            throw new RuntimeException("No such template " + templateName);
        }
        List<String> argsList = tableRowToList(args, "args");
        argsList = argsList.stream().map(s -> checkAndConvertToSting(s)).collect(Collectors.toList());
        contractIDs.put(contractName, StepdefsKt.instantiateContract(contractIDs, declarationId, entryPoint, contractName, client, argsList, peersList));
    }

    //input is a JSON String
    //if it is a pimitive string (not a complex type or another primitive)
    //add quotation marks
    //so that /contract/report can convert it properly
    private String checkAndConvertToSting(String s) {
        if (s.matches(".*[\\(\\)\\[\\]\\{\\}\\d]+.*")) return s;
        return "\"" + s + "\"";
    }

    @Step("instantiate the template named <templateName> as <contractName> with entrypoint <entryPoint> and the following args on the following peers <table>")
    public void instantiateContractByTemplateName(String templateName, String contractName, String entryPoint, Table table) {
        List<String> peersList = tableRowToList(table, "peers");
        String peers = String.join(", ", peersList);
        instantiateContractByTemplateName(templateName, contractName, peers, entryPoint, table);
    }

    private static List<String> tableRowToList(Table table, String rowName) {
        List<String> rowList = table.getColumnValues(rowName);
        if (rowList.isEmpty()) {
            throw new RuntimeException("one heading must be called '" + rowName + "'");
        }
        //eliminate empty entries
        rowList.removeIf(String::isEmpty);
        return rowList;
    }

    @Step("add the following events to <contractName> on peer <executingPeer> <events>")
    public void addEvents(String contractName, String executingPeer, Table events) {
        DeonAPIClient client = getApiClient(executingPeer);
        List<String> eventsList = convertToEventsList(events);
        if (eventsList.isEmpty()) throw new RuntimeException("Expected column with heading 'events'");
        StepdefsKt.addEvents(contractName, contractIDs, client, eventsList);
    }

    //gets the events csl and replaces any args in form of '$<arg>'
    private List<String> convertToEventsList(Table events) {
        List<String> eventsList = tableRowToList(events, "events");
        // replacement of args needed? ($<>)
        if (eventsList.stream().anyMatch(s -> s.matches(".*"+StepdefsKt.getRegex().getPattern()+".*"))){
            //build map for replacement
            List<String> argsList = tableRowToList(events, "args");
            Map<String, String> argsMap = new HashMap<>(argsList.size());
            for (int i = 0; i<argsList.size(); i++){
                argsMap.put("arg"+(i+1), argsList.get(i));
            }

            eventsList = eventsList.stream().map(s -> StepdefsKt.replaceVars(s, argsMap)).collect(Collectors.toList());
        }
        return eventsList;
    }

    @Step("pause")
    public void waitReturnKeyPressed() throws IOException, InterruptedException {
        System.out.println("Paused - please press the RETURN key to continue.");
        System.in.read();
        System.out.println("continuing execution");
    }

}



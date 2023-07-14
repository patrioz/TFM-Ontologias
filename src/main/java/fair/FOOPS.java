/*
 * Copyright 2021-22 Ontology Engineering Group, Universidad Politecnica de Madrid, Spain
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * Author: Daniel Garijo and Maria Poveda
 */

package fair;

import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCallerOptions;
import com.github.rcaller.rstuff.RCode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import entities.Check;
import entities.checks.*;
import entities.Ontology;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;




public class FOOPS {
    private static final Logger logger = LoggerFactory.getLogger(FOOPS.class);

    private ArrayList<Check> checks;
    private Path tmpFolder;
    private Ontology ontology;
    private Check ont;
    
    public FOOPS(String o, boolean isFromFile){
        tmpFolder = null;
        try {
            tmpFolder = Files.createTempDirectory(Path.of("."), "foops");
        }catch(Exception e){
            logger.error("Could not create temporary folder. Exiting");
            return;
        }
        this.ontology = new Ontology(o, isFromFile, tmpFolder);
        Check_PURL1_PersistentURIs f1 = new Check_PURL1_PersistentURIs(ontology);
        Check_URI1_URIResolvable uri1 = new Check_URI1_URIResolvable(ontology);
        Check_CN1_ContentNegotiation a1 = new Check_CN1_ContentNegotiation(ontology);
        Check_DOC1_HTMLDoc d1 = new Check_DOC1_HTMLDoc(ontology);
        Check_RDF1_RDFAvailability rdf1 = new Check_RDF1_RDFAvailability(ontology);
        Check_OM1_MinimumMetadata om1 = new Check_OM1_MinimumMetadata(ontology);
        Check_OM2_RecommendedMetadata om2 = new Check_OM2_RecommendedMetadata(ontology);
        Check_OM3_DetailedMetadata om3 = new Check_OM3_DetailedMetadata(ontology);
        Check_OM4_1_License om41 = new Check_OM4_1_License(ontology);
        Check_OM4_2_LicenseIsResolvable om42 = new Check_OM4_2_LicenseIsResolvable(ontology);
        Check_OM5_1_ProvenanceMetadataBasic om51 = new Check_OM5_1_ProvenanceMetadataBasic(ontology);
        Check_OM5_2_ProvenanceMetadataFull om52 = new Check_OM5_2_ProvenanceMetadataFull(ontology);
        Check_FIND1_Prefix find1 = new Check_FIND1_Prefix(ontology);
        Check_FIND2_PrefixInRegistry find2 = new Check_FIND2_PrefixInRegistry(ontology);
        Check_FIND3_FindOntologyInRegistry find3 = new Check_FIND3_FindOntologyInRegistry(ontology);
        Check_FIND3_BIS_MetadataAccessible find3_bis = new Check_FIND3_BIS_MetadataAccessible(ontology,find3);
        Check_HTTP1_AccessProtocol http1 = new Check_HTTP1_AccessProtocol(ontology);
        Check_VOC1_VocabReuseMetadata voc1 = new Check_VOC1_VocabReuseMetadata(ontology);
        Check_VOC2_VocabReuse voc2 = new Check_VOC2_VocabReuse(ontology);
        Check_VOC3_TermMetadataLabel voc3 = new Check_VOC3_TermMetadataLabel(ontology);
        Check_VOC4_TermMetadataDescription voc4 = new Check_VOC4_TermMetadataDescription(ontology);
        Check_VER1_VersionIRI ver1 = new Check_VER1_VersionIRI(ontology);
        Check_VER2_ResolvableVersionIRI ver2 = new Check_VER2_ResolvableVersionIRI(ontology);
        checks = new ArrayList<>();
        checks.add(a1);
        checks.add(f1);
        checks.add(uri1);
        checks.add(d1);
        checks.add(rdf1);
        checks.add(om1); 
        checks.add(om2); 
        checks.add(om3);
        checks.add(om41); 
        checks.add(om42);
        checks.add(om51);
        checks.add(om52);
        checks.add(find1);
        checks.add(find2);
        checks.add(find3);
        checks.add(find3_bis);
        checks.add(http1);
        checks.add(voc1); 
        checks.add(voc2);
        checks.add(voc3);
        checks.add(voc4);
        checks.add(ver1);
        checks.add(ver2);
        //only add this check if ontology was loaded through it URI
        if(!isFromFile){
            Check_URI2_OntologyURIEqualToID uri2 = new Check_URI2_OntologyURIEqualToID(ontology, o);
            checks.add(uri2);
        }
    }


    /**
     * Method for passing all the checks.
     */
    public void fairTest(){
        try {
            checks.forEach(Check::check);
        }catch(Exception e){
            logger.error("Error with check");
        }

    }

    private float getTotalScore(){
        float totalNum = 0;
        for (Check check : checks) {
            totalNum += (float)check.getTotal_passed_tests()/(float)check.getTotal_tests_run();
        }
        //we divide among the number of checks, as some checks do more tests than others but all
        // are equally important
        return totalNum/checks.size();
    }

    /**
     * This method writes the results as a JSON file
     */
    public String exportJSON_CAB(){
        String license, title, uri, score;
        if (this.ontology.getLicense()!=null && !"".equals(ontology.getLicense())){
            license = "\"ontology_license\": \""+this.ontology.getLicense()+"\",\n";
        }else{
            license = "\"ontology_license\": \"unknown\",\n";
        }
        if (this.ontology.getTitle()!=null && !"".equals(ontology.getTitle())){
            title = "\"ontology_title\": \""+this.ontology.getTitle()+"\",\n";
        }else{
            title = "\"ontology_title\": \"unknown\",\n";
        }
    
        
        uri = "{\n\"ontology_URI\": \""+this.ontology.getOntologyURI()+"\",\n";
        score = "\"overall_score\":"+this.getTotalScore()+"}" ;
        String out_cab = uri+ title +  license + score;
        System.out.println(out_cab);
        return out_cab;
    }

    public String exportJSON_DATA(){

        String out_data = "{"  ;      
        Gson gson = new GsonBuilder().
                excludeFieldsWithoutExposeAnnotation().
                setPrettyPrinting().
                create();
      
      String jsonChecks = gson.toJson(checks);
      out_data = jsonChecks ;
        System.out.println(out_data);
        return out_data;
    }
    public void removeTemporaryFolders(){
        try {
            FileUtils.deleteDirectory(new File(this.tmpFolder.toString()));
        }catch(Exception e){
            logger.error("Could not delete tmp folder");
        }
    }
    
 

    public static void main(String[] args) throws IOException {
    	logger.info("\n\n--FAIR ontologies: validation tests--\n");
    	
    	File theDir = new File("salida");
    	if (theDir.exists()) {
    		FileUtils.cleanDirectory(theDir);
    		FileUtils.forceDelete(theDir);
    	}
    		
    	 
    	  theDir.mkdirs();
    	
    	String ontology = "", outPath = "",  outPath_data = "";
    	ArrayList<String> ontList = new ArrayList();
    	File inFile = null;
    	
    	if (0 < args.length) {
    	   inFile = new File(args[0]);
    	} else {
    	   System.err.println("Invalid arguments count:" + args.length);
    	   System.exit(0);
    	}
          BufferedReader br = null;
          try {
              String sCurrentLine;
              br = new BufferedReader(new FileReader(inFile));
              while ((sCurrentLine = br.readLine()) != null) {
            	  ontList.add(sCurrentLine);
                  System.out.println(sCurrentLine);
              }
          } 

          catch (IOException e) {
              e.printStackTrace();
          } 

          finally {
              try {
                  if (br != null) br.close();
              } catch (IOException ex) {
                  ex.printStackTrace();
              }
          }
          
          if(outPath.isEmpty()){
              outPath = Constants.DEFAULT_OUT_PATH_CAB;
              outPath_data = Constants.DEFAULT_OUT_PATH_DAT;
              
          }
          FOOPS f = null;
          try {
          	int j =1;
          	//PrintWriter out = new PrintWriter(outPath);
          	while(j <= ontList.size()) {
          		logger.info("Validating: " + ontList.get(j-1));
          		System.out.print(outPath);
          		PrintWriter out_CAB = new PrintWriter(outPath + j + ".json");
          		PrintWriter out_data = new PrintWriter(outPath_data + j + ".json");
          		f = new FOOPS(ontList.get(j-1), false);
                  f.fairTest();              
                  out_CAB.println(f.exportJSON_CAB());	
                  out_CAB.close();
                  out_data.println(f.exportJSON_DATA());	
                  out_data.close();
                  j++; 
          	}
          	Runtime.getRuntime().exec("Rscript codigo_R.R");

          	
          }catch(Exception e){
              logger.error("Error! "+ e.getMessage());
          }finally{
              if (f != null){
                  f.removeTemporaryFolders();
              }
          }
      }
    
}


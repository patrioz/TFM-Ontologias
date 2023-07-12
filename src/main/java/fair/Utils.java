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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * Method that will download the ontology to document with Widoco.
     *
     * @param isFromFile boolean to indicate whether the ontology is from file or from URI.
     * @throws java.lang.Exception
     */
    public static OWLOntology loadModelToDocument(String pathOrURI,boolean isFromFile, String downloadFolder) throws Exception {
        String ontologyPath = pathOrURI;
        if (!isFromFile) {
            ontologyPath = downloadFolder + File.separator + "ontology";
            downloadOntology(pathOrURI, ontologyPath);
        }
        logger.info("Loading ontology ");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration loadingConfig = new OWLOntologyLoaderConfiguration();
        loadingConfig = loadingConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        logger.info("Parsing type: "+loadingConfig.isStrict());
        return manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(ontologyPath)), loadingConfig);
    }

    /**
     * Method that will download an ontology given its URI, doing content
     * negotiation The ontology will be downloaded in the first serialization
     * available (see Constants.POSSIBLE_VOCAB_SERIALIZATIONS)
     *
     * @param uri the URI of the ontology
     * @param downloadPath path where the ontology will be saved locally.
     */
    public static void downloadOntology(String uri, String downloadPath) {

        for (String serialization : Constants.POSSIBLE_VOCAB_SERIALIZATIONS) {
            logger.info("Attempting to download vocabulary in " + serialization);
            try {
                HttpURLConnection connection = doNegotiation(uri,serialization);
                InputStream in = (InputStream) connection.getInputStream();
                Files.copy(in, Paths.get(downloadPath), StandardCopyOption.REPLACE_EXISTING);
                in.close();
                logger.info("Vocabulary in "+serialization+" downloaded successfully " +
                        "(not downloading other serializations)");
                break;
            } catch (Exception e) {
                final String message = "Failed to download vocabulary in RDF format [" + serialization +"]: ";
                logger.error(message + e.toString());
                throw new RuntimeException(message, e);
            }
        }
    }

    public static String getValueAsLiteralOrURI(OWLAnnotationValue v) {
        try {
            return v.asIRI().get().getIRIString();
        } catch (Exception e) {
            // instead of a resource, it was added as a String
            return v.asLiteral().get().getLiteral();
        }
    }

    public static Document loadOntologyHTML(String uri){
        try {
            HttpURLConnection connection = doNegotiation(uri,Constants.TEXT_HTML);
            InputStream in = (InputStream) connection.getInputStream();
            //Files.copy(in, Paths.get(downloadPath), StandardCopyOption.REPLACE_EXISTING);
            Document doc = Jsoup.parse(in, "UTF-8", "http://example.com/");
            in.close();
            logger.info("Vocabulary in HTML parsed successfully.");
            return doc;
        } catch (Exception e) {
            final String message = "Failed to download vocabulary in HTML.";
            logger.error(message + e.toString());
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Method that given a URI and a content type, it will retrieve the content from that type
     * @param uri URI of the resource to fetch
     * @param serialization serialization to fetch. If null, it will fetch content by default
     * @return
     * @throws Exception
     */
    public static HttpURLConnection doNegotiation(String uri, String serialization) throws Exception{
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        if(serialization!=null) {
            connection.setRequestProperty("Accept", serialization);
        }
        int status = connection.getResponseCode();
        boolean redirect = false;
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }
        // there are some vocabularies with multiple redirections:
        // 301 -> 303 -> owl
        while (redirect) {
            String newUrl = connection.getHeaderField("Location");
            connection = (HttpURLConnection) new URL(newUrl).openConnection();
            if(serialization!=null) {
                connection.setRequestProperty("Accept", serialization);
            }
            status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_MOVED_TEMP && status != HttpURLConnection.HTTP_MOVED_PERM
                    && status != HttpURLConnection.HTTP_SEE_OTHER)
                redirect = false;
        }
        return connection;
    }

    /**
     * Writes a model into a file
     *
     * @param m the manager
     * @param o the ontology to write
     * @param f the format in which should be written
     * @param outPath
     */
    public static void writeModel(OWLOntologyManager m, OWLOntology o, OWLDocumentFormat f, String outPath) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(outPath);
            m.saveOntology(o, f, out);
            out.close();
        } catch (Exception ex) {
            logger.error("Error while writing the model to file " + ex.getMessage());
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * This method checks if the target URI resolves to something.
     * It does not check what is the response in detail, just that something is returned.
     * @param uri URI to resolve
     * @return true if resolves successfully.
     */
    public static boolean isURIResolvable(String uri){
        try {
            HttpURLConnection connection = Utils.doNegotiation(uri, null);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                return true;
            }
            if (connection.getResponseCode() > 400 ){
                logger.error("Return code for license is" +connection.getResponseCode());
                return false;
            }
        }catch(Exception e){
            logger.error(e.getMessage());
            return false;
        }
        return false;
    }
}

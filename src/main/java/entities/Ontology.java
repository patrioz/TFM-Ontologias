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

package entities;

import fair.Constants;
import fair.Utils;
import org.jsoup.nodes.Document;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Ontology {
    private OWLOntology ontologyModel;
    private Document htmlDocumentation;
    private static final Logger logger = LoggerFactory.getLogger(Ontology.class);

    //metadata attributes
    private String ontologyURI;
    private String namespacePrefix;
    private String title;
    private String description;
    private String versionIRI;
    private String versionInfo;
    private ArrayList<String> authors;
    private ArrayList<String> contributors;
    private String license;
    private String rights;
    private String status;
    private String previousVersion;
    private String creationDate;
    private String issuedDate;
    private String modifiedDate;
    private String source;
    private String backwardCompatibility;
    private String publisher;
    private String citation;
    private String doi;
    private String logo;
    private final ArrayList<String> supportedMetadata;
    private final ArrayList<String> reusedMetadataVocabularies;
    private final ArrayList<String> reusedVocabularies;
    List<OWLImportsDeclaration> importedVocabularies;
    private final ArrayList<String> termsWithLabel;
    private final ArrayList<String> terms; // all terms
    private final ArrayList<String> termsWithDescription;
    private boolean isSKOS = false;

    /**
     *
     * @param o ontology path or URI
     * @param isFromFile flag to indicate whether the ontology should be loaded from file or URL
     * @param tmpFolder temporary folder where to store the downloaded ontology
     */
    public Ontology(String o, boolean isFromFile, Path tmpFolder){
        supportedMetadata = new ArrayList<>();
        reusedMetadataVocabularies = new ArrayList<>();
        reusedVocabularies = new ArrayList<>();
        termsWithLabel = new ArrayList<>();
        termsWithDescription = new ArrayList<>();
        terms = new ArrayList<>();
        //Download ontology (any serialization)
        try {
            this.ontologyModel = Utils.loadModelToDocument(o, isFromFile, tmpFolder.toString());
            this.ontologyURI = this.ontologyModel.getOntologyID().getOntologyIRI().get().toString();
        }catch(Exception e){
            if(ontologyURI == null && ontologyModel !=null){
                logger.error("Could load the ontology, but no owl:Ontology declared!");
                //check if this is a SKOS vocabulary: skos:ConceptScheme
                List<OWLIndividual> cs = EntitySearcher.getInstances(ontologyModel.getOWLOntologyManager().getOWLDataFactory().getOWLClass("http://www.w3.org/2004/02/skos/core#ConceptScheme"), ontologyModel).collect(Collectors.toList());
                if(!cs.isEmpty()){
                    //we retrieve the first concept scheme
                    if(cs.size()>1){
                        logger.warn("More than one concept scheme detected! Running foops only for the first one detected");
                    }
                    this.ontologyURI = cs.get(0).toStringID();//this is a little brittle.
                    this.isSKOS = true;
                }
            }
            else{
                logger.error("Could not load the ontology");
                if(!isFromFile){
                    ontologyURI = o.strip();
                }
            }
        }
        //Download HTML of the onto (if available)
        try{
            htmlDocumentation = Utils.loadOntologyHTML(ontologyURI);
        }
        catch(Exception e){
            logger.error("Could not load ontology in HTML");
            htmlDocumentation = null;
        }
        try {
            this.getOntologyMetadata();
            this.getOntologyCoverage();
        }catch(Exception e){
            logger.error("Error when extracting metadata or annotations");
        }
    }

    /**
     * Method that loads all available ontology metadata, using a series of common vocabularies as reference
     */
    private void getOntologyMetadata(){
        logger.info("Retrieving ontology metadata");
        if (this.ontologyModel == null) {
            logger.error("No ontology loaded! Metadata cannot be extracted");
            return;
        }
        this.authors = new ArrayList<>();
        this.contributors = new ArrayList<>();
        this.title = "Title unavailable";
        if (isSKOS){
            //get annotations on the onto URI.
            OWLIndividual cs = EntitySearcher.getInstances(ontologyModel.getOWLOntologyManager().getOWLDataFactory().
                    getOWLClass(Constants.SKOS_CONCEPT_SCHEME), ontologyModel).collect(Collectors.toList()).get(0);
            EntitySearcher.getAnnotations((OWLEntity) cs, this.getOntologyModel()).forEach(this::completeMetadata);
        }else {
            try {
                this.versionIRI = this.ontologyModel.getOntologyID().getVersionIRI().get().toString();
                this.supportedMetadata.add(Constants.FOOPS_VERSION_IRI);
            } catch (Exception e) {
                logger.info("No version IRI detected");
            }
            this.ontologyModel.annotations().forEach(this::completeMetadata);
        }
    }

    private void completeMetadata(OWLAnnotation a) {
        String propertyName = a.getProperty().getIRI().getIRIString();
        String value;
        switch (propertyName) {
            case Constants.PROP_DC_TITLE:
            case Constants.PROP_DCTERMS_TITLE:
            case Constants.PROP_SCHEMA_NAME:
                try {
//                    valueLanguage = a.getValue().asLiteral().get().getLang();
                    this.title = a.getValue().asLiteral().get().getLiteral();
                    this.supportedMetadata.add(Constants.FOOPS_TITLE);
                } catch (Exception e) {
                    logger.error("Error while getting ontology title. No literal provided");
                }
                break;
            case Constants.PROP_DCTERMS_ABSTRACT:
            case Constants.PROP_DC_ABSTRACT:
            case Constants.PROP_DCTERMS_DESCRIPTION:
            case Constants.PROP_DC_DESCRIPTION:
            case Constants.PROP_SCHEMA_DESCRIPTION:
            case Constants.PROP_RDFS_COMMENT:
            case Constants.PROP_SKOS_NOTE:
                try {
                    this.description = a.getValue().asLiteral().get().getLiteral();
                    this.supportedMetadata.add(Constants.FOOPS_DESCRIPTION);
                } catch (Exception e) {
                    logger.error("Error while getting ontology abstract. No literal provided");
                }
                break;
            case Constants.PROP_DCTERMS_REPLACES:
            case Constants.PROP_DC_REPLACES:
            case Constants.PROP_PROV_WAS_REVISION_OF:
            case Constants.PROP_OWL_PRIOR_VERSION:
            case Constants.PROP_PAV_PREVIOUS_VERSION:
                this.previousVersion = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_PREVIOUS_VERSION);
                break;
            case Constants.PROP_OWL_VERSION_INFO:
            case Constants.PROP_SCHEMA_SCHEMA_VERSION:
                try {
                    this.versionInfo = a.getValue().asLiteral().get().getLiteral();
                    this.supportedMetadata.add(Constants.FOOPS_VERSION_INFO);
                } catch (Exception e) {
                    logger.error("Error while getting ontology abstract. No literal provided");
                }
                break;
            case Constants.PROP_VANN_PREFIX:
                this.namespacePrefix = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_NS_PREFIX);
                break;
            case Constants.PROP_VANN_URI:
                value = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_NS_URI);
                if (!this.ontologyURI.equals(value)) {
                    logger.warn("Ontology NS URI declared and annotated is not the same!");
                }
                break;
            case Constants.PROP_DCTERMS_LICENSE:
            case Constants.PROP_SCHEMA_LICENSE:
            case Constants.PROP_CC_LICENSE:
                try {
                    this.license = Utils.getValueAsLiteralOrURI(a.getValue());
                    this.supportedMetadata.add(Constants.FOOPS_LICENSE);
                } catch (Exception e) {
                    logger.error("Could not retrieve license. Please avoid using blank nodes...");
                }
                break;
            case Constants.PROP_DC_RIGHTS:
                try {
                    this.rights = Utils.getValueAsLiteralOrURI(a.getValue());
                    this.supportedMetadata.add(Constants.FOOPS_RIGHTS);
                } catch (Exception e) {
                    logger.error("Could not retrieve rights. Please avoid using blank nodes...");
                }
                break;
            case Constants.PROP_DC_CONTRIBUTOR:
            case Constants.PROP_DCTERMS_CONTRIBUTOR:
            case Constants.PROP_SCHEMA_CONTRIBUTOR:
            case Constants.PROP_PAV_CONTRIBUTED_BY:
            case Constants.PROP_DC_CREATOR:
            case Constants.PROP_DCTERMS_CREATOR:
            case Constants.PROP_SCHEMA_CREATOR:
            case Constants.PROP_PAV_CREATED_BY:
            case Constants.PROP_PROV_ATTRIBUTED_TO:
            case Constants.PROP_DC_PUBLISHER:
            case Constants.PROP_DCTERMS_PUBLISHER:
            case Constants.PROP_SCHEMA_PUBLISHER:
                try {
                    value = Utils.getValueAsLiteralOrURI(a.getValue());
                    switch (propertyName) {
                        case Constants.PROP_DC_CONTRIBUTOR:
                        case Constants.PROP_DCTERMS_CONTRIBUTOR:
                        case Constants.PROP_SCHEMA_CONTRIBUTOR:
                        case Constants.PROP_PAV_CONTRIBUTED_BY:
                            this.contributors.add(value);
                            this.supportedMetadata.add(Constants.FOOPS_CONTRIBUTOR);
                            break;
                        case Constants.PROP_DC_CREATOR:
                        case Constants.PROP_DCTERMS_CREATOR:
                        case Constants.PROP_PAV_CREATED_BY:
                        case Constants.PROP_PROV_ATTRIBUTED_TO:
                        case Constants.PROP_SCHEMA_CREATOR:
                            this.authors.add(value);
                            this.supportedMetadata.add(Constants.FOOPS_AUTHOR);
                            break;
                        default:
                            this.publisher = value;
                            this.supportedMetadata.add(Constants.FOOPS_PUBLISHER);
                            break;
                    }
                } catch (Exception e) {
                    logger.error("Could not retrieve creator/contributor. Please avoid using blank nodes...");
                }
                break;
            case Constants.PROP_DCTERMS_CREATED:
            case Constants.PROP_SCHEMA_DATE_CREATED:
            case Constants.PROP_PROV_GENERATED_AT_TIME:
            case Constants.PROP_PAV_CREATED_ON:
                try {
                    this.creationDate = a.getValue().asLiteral().get().getLiteral();
                    this.supportedMetadata.add(Constants.FOOPS_CREATION_DATE);
                } catch (Exception e) {
                    logger.error("Error while getting the date. No literal provided");
                }
                break;
            case Constants.PROP_DCTERMS_MODIFIED:
            case Constants.PROP_SCHEMA_DATE_MODIFIED:
                try {
                    this.modifiedDate = a.getValue().asLiteral().get().getLiteral();
                    this.supportedMetadata.add(Constants.FOOPS_MODIFIED);
                } catch (Exception e) {
                    logger.error("Error while getting the date. No literal provided");
                }
                break;
            case Constants.PROP_DCTERMS_BIBLIOGRAPHIC_CIT:
            case Constants.PROP_SCHEMA_CITATION:
                this.citation = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_CITATION);
                break;
            case Constants.PROP_BIBO_DOI:
                this.doi = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_DOI);
                break;
            case Constants.PROP_BIBO_STATUS:
                try {
                    this.status = a.getValue().asLiteral().get().getLiteral();
                    this.supportedMetadata.add(Constants.FOOPS_STATUS);
                } catch (Exception e) {
                    logger.error("Error while getting the status. No literal provided");
                }
                break;
            case Constants.PROP_OWL_BACKWARDS_COMPATIBLE:
                this.backwardCompatibility = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_B_COMPATIBILITY);
                break;
            case Constants.PROP_FOAF_LOGO:
            case Constants.PROP_SCHEMA_SCHEMA_LOGO:
                this.logo = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_LOGO);
                break;
            case Constants.PROP_DC_SOURCE:
            case Constants.PROP_DCTERMS_SOURCE:
                this.source = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_SOURCE);
                break;
            case Constants.PROP_DCTERMS_ISSUED:
                this.issuedDate = Utils.getValueAsLiteralOrURI(a.getValue());
                this.supportedMetadata.add(Constants.FOOPS_ISSUED);
                break;
        }
    }

    /**
     * Method to extract the coverage of the ontology:
     * - Reused vocabularies
     * - Imported vocabularies
     * - Terms with no label
     * - Terms with no description
     * Since this iterates over all terms of the ontology, we do it once in this method
     */
    private void getOntologyCoverage(){
        //metadata vocabularies
        this.ontologyModel.annotations().forEach(this::checkNamespaces);
        //imports
        this.importedVocabularies = this.ontologyModel.importsDeclarations().collect(Collectors.toList());
        //vocabulary reuse
        logger.info("Extracting namespaces, labels, descriptions");
        if(isSKOS) {
            //extract only for skos:Concept
            EntitySearcher.getInstances(ontologyModel.getOWLOntologyManager().getOWLDataFactory().getOWLClass(Constants.SKOS_CONCEPT), ontologyModel).forEach(a-> {
                this.terms.add(a.toStringID());
                OWLAnnotationProperty skosLabel = ontologyModel.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(Constants.PROP_SKOS_PREF_LABEL);
                EntitySearcher.getAnnotations((OWLEntity) a, this.getOntologyModel(), skosLabel).forEach(ann -> {
                    OWLAnnotationValue val = ann.getValue();
                    if(val instanceof OWLLiteral) {
                        this.termsWithLabel.add(a.toStringID());
                    }
                });
                OWLAnnotationProperty skosDescription = ontologyModel.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(Constants.PROP_SKOS_PREF_DEFINITION);
                EntitySearcher.getAnnotations((OWLEntity) a, this.getOntologyModel(), skosDescription).forEach(ann -> {
                    OWLAnnotationValue val = ann.getValue();
                    if(val instanceof OWLLiteral) {
                        this.termsWithDescription.add(a.toStringID());
                    }
                });
            });
        }else{
            this.ontologyModel.classesInSignature().forEach(this::checkTermCoverage);
            this.ontologyModel.objectPropertiesInSignature().forEach(this::checkTermCoverage);
            this.ontologyModel.dataPropertiesInSignature().forEach(this::checkTermCoverage);
        }
    }

    /**
     * This method checks if the metadata vocabularies used in the annotations of the ontologies match our white list
     * @param a annotation to analyze
     */
    private void checkNamespaces(OWLAnnotation a){
        for (String vocab: Constants.VOCS_REUSE_METADATA){
            if (a.getProperty().getIRI().getIRIString().contains(vocab)){
                if(!reusedMetadataVocabularies.contains(vocab)) {
                    reusedMetadataVocabularies.add(vocab);
                }
            }
        }
    }

    /**
     * This method checks whether the ontology is reusing a vocabulary or not
     * @param a named object to analyze.
     */
    private void checkTermCoverage(OWLNamedObject a){
        String termNS = a.getIRI().getNamespace();
        if(termNS.equals(Constants.NS_OWL)) return; //We ignore OWL reuse.
        if(!termNS.contains(this.ontologyURI)) {
            if (!this.reusedVocabularies.contains(termNS)) {
                this.reusedVocabularies.add(termNS);
            }
        }else{
            //get label/def coverage for the ontology URI considered
            String termIRI = a.getIRI().getIRIString();
            if(!terms.contains(termIRI)) { // to avoid duplicates
                terms.add(termIRI);
            }
            OWLAnnotationProperty label = ontologyModel.getOWLOntologyManager().getOWLDataFactory().getRDFSLabel();
            EntitySearcher.getAnnotations((OWLEntity) a, this.getOntologyModel(), label).forEach(ann -> {
                OWLAnnotationValue val = ann.getValue();
                if(val instanceof OWLLiteral) {
                    if(!termsWithLabel.contains(termIRI)) {
                        this.termsWithLabel.add(termIRI);
                    }
                }
            });
            OWLAnnotationProperty description = ontologyModel.getOWLOntologyManager().getOWLDataFactory().
                    getRDFSComment();
            EntitySearcher.getAnnotations((OWLEntity) a, this.getOntologyModel(), description).forEach(ann -> {
                OWLAnnotationValue val = ann.getValue();
                if (val instanceof OWLLiteral) {
                    if(!termsWithDescription.contains(termIRI)) {
                        this.termsWithDescription.add(termIRI);
                    }
                }
            });
        }
    }

    public OWLOntology getOntologyModel() {
        return ontologyModel;
    }

    public String getOntologyURI() {
        return ontologyURI;
    }

    public Document getHtmlDocumentation() {
        return htmlDocumentation;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getLicense() {
        return license;
    }

    public String getRights() {
        return rights;
    }

    public String getNamespacePrefix() {
        return namespacePrefix;
    }

    public String getVersionIRI() {
        return versionIRI;
    }

    public ArrayList<String> getSupportedMetadata() {
        return supportedMetadata;
    }

    public ArrayList<String> getReusedMetadataVocabularies() {
        return reusedMetadataVocabularies;
    }

    public List<OWLImportsDeclaration> getImportedVocabularies() {
        return importedVocabularies;
    }

    public ArrayList<String> getReusedVocabularies() {
        return reusedVocabularies;
    }

    public ArrayList<String> getTermsWithLabel() {
        return termsWithLabel;
    }

    public ArrayList<String> getTermsWithDescription() {
        return termsWithDescription;
    }

    public ArrayList<String> getTerms() {
        return terms;
    }

    public boolean isSKOS() {
        return isSKOS;
    }
}

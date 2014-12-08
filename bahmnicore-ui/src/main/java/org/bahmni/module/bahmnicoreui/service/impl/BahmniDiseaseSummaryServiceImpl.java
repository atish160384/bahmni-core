package org.bahmni.module.bahmnicoreui.service.impl;

import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.bahmni.module.bahmnicoreui.contract.DiseaseDataParams;
import org.bahmni.module.bahmnicoreui.contract.DiseaseSummaryData;
import org.bahmni.module.bahmnicoreui.mapper.DiseaseSummaryMapper;
import org.bahmni.module.bahmnicoreui.service.BahmniDiseaseSummaryService;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bahmniemrapi.encountertransaction.mapper.BahmniObservationMapper;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResult;
import org.openmrs.module.bahmniemrapi.laborder.service.LabOrderResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class BahmniDiseaseSummaryServiceImpl implements BahmniDiseaseSummaryService {

    private PatientService patientService;
    private BahmniObsService bahmniObsService;
    private LabOrderResultsService labOrderResultsService;

    private ConceptService conceptService;

    private DiseaseSummaryMapper diseaseSummaryMapper;

    @Autowired
    public BahmniDiseaseSummaryServiceImpl(PatientService patientService, BahmniObsService bahmniObsService, LabOrderResultsService labOrderResultsService, ConceptService conceptService) {
        this.patientService = patientService;
        this.bahmniObsService = bahmniObsService;
        this.labOrderResultsService = labOrderResultsService;
        this.conceptService = conceptService;
        this.diseaseSummaryMapper = new DiseaseSummaryMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public DiseaseSummaryData getDiseaseSummary(String patientUuid, DiseaseDataParams queryParams) {
        DiseaseSummaryData diseaseSummaryData = new DiseaseSummaryData();
        Collection<Concept> concepts = new ArrayList<>();
            if(queryParams.getObsConcepts() == null){
            throw new RuntimeException("ObsConcept list is null: atleast one concept name should be specified for getting observations of related concept");
        }
        for (String conceptName : queryParams.getObsConcepts()) {
            concepts.add(conceptService.getConceptByName(conceptName));
        }
        List<BahmniObservation> bahmniObservations = bahmniObsService.observationsFor(patientUuid, concepts, queryParams.getNumberOfVisits());

        if(queryParams.getLabConcepts() == null){
            throw new RuntimeException("LabConcept list is null: atleast one concept name should be specified for getting observations of related concept");
        }

        Patient patient = patientService.getPatientByUuid(patientUuid);
//        List<Visit> visits = null;
//        if(queryParams.getNumberOfVisits() != null) {
//            visits = orderDao.getVisitsWithOrders(patient, "TestOrder", true, numberOfVisits);
//        }


        List<LabOrderResult> labOrderResults = labOrderResultsService.getAllForConcepts(patient, queryParams.getLabConcepts(), null);
        diseaseSummaryData.setTabularData(diseaseSummaryMapper.mapObservations(bahmniObservations));
//        diseaseSummaryData.setTabularData(diseaseSummaryMapper.mapLabResults(labOrderResults));
        diseaseSummaryData.setConceptNames(getLeafConceptNames(queryParams.getObsConcepts()));
        return diseaseSummaryData;
    }

    private Set<String> getLeafConceptNames(List<String> obsConcepts) {
        Set<String> leafConcepts = new HashSet<>();
        for (String conceptName : obsConcepts) {
            Concept concept = conceptService.getConceptByName(conceptName);
            addLeafConcepts(concept, null, leafConcepts);
        }
        return leafConcepts;
    }

    private void addLeafConcepts(Concept rootConcept, Concept parentConcept, Collection<String> leafConcepts) {
        if(rootConcept.isSet()){
            for (Concept setMember : rootConcept.getSetMembers()) {
                addLeafConcepts(setMember,rootConcept,leafConcepts);
            };
        }
        else if(!shouldBeExcluded(rootConcept)){
            Concept conceptToAdd = rootConcept;
            if(parentConcept != null){
                if(BahmniObservationMapper.CONCEPT_DETAILS_CONCEPT_CLASS.equals(parentConcept.getConceptClass().getName())){
                    conceptToAdd = parentConcept;
                }
            }
            String fullName = getConceptName(conceptToAdd, ConceptNameType.FULLY_SPECIFIED);
            String shortName = getConceptName(conceptToAdd, ConceptNameType.SHORT);
            leafConcepts.add(shortName==null?fullName:shortName);
        }
    }

    private String getConceptName(Concept rootConcept, ConceptNameType conceptNameType) {
        String conceptName = null;
        ConceptName name = rootConcept.getName(Context.getLocale(), conceptNameType, null);
        if(name != null){
            conceptName  = name.getName();
        }
        return conceptName;
    }

    private boolean shouldBeExcluded(Concept rootConcept) {
        if(BahmniObservationMapper.ABNORMAL_CONCEPT_CLASS.equals(rootConcept.getConceptClass().getName()) ||
                BahmniObservationMapper.DURATION_CONCEPT_CLASS.equals(rootConcept.getConceptClass().getName())){
            return true;
        }
        return false;
    }

}
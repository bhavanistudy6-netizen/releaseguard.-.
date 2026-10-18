package com.releaseguard.comparison;

import com.releaseguard.release.ReleaseRepository;
import com.releaseguard.sbom.SbomComponentRepository;
import com.releaseguard.sbom.SbomDocument;
import com.releaseguard.sbom.SbomDocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReleaseComparisonService {

    private final ReleaseRepository releaseRepository;
    private final SbomDocumentRepository documentRepository;
    private final SbomComponentRepository componentRepository;
    private final DependencyComparisonEngine engine;

    public ReleaseComparisonService(
            ReleaseRepository releaseRepository,
            SbomDocumentRepository documentRepository,
            SbomComponentRepository componentRepository,
            DependencyComparisonEngine engine) {
        this.releaseRepository = releaseRepository;
        this.documentRepository = documentRepository;
        this.componentRepository = componentRepository;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public ReleaseComparisonResponse compare(
            Long applicationId,
            Long fromReleaseId,
            Long toReleaseId) {

        checkRelease(applicationId, fromReleaseId);
        checkRelease(applicationId, toReleaseId);

        SbomDocument oldDocument = findDocument(fromReleaseId);
        SbomDocument newDocument = findDocument(toReleaseId);

        List<DependencySnapshot> before = loadInventory(oldDocument);
        List<DependencySnapshot> after = loadInventory(newDocument);

        return new ReleaseComparisonResponse(
                applicationId,
                fromReleaseId,
                toReleaseId,
                engine.compare(before, after));
    }

    private void checkRelease(Long applicationId, Long releaseId) {
        releaseRepository
                .findByIdAndApplication_Id(releaseId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Release " + releaseId
                                + " not found for this application"));
    }

    private SbomDocument findDocument(Long releaseId) {
        return documentRepository
                .findByRelease_Id(releaseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No SBOM has been uploaded for release " + releaseId));
    }

    private List<DependencySnapshot> loadInventory(
            SbomDocument document) {

        return componentRepository
                .findAllBySbomDocument_IdOrderByIdAsc(document.getId())
                .stream()
                .map(DependencySnapshot::from)
                .toList();
    }
}
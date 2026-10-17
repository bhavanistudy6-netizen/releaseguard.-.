package com.releaseguard.sbom;

import com.releaseguard.release.Release;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sbom_documents")
public class SbomDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false, unique = true)
    private Release release;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "bom_format", nullable = false, length = 30)
    private String bomFormat;

    @Column(name = "spec_version", nullable = false, length = 20)
    private String specVersion;

    @Column(name = "document_sha256", nullable = false, length = 64)
    private String documentSha256;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected SbomDocument() {
    }

    public SbomDocument(
            Release release,
            String originalFilename,
            String bomFormat,
            String specVersion,
            String documentSha256) {
        this.release = release;
        this.originalFilename = originalFilename;
        this.bomFormat = bomFormat;
        this.specVersion = specVersion;
        this.documentSha256 = documentSha256;
    }

    @PrePersist
    void onCreate() {
        uploadedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Release getRelease() {
        return release;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getBomFormat() {
        return bomFormat;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getDocumentSha256() {
        return documentSha256;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
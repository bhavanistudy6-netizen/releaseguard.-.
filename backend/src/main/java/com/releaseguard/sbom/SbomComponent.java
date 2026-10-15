package com.releaseguard.sbom;

import jakarta.persistence.*;

@Entity
@Table(name = "sbom_components")
public class SbomComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sbom_document_id", nullable = false)
    private SbomDocument sbomDocument;

    @Column(name = "bom_ref", length = 255)
    private String bomRef;

    @Column(name = "component_type", nullable = false, length = 50)
    private String componentType;

    @Column(name = "group_name", length = 255)
    private String groupName;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String purl;

    protected SbomComponent() {
    }

    public SbomComponent(
            SbomDocument sbomDocument,
            String bomRef,
            String componentType,
            String groupName,
            String name,
            String version,
            String purl) {
        this.sbomDocument = sbomDocument;
        this.bomRef = bomRef;
        this.componentType = componentType;
        this.groupName = groupName;
        this.name = name;
        this.version = version;
        this.purl = purl;
    }

    public Long getId() {
        return id;
    }

    public SbomDocument getSbomDocument() {
        return sbomDocument;
    }

    public String getBomRef() {
        return bomRef;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getPurl() {
        return purl;
    }
}
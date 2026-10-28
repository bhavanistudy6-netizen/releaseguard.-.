ALTER TABLE deployments
    ADD COLUMN deployment_type VARCHAR(20) NOT NULL DEFAULT 'DEPLOY',
    ADD COLUMN rollback_from_deployment_id BIGINT,
    ADD COLUMN rollback_target_deployment_id BIGINT,
    ADD COLUMN rollback_reason TEXT;

-- Allows the rollback target FK to also verify the restored release.
ALTER TABLE deployments
    ADD CONSTRAINT uq_deployments_id_app_env_release
        UNIQUE (id, application_id, environment, release_id);

ALTER TABLE deployments
    ADD CONSTRAINT fk_deployments_rollback_from
        FOREIGN KEY (
            rollback_from_deployment_id,
            application_id,
            environment
        )
        REFERENCES deployments (
            id,
            application_id,
            environment
        ),

    ADD CONSTRAINT fk_deployments_rollback_target
        FOREIGN KEY (
            rollback_target_deployment_id,
            application_id,
            environment,
            release_id
        )
        REFERENCES deployments (
            id,
            application_id,
            environment,
            release_id
        ),

    ADD CONSTRAINT chk_deployments_rollback_shape
        CHECK (
            (
                deployment_type = 'DEPLOY'
                AND rollback_from_deployment_id IS NULL
                AND rollback_target_deployment_id IS NULL
                AND rollback_reason IS NULL
            )
            OR
            (
                deployment_type = 'ROLLBACK'
                AND rollback_from_deployment_id IS NOT NULL
                AND rollback_target_deployment_id IS NOT NULL
                AND rollback_reason IS NOT NULL
                AND LENGTH(BTRIM(rollback_reason)) > 0
                AND rollback_from_deployment_id
                    <> rollback_target_deployment_id
                AND rollback_from_deployment_id < id
                AND rollback_target_deployment_id
                    < rollback_from_deployment_id
            )
        );

CREATE INDEX idx_deployments_rollback_from
    ON deployments (
        rollback_from_deployment_id,
        application_id,
        environment
    )
    WHERE rollback_from_deployment_id IS NOT NULL;

CREATE INDEX idx_deployments_rollback_target
    ON deployments (
        rollback_target_deployment_id,
        application_id,
        environment,
        release_id
    )
    WHERE rollback_target_deployment_id IS NOT NULL;
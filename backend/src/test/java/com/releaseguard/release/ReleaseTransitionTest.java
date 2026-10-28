package com.releaseguard.release;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTransitionTest {

    @Test
    void newReleaseStartsInDraft() {
        assertEquals(ReleaseStatus.DRAFT, release().getStatus());
    }

    @Test
    void draftCanBeSubmittedForReview() {
        Release release = release();

        release.submitForReview();

        assertEquals(
                ReleaseStatus.READY_FOR_REVIEW,
                release.getStatus()
        );
    }

    @Test
    void reviewedReleaseCanBeApproved() {
        Release release = release();
        release.submitForReview();

        release.approve();

        assertEquals(ReleaseStatus.APPROVED, release.getStatus());
    }

    @Test
    void reviewedReleaseCanBeRejectedAndResubmitted() {
        Release release = release();
        release.submitForReview();

        release.reject();

        assertEquals(ReleaseStatus.DRAFT, release.getStatus());

        release.submitForReview();

        assertEquals(
                ReleaseStatus.READY_FOR_REVIEW,
                release.getStatus()
        );
    }

    @Test
    void approvalCanBeRevoked() {
        Release release = release();
        release.submitForReview();
        release.approve();

        release.revokeApproval();

        assertEquals(ReleaseStatus.DRAFT, release.getStatus());
    }

    @Test
    void draftCannotBeApprovedDirectly() {
        Release release = release();

        assertThrows(IllegalStateException.class, release::approve);

        assertEquals(ReleaseStatus.DRAFT, release.getStatus());
    }

    @Test
    void approvedReleaseCannotBeSubmittedAgain() {
        Release release = release();
        release.submitForReview();
        release.approve();

        assertThrows(
                IllegalStateException.class,
                release::submitForReview
        );

        assertEquals(ReleaseStatus.APPROVED, release.getStatus());
    }

    @Test
    void draftCannotBeRejectedOrRevoked() {
        Release release = release();

        assertThrows(IllegalStateException.class, release::reject);
        assertThrows(
                IllegalStateException.class,
                release::revokeApproval
        );

        assertEquals(ReleaseStatus.DRAFT, release.getStatus());
    }

    @Test
    void eachActiveStatusCanBeRetired() {
        Release draft = release();

        Release reviewed = release();
        reviewed.submitForReview();

        Release approved = release();
        approved.submitForReview();
        approved.approve();

        for (Release release : new Release[]{draft, reviewed, approved}) {
            release.retire();

            assertEquals(ReleaseStatus.RETIRED, release.getStatus());
        }
    }

    @Test
    void retiredReleaseRejectsFurtherTransitions() {
        Release release = release();
        release.retire();

        assertThrows(
                IllegalStateException.class,
                release::submitForReview
        );
        assertThrows(IllegalStateException.class, release::approve);
        assertThrows(IllegalStateException.class, release::reject);
        assertThrows(
                IllegalStateException.class,
                release::revokeApproval
        );
        assertThrows(IllegalStateException.class, release::retire);

        assertEquals(ReleaseStatus.RETIRED, release.getStatus());
    }

    private Release release() {
        return new Release(
                null,
                "v1.0.0",
                "a".repeat(40),
                "demo-app:1.0.0",
                null
        );
    }
}
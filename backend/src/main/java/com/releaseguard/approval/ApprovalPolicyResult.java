package com.releaseguard.approval;

import java.util.List;

public record ApprovalPolicyResult(
        String policyVersion,
        List<String> blockers) {
    public ApprovalPolicyResult {
        blockers = List.copyOf(blockers);
    }

    public boolean allowed() {
        return blockers.isEmpty();
    }
}
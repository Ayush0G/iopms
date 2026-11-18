package com.valenlabs.iopms.common;

import jakarta.validation.constraints.NotNull;

/** Admin review action on a report or a submission. */
public record ReviewRequest(@NotNull ReviewStatus status, String remarks) {
}

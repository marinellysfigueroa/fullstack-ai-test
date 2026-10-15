package com.insightdevelop.orders.service;

import com.insightdevelop.orders.domain.Order;
import com.insightdevelop.orders.eligibility.EligibilityResult;

/**
 * @param alreadyApproved {@code true} when the order was approved by an earlier request (idempotent replay)
 * @param eligibility     the evaluation that justified the approval (null on replay of a seeded approval)
 */
public record ApprovalOutcome(Order order, boolean alreadyApproved, EligibilityResult eligibility) {
}

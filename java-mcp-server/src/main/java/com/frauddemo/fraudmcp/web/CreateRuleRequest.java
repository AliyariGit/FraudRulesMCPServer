package com.frauddemo.fraudmcp.web;

public record CreateRuleRequest(String name, String condition, String action, Integer priority) {
}

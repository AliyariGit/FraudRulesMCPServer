package com.frauddemo.fraudmcp.web;

import com.frauddemo.fraudmcp.repository.RuleDataService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final RuleDataService ruleDataService;

    public DashboardController(RuleDataService ruleDataService) {
        this.ruleDataService = ruleDataService;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        return DashboardHtml.render(ruleDataService.listAllRules(), ruleDataService.listRecentEvaluations(50));
    }
}

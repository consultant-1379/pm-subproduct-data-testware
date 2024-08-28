/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.testware.pm.sub.productdata.scenarios;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.runner;

import javax.inject.Inject;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.cifwk.taf.scenario.TestScenarioRunner;
import com.ericsson.cifwk.taf.scenario.api.ScenarioExceptionHandler;
import com.ericsson.cifwk.taf.scenario.impl.LoggingSecurityScenarioListener;
import com.ericsson.oss.testware.security.authentication.flows.LoginLogoutRestFlows;

public class pmProductDataBaseScenario extends TafTestBase {
    @Inject
    LoginLogoutRestFlows loginLogoutFlows;

    static void execute(final TestScenario scenario) {
        final TestScenarioRunner runner = runner().withDefaultExceptionHandler(ScenarioExceptionHandler.PROPAGATE)
            .withListener(new LoggingSecurityScenarioListener()).build();
        runner.start(scenario);
    }

    /**
     * Inner class containing all datasource file names.
     */
    public static final class DataSourceNames {
        public static final boolean SHARE_DATA_SOURCE = true;
        public static final String USERS_TO_LOGIN = "usersToLogin";
    }

}

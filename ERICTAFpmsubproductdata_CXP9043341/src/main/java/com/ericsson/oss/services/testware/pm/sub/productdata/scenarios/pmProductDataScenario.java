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

import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.cifwk.taf.datasource.UnknownDataSourceTypeException;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.oss.testware.pm.subscription.testflows.PmCrudTestFlows;
import com.ericsson.oss.testware.pm.subscription.testflows.PmFileCollectionFlows;
import com.ericsson.oss.testware.pm.subscription.testflows.PmInitiationTestFlows;
import com.ericsson.oss.testware.pm.subscription.teststeps.crud.PmCreateSubscriptionTestSteps;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.*;
import static com.ericsson.oss.services.testware.pm.sub.productdata.scenarios.pmProductDataBaseScenario.DataSourceNames.SHARE_DATA_SOURCE;
import static com.ericsson.oss.services.testware.pm.sub.productdata.scenarios.pmProductDataBaseScenario.DataSourceNames.USERS_TO_LOGIN;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.AVAILABLE_USERS;
import static com.ericsson.oss.testware.pm.common.dataprovider.SubscriptionInputDataProvider.SubscriptionInputDataSource.SUBSCRIPTION_INPUT_DATA_RECORD;
import static com.ericsson.oss.testware.pm.common.utils.DataSourceHelper.initialiseDataSource;

public class  pmProductDataScenario extends pmProductDataBaseScenario  {
    @Inject
    private PmCrudTestFlows pmCrudTestFlows;
    @Inject
    private PmCreateSubscriptionTestSteps createSubscriptionTestSteps;
    @Inject
    private PmInitiationTestFlows pmInitiationTestFlows;
    @Inject
    private PmFileCollectionFlows pmFileCollectionFlows;

    @Test(groups = {"RFA250"}, enabled = true)
    @TestSuite
    @Parameters({"productDataSubscriptionScenarioData"})
    public void CreateDeleteProductDataSubscription(final String productDataSubscriptionScenarioData) throws UnknownDataSourceTypeException {
        initialiseDataSource(productDataSubscriptionScenarioData, SHARE_DATA_SOURCE);
        final TestScenario scenario = dataDrivenScenario("Create and Delete Product Data Subscription Scenario")
                .addFlow(loginLogoutFlows.login().bindDataSource(AVAILABLE_USERS, dataSource(USERS_TO_LOGIN)))
                .addFlow(pmCrudTestFlows.createSubscription().build())
                .addFlow(pmInitiationTestFlows.activateSubscription().build())
                .addFlow(pmFileCollectionFlows.pdMonitorFiles().build())
                .addFlow(pmInitiationTestFlows.deactivateSubscription().build()).alwaysRun()
                .addFlow(pmCrudTestFlows.deleteSubscription().build()).alwaysRun()
                .addFlow(loginLogoutFlows.logout())
                .withScenarioDataSources(dataSource(AVAILABLE_USERS), dataSource(productDataSubscriptionScenarioData).bindTo(SUBSCRIPTION_INPUT_DATA_RECORD))
                .build();
        execute(scenario);
    }
}

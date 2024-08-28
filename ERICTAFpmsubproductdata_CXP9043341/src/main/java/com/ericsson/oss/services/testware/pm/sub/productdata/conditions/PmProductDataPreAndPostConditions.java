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

package com.ericsson.oss.services.testware.pm.sub.productdata.conditions;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.runner;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.scenario;
import static com.ericsson.oss.testware.cm.cruda.teststeps.CrudaTestSteps.Parameters.SET_NODENAME;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.ADDED_NODES;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.NODES_TO_ADD;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.PMIC_NODES;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.USERS_TO_CREATE;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.USERS_TO_DELETE;
import static com.ericsson.oss.testware.nodeintegration.utilities.NodeIntegrationConstants.NETWORKELEMENTID;
import static com.ericsson.oss.testware.pm.common.dataprovider.Constants.TEST_SUITE_NAME;
import static com.ericsson.oss.testware.pm.common.dataprovider.DatasourceFilters.ADD_AS_PRECONDITION_NULL_ADD_AS_PRECONDITION_TRUE;
import static com.ericsson.oss.testware.pm.common.dataprovider.DatasourceFilters.NODE_SYNC_ENABLE_FILTER;
import static com.ericsson.oss.testware.pm.common.dataprovider.DatasourceFilters.PM_FUNCTION_ENABLED_FILTER;
import static com.ericsson.oss.testware.pm.common.dataprovider.DatasourceFilters.PREDICATE_AND;
import static com.ericsson.oss.testware.pm.common.dataprovider.DatasourceFilters.getSuiteNameFilter;

import javax.inject.Inject;

import com.ericsson.cifwk.taf.scenario.impl.LoggingScenarioListener;
import com.ericsson.oss.testware.pm.common.netsim.NetsimScenarioHelper;
import com.ericsson.oss.testware.pm.common.testflows.PmFunctionTestFlows;
import org.apache.commons.io.FilenameUtils;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.TestContext;
import com.ericsson.cifwk.taf.configuration.TafConfigurationProvider;
import com.ericsson.cifwk.taf.data.DataHandler;
import com.ericsson.cifwk.taf.datasource.DataRecord;
import com.ericsson.cifwk.taf.datasource.TafDataSources;
import com.ericsson.cifwk.taf.datasource.TestDataSource;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.cifwk.taf.scenario.TestScenarioRunner;
import com.ericsson.cifwk.taf.scenario.api.ScenarioExceptionHandler;
import com.ericsson.oss.testware.nodeintegration.flows.NodeIntegrationFlows;
import com.ericsson.oss.testware.security.authentication.flows.LoginLogoutRestFlows;
import com.ericsson.oss.testware.security.gim.flows.UserManagementTestFlows;

import com.google.common.base.Predicate;

/**
 * Before/Aftersuite for pm productData scenarios.
 */
public class PmProductDataPreAndPostConditions extends TafTestBase {

    @Inject
    private LoginLogoutRestFlows loginLogoutFlows;

    private String definedPmicNodes = PMIC_NODES;

    private String suiteName;

    @Inject
    private NodeIntegrationFlows nodeIntegrationFlows;

    @Inject
    private UserManagementTestFlows userManagementTestFlows;

    @Inject
    private NetsimScenarioHelper netsimScenarioHelper;

    @Inject
    private PmFunctionTestFlows pmFunctionTestFlows;

    @Inject
    private TestContext context;

    static void execute(final TestScenario scenario){
        execute(scenario, ScenarioExceptionHandler.PROPAGATE);
    }

    private static void execute(final TestScenario scenario, final  ScenarioExceptionHandler handler){
        final TestScenarioRunner runner = runner().withDefaultExceptionHandler(handler).withListener(new LoggingScenarioListener()).build();
        runner.start(scenario);
    }

    @Parameters({"userName" })
    @BeforeSuite(groups = { "RFA250" }, enabled = true)
    public void preconditions(final String userName, final ITestContext iTestContext) {
        suiteName = FilenameUtils.getBaseName(iTestContext.getSuite().getXmlSuite().getFileName());
        addApplicableUsersToContext(userName);
        createPmUser();
        TafConfigurationProvider.provide().setProperty(TEST_SUITE_NAME, suiteName);
        startNetSimNodes();
        addNodes();
        syncNodes();
        enabledPmFunctionForAllNodes();
    }

    @AfterSuite(groups = { "RFA250" }, enabled = true, alwaysRun = true)
    public void cleanUp() {
        deleteNodes();
        deletePmUser();
    }

    private void createPmUser(){
        final TestScenario scenario = scenario("Create User for PM").addFlow(userManagementTestFlows.deleteUser())
            .withExceptionHandler(ScenarioExceptionHandler.LOGONLY)
            .addFlow(userManagementTestFlows.createUser()).build();
        execute(scenario);
    }

    private void addApplicableUsersToContext(final String userName) {
        final TestDataSource<DataRecord> userList = TafDataSources.copy(TafDataSources.fromCsv("usersToCreate.csv"));
        final TestDataSource<DataRecord> filteredUserList = TafDataSources.filter(userList, new Predicate<DataRecord>() {
            @Override
            public boolean apply(final DataRecord dataRecord) {
                if (dataRecord.getFieldValue("username").equals(userName)) {
                    return true;
                }
                return false;
            }
        });
        context.addDataSource(USERS_TO_CREATE, filteredUserList);
        context.addDataSource(USERS_TO_DELETE, filteredUserList);
        context.addDataSource("usersToLogin", filteredUserList);
    }

    private void startNetSimNodes() {
        final boolean startNodes = DataHandler.getConfiguration().getProperty("startNodes", java.lang.Boolean.FALSE, Boolean.TYPE);
        if (startNodes) {
            execute(netsimScenarioHelper.startNodesScenario(PMIC_NODES, suiteName));
        }
    }

    private void addNodes() {
        final TestScenario scenario = scenario("Add Nodes to ENM").addFlow(loginLogoutFlows.loginDefaultUser())
            .addFlow(loginLogoutFlows.isUserLoggedIn()).addFlow(
                flow("Add Nodes").beforeFlow(TafDataSources.shareDataSource(PMIC_NODES)).addSubFlow(nodeIntegrationFlows.addNode())
                    .withDataSources(dataSource(PMIC_NODES)
                        .withFilter(ADD_AS_PRECONDITION_NULL_ADD_AS_PRECONDITION_TRUE + PREDICATE_AND + getSuiteNameFilter(suiteName))
                        .bindTo(NODES_TO_ADD)).build()).addFlow(loginLogoutFlows.logout()).withDefaultVusers(1).build();
        execute(scenario);
    }

    private void syncNodes() {
        final TestScenario scenario = scenario("Synchronize Nodes").addFlow(loginLogoutFlows.loginDefaultUser())
            .addFlow(loginLogoutFlows.isUserLoggedIn()).addFlow(
                flow("Sync Nodes").beforeFlow(TafDataSources.shareDataSource(PMIC_NODES)).addSubFlow(nodeIntegrationFlows.syncNode())
                    .withDataSources(dataSource(PMIC_NODES).withFilter(
                        ADD_AS_PRECONDITION_NULL_ADD_AS_PRECONDITION_TRUE + PREDICATE_AND + NODE_SYNC_ENABLE_FILTER + PREDICATE_AND
                            + getSuiteNameFilter(suiteName)).allowEmpty().bindTo(NODES_TO_ADD)).build())
            .addFlow(loginLogoutFlows.logout()).withDefaultVusers(1).build();
        execute(scenario);
    }

    private void enabledPmFunctionForAllNodes() {
        final TestScenario scenario = scenario("Activate PmFunction for all added nodes").addFlow(loginLogoutFlows.loginDefaultUser())
            .addFlow(loginLogoutFlows.isUserLoggedIn())
            .addFlow(pmFunctionTestFlows.setPmFunction()
                .withDataSources(dataSource("pmFunction"), dataSource(definedPmicNodes).withFilter(
                    PM_FUNCTION_ENABLED_FILTER + PREDICATE_AND + ADD_AS_PRECONDITION_NULL_ADD_AS_PRECONDITION_TRUE + PREDICATE_AND
                        + getSuiteNameFilter(suiteName)).bindColumn(NETWORKELEMENTID, SET_NODENAME)).build())
            .addFlow(loginLogoutFlows.logout()).build();
        execute(scenario);
    }

    void deleteNodes() {
        final TestScenario scenario = scenario("Remove Nodes from ENM").addFlow(loginLogoutFlows.loginDefaultUser())
            .addFlow(loginLogoutFlows.isUserLoggedIn()).addFlow(
                flow("Remove Nodes").beforeFlow(TafDataSources.shareDataSource(PMIC_NODES)).addSubFlow(nodeIntegrationFlows.deleteNode())
                    .withDataSources(dataSource(ADDED_NODES)).build()).addFlow(loginLogoutFlows.logout()).withDefaultVusers(1).build();
        execute(scenario);
    }

    void deletePmUser() {
        final TestScenario scenario = scenario("Delete User for PM").addFlow(userManagementTestFlows.deleteUser()).build();
        execute(scenario);
    }
}

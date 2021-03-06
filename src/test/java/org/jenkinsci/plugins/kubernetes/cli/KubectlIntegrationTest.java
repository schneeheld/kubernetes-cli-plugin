package org.jenkinsci.plugins.kubernetes.cli;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * @author Max Laverse
 */
public class KubectlIntegrationTest extends KubectlTestBase {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void checkKubectlPresence() throws Exception {
        assumeTrue(kubectlPresent());
    }

    @Test
    public void testBasicWithCa() throws Exception {
        String encodedCertificate = new String(Base64.getEncoder().encode(CA_CERTIFICATE.getBytes()));
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithCa");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServerAndCa.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        r.assertLogContains("kubectl configuration cleaned up", b);
        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("certificate-authority-data: \"" + encodedCertificate + "\""));
        assertThat(configDumpContent, containsString("server: \"" + SERVER_URL + "\""));
    }

    @Test
    public void testBasicWithCluster() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithCluster");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithCluster.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        r.assertLogContains("kubectl configuration cleaned up", b);
        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("name: \"" + CLUSTER_NAME + "\""));
    }

    @Test
    public void testBasicWithoutCa() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithoutCa");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        r.assertLogContains("kubectl configuration cleaned up", b);
        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("insecure-skip-tls-verify: true"));
        assertThat(configDumpContent, containsString("server: \"" + SERVER_URL + "\""));
    }

    @Test
    public void testBasicWithNamespace() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithNamespace");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithNamespace.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        r.assertLogContains("kubectl configuration cleaned up", b);
        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("namespace: \"test-ns\""));
    }

    @Test
    public void testUsernamePasswordCredentials() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), usernamePasswordCredentialWithSpace(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testUsernamePasswordCredentials");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));
        r.assertLogNotContains(PASSWORD_WITH_SPACE, b);

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("username: \"" + USERNAME_WITH_SPACE + "\""));
        assertThat(configDumpContent, containsString("password: \"" + PASSWORD_WITH_SPACE + "\""));
    }

    @Test
    public void testNonFileCredentialsWithContext() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testUsernamePasswordCredentials");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServerAndContext.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("username: \"" + USERNAME + "\""));
        assertThat(configDumpContent, containsString("password: \"" + PASSWORD + "\""));
        assertThat(configDumpContent, containsString("current-context: \"test-sample\""));
    }

    @Test
    public void testSecretCredentials() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), secretCredentialWithSpace(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testSecretCredentials");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));
        r.assertLogNotContains(PASSWORD_WITH_SPACE, b);

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("token: \"" + PASSWORD_WITH_SPACE + "\""));
    }

    @Test
    public void testCertificateCredentials() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), certificateCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testCertificateCredentials");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("client-certificate-data: \"" +
                "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNhekNDQWRRQ0NRR" +
                "FZ0VnhhSHZxcXR6QU5CZ2txaGtpRzl3MEJBUVVGQURCNk1Rc3dDUVlEVl" +
                "FRR0V3SkJWVEVUTUJFR0ExVUVDQk1LVTI5dFpTMVRkR0YwWlRFUU1BNEd" +
                "BMVVFQ2hNSFNtVnVhMmx1Y3pFYU1CZ0dBMVVFQXhNUlMzVmlaWEp1WlhS" +
                "bGN5MVZjMlZ5TFRFeEtEQW1CZ2txaGtpRzl3MEJDUUVXR1d0MVltVnlib" +
                "VYwWlhNdGRYTmxjaTB4UUdwbGJtdHBibk13SGhjTk1UY3hNREF6TVRJMU" +
                "56VTVXaGNOTVRneE1EQXpNVEkxTnpVNVdqQjZNUXN3Q1FZRFZRUUdFd0p" +
                "CVlRFVE1CRUdBMVVFQ0JNS1UyOXRaUzFUZEdGMFpURVFNQTRHQTFVRUNo" +
                "TUhTbVZ1YTJsdWN6RWFNQmdHQTFVRUF4TVJTM1ZpWlhKdVpYUmxjeTFWY" +
                "zJWeUxURXhLREFtQmdrcWhraUc5dzBCQ1FFV0dXdDFZbVZ5Ym1WMFpYTX" +
                "RkWE5sY2kweFFHcGxibXRwYm5Nd2daOHdEUVlKS29aSWh2Y05BUUVCQlF" +
                "BRGdZMEFNSUdKQW9HQkFMS0ViejIrbGpwN3dNTEZYckdhVEZ4M25HUUE0" +
                "c1dsWGtLcGdqYjYrd1U3ZTdYVDFuOHFoOGpEeVNITDRHVUp1TjVUTUNON" +
                "TZOQ3g3Y013SHdYZmRyUlhHZFB0UkxZcUdBSStENnFZWlRsQzhzSFNyTF" +
                "ZXU1ZZQ01IaElIZEZ6QmxJN2t3RVh2RW1JcVIvMVJXS2dHMG1sQnhpQjV" +
                "mbmxXbmphME9UdDRpY2hBZ01CQUFFd0RRWUpLb1pJaHZjTkFRRUZCUUFE" +
                "Z1lFQUZIdktxTU5vdStpZE5aQ2FKSjZ4MnUweHJreEJHMDFVYnNteHlWd" +
                "1Q1dWlDck96c3cveGk5SVc0dmpGRmtKZXpNMlJxc0NHaEZvRFA0aTY0U0" +
                "srK0NYbXJ6VVJ4UUpJYi9xeEdqRUM4SDR5QVU2dGs3YStoellYVWt4bnZ" +
                "sK0F5OWc5WnBWR3Z5a1krbHlGNEJkdnlYZ2I5aGVBbGp3azRtdHRoNmdV" +
                "eXdaRT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==\""));
        assertThat(configDumpContent, containsString("client-key-data: \"" +
                "LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JSUNkUUlCQURBTkJna" +
                "3Foa2lHOXcwQkFRRUZBQVNDQWw4d2dnSmJBZ0VBQW9HQkFMS0ViejIrbG" +
                "pwN3dNTEZYckdhVEZ4M25HUUE0c1dsWGtLcGdqYjYrd1U3ZTdYVDFuOHF" +
                "oOGpEeVNITDRHVUp1TjVUTUNONTZOQ3g3Y013SHdYZmRyUlhHZFB0UkxZ" +
                "cUdBSStENnFZWlRsQzhzSFNyTFZXU1ZZQ01IaElIZEZ6QmxJN2t3RVh2R" +
                "W1JcVIvMVJXS2dHMG1sQnhpQjVmbmxXbmphME9UdDRpY2hBZ01CQUFFQ2" +
                "dZQUdFN29SdVFZMk1YWkxheHFoSXlhTVUwb1FvWE1XMVYxVEdhQWtMUUV" +
                "VbVlUSm1NK0pmckltcEh1WldlNW1vaUVYK0c4QUZpdFZ4MmpYcHpDM0sz" +
                "ZEg5OEZCOXJrcmZGamJaWEpQOG1kaHVUUXo1eVEwVkZ5c1gvRStzZi9Zd" +
                "E5sNjNxd2dDQU1POEU4TkRYUnA3NDFwTWpyRXA2cHk1d1JWRHo3aDdnY3" +
                "dBUUpCQU9kNExXSjlpQ09DOWpQQmR1QVZXcVJsMW81b3dDR3RWcHlCV25" +
                "OMHZENFZRMk5ZZko2WVBWYXZreDJMU1p6eEdGMzllWDFCemRFVVEvTHJR" +
                "WThINXFFQ1FRREZiNmM3bG1Od09ZSXh6OUlhWWZ3b0krblpwMFpFTnUrY" +
                "k14M3EyL01NRWdYREhhS2l5Sm1peGFTbTUvT2IybHVMcVFTRTZvKzluUS" +
                "twWGQ5a3NQQ0JBa0FMQ21wdnhqa1dLSXNCMFBxUW1iUW5IMHhxb29oM2t" +
                "zTU0yQWF1ZHlUN2VSd3J3dTYreWRnektGREdHZnk2NWEwWjNwdEs1RGFq" +
                "QUdwMVRjOWt1U1hCQWtBU29UNStlT3BaSkpRTWJ6ZThGWkxkbHNYeUs3N" +
                "k5vVUZxdTZBUEVVSVYyWDJCczhJczZoRFZNeUVlUHJUVjkveTdhTzlzTz" +
                "FYazVuVWIzaWUrTUpRQkFrQngrNWRWTHh1UVJ3YUZVOTJsQ2syR2p5Rk9" +
                "XN0MvMk55bFlKUldlNDd1NkRqOCt6R0NPblZFaGlNQlpJMHppbWdRWDlV" +
                "aHVkT1NSQis5YzRYWFNFTzUKLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLQ==\""));

    }

    @Test
    public void testTokenProducer() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), tokenCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "tokenProducerCredential");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertThat(configDumpContent, containsString("token: \"faketoken:bob:s3cr3t\""));
    }

    @Test
    public void testPlainKubeConfig() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), fileCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "fileCredential");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpMinimal.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        String originalContent = new BufferedReader(new InputStreamReader(fileCredential(CREDENTIAL_ID).getContent())).lines().collect(Collectors.joining("\n"));
        assertEquals(originalContent, configDumpContent);
    }

    @Test
    public void testMultiKubeConfig() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), fileCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(), fileCredential(SECONDARY_CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfig");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlMultiDumpMinimal.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        assertThat(configDumpContent, containsString("apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"\"\n" +
                "  name: cred9999\n" +
                "- cluster:\n" +
                "    server: \"\"\n" +
                "  name: test-sample\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: cred9999\n" +
                "    user: \"\"\n" +
                "  name: cred9999\n" +
                "- context:\n" +
                "    cluster: \"\"\n" +
                "    user: \"\"\n" +
                "  name: minikube\n" +
                "- context:\n" +
                "    cluster: test-sample\n" +
                "    user: \"\"\n" +
                "  name: test-sample\n" +
                "current-context: test-sample\n"));
    }

    @Test
    public void testMultiKubeConfigUsernames() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), secretCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(), secretCredential(SECONDARY_CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfigUsernames");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlMultiDumpUsernames.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        assertEquals("apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:1234\n" +
                "  name: clus1234\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:9999\n" +
                "  name: clus9999\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: clus1234\n" +
                "    user: cred1234\n" +
                "  name: cont1234\n" +
                "- context:\n" +
                "    cluster: clus9999\n" +
                "    user: cred9999\n" +
                "  name: cont9999\n" +
                "current-context: \"\"\n" +
                "kind: Config\n" +
                "preferences: {}\n" +
                "users:\n" +
                "- name: cred1234\n" +
                "  user:\n" +
                "    token: s3cr3t\n" +
                "- name: cred9999\n" +
                "  user:\n" +
                "    token: s3cr3t", configDumpContent);
    }

    @Test
    public void testMultiKubeConfigWithServer() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), fileCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(), fileCredential(SECONDARY_CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfigWithServer");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlMultiDumpWithServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        assertThat(configDumpContent, containsString("apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:1234\n" +
                "  name: cred1234\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:9999\n" +
                "  name: cred9999\n" +
                "- cluster:\n" +
                "    server: \"\"\n" +
                "  name: test-sample\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: cred9999\n" +
                "    user: \"\"\n" +
                "  name: cred9999\n" +
                "- context:\n" +
                "    cluster: \"\"\n" +
                "    user: \"\"\n" +
                "  name: minikube\n" +
                "- context:\n" +
                "    cluster: cred1234\n" +
                "    user: \"\"\n" +
                "  name: test-sample\n" +
                "current-context: test-sample\n"));
    }

    @Test
    public void testPlainKubeConfigWithContext() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), fileCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "fileCredential");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithContext.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertEquals("---\n" +
                "apiVersion: \"v1\"\n" +
                "clusters:\n" +
                "- name: \"test-sample\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"test-sample\"\n" +
                "  name: \"test-sample\"\n" +
                "- name: \"minikube\"\n" +
                "current-context: \"minikube\"\n" +
                "users: []", configDumpContent);
    }

    @Test
    public void testPlainKubeConfigWithCluster() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), fileCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "fileCredential");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithCluster.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertEquals("---\n" +
                "apiVersion: \"v1\"\n" +
                "clusters:\n" +
                "- name: \"test-sample\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"test-cluster\"\n" +
                "  name: \"test-sample\"\n" +
                "- name: \"minikube\"\n" +
                "current-context: \"test-sample\"\n" +
                "users: []", configDumpContent);
    }

    @Test
    public void testPlainKubeConfigWithServer() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), fileCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "fileCredential");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertEquals("---\n" +
                "apiVersion: \"v1\"\n" +
                "clusters:\n" +
                "- name: \"test-sample\"\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "  name: \"test-sample\"\n" +
                "- name: \"minikube\"\n" +
                "current-context: \"test-sample\"\n" +
                "users: []", configDumpContent);
    }

    @Test
    public void testPlainKubeConfigWithNamespace() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), fileCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "fileCredential");
        p.setDefinition(new CpsFlowDefinition(loadResource("kubectlDumpWithNamespace.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();
        assertEquals("---\n" +
                "apiVersion: \"v1\"\n" +
                "clusters:\n" +
                "- name: \"test-sample\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"test-sample\"\n" +
                "    namespace: \"test-ns\"\n" +
                "  name: \"test-sample\"\n" +
                "- name: \"minikube\"\n" +
                "current-context: \"test-sample\"\n" +
                "users: []", configDumpContent);
    }
}

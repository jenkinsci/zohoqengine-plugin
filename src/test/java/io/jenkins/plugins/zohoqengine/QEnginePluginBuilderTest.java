package io.jenkins.plugins.zohoqengine;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import org.junit.Test;

public class QEnginePluginBuilderTest {

    @Test(expected = NullPointerException.class)
    public void perform() {
        AbstractBuild build = mock(AbstractBuild.class);
        QEnginePluginBuilder builder = new QEnginePluginBuilder(null, "", 10, "test");
        builder.perform(null, null, null, null);
	    assertSame("Unable to initiate the testplan.", build.getResult(), Result.FAILURE);
    }
}

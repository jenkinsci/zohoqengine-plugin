package io.jenkins.plugins.zohoqengine;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;

public class QEnginePluginBuilderTest {
	
	@Test(expected = NullPointerException.class)
	public void perform() throws Exception {
		AbstractBuild build = mock(AbstractBuild.class);
		QEnginePluginBuilder builder = new QEnginePluginBuilder(null,"",10,"test");
		builder.perform(null,null,null,null);
		assertTrue("Unable to initiate the testplan.", build.getResult() == Result.FAILURE);
	}

}

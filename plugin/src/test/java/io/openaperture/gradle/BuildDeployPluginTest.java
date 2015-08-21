package io.openaperture.gradle;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class BuildDeployPluginTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public BuildDeployPluginTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BuildDeployPluginTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testOAGradlePlugin()
    {
        assertTrue( true );
    }
}

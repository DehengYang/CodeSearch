package dale.search;

import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class MainTest 
    extends TestCase
{
	
	private static Logger log = Logger.getLogger(MainTest.class.getName());
	
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MainTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( MainTest.class );
    }

//    /**
//     * Rigourous Test :-)
//     */
//    public void testApp()
//    {
//        assertTrue( true );
//    }
    
//    @Test
    public void test_source_path() throws IOException{
    	log.info("Testing Chart 1");
    	String[] args = new String[] {
    			"org.jfree.chart.renderer.category.AbstractCategoryItemRenderer:1797",
    			"/home/dale/d4j/Chart/Chart_1"};
    	Main.main(args);
    	
    	log.info("Testing Math 1");
    	String[] args2 = new String[] {
    			null,
    			"/home/dale/d4j/Math/Math_1"};
    	Main.main(args2);
    }
    
}

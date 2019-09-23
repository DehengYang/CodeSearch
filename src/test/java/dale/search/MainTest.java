package dale.search;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.Test;

import junit.framework.TestCase;


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

//    /**
//     * @return the suite of tests being tested
//     */
//    public static Test suite()
//    {
//        return new TestSuite( MainTest.class );
//    }

//    /**
//     * Rigourous Test :-)
//     */
//    public void testApp()
//    {
//        assertTrue( true );
//    }
    
//    @Test
//    public void test_source_path() throws IOException{
//    	log.info("Testing Chart 1");
//    	String[] args = new String[] {
//    			"org.jfree.chart.renderer.category.AbstractCategoryItemRenderer:1797",
//    			"/home/dale/d4j/Chart/Chart_1"};
//    	Main.main(args);
//    	
//    	log.info("Testing Math 1");
//    	String[] args2 = new String[] {
//    			null,
//    			"/home/dale/d4j/Math/Math_1"};
//    	Main.main(args2);
//    }
    
    
//    Chart 
//    3 
//    /home/dale/d4j/Chart/Chart_3 
//    /home/dale/d4j/fixed_bugs_dir/Chart/Chart_3  
//    /home/dale/d4j/fixed_bugs_dir/Chart/Chart_3
    
    //Chart 3 /home/dale/d4j/Chart/Chart_3 /home/dale/d4j/fixed_bugs_dir/Chart/Chart_3  /home/dale/d4j/Chart/Chart_3
    @Test
    public void test_time2() throws IOException{
    	log.info("Testing time 2");
    	String[] args2 = new String[] {
    			"Time",
    			"2",
    			"/home/dale/d4j/fixed_bugs_dir/Time/Time_2",
    			"/home/dale/d4j/fixed_bugs_dir/Time/Time_2",
    			"/home/dale/d4j/fixed_bugs_dir/Time/Time_2",
    			};
    	Main.main(args2);
    }
    
//    Mockito 
//    4
//    /home/dale/d4j/Mockito/Mockito_4 
//    /home/dale/d4j/fixed_bugs_dir/Mockito/Mockito_4  
//    /home/dale/d4j/fixed_bugs_dir/Mockito/Mockito_4
    @Test
    public void test_mock4() throws IOException{
    	log.info("Testing mock 4");
    	String[] args2 = new String[] {
    			"Mockito",
    		    "4",
    		    "/home/dale/d4j/Mockito/Mockito_4", 
    		    "/home/dale/d4j/fixed_bugs_dir/Mockito/Mockito_4",  
    		    "/home/dale/d4j/fixed_bugs_dir/Mockito/Mockito_4"
    			};
    	Main.main(args2);
    }
    
    
}

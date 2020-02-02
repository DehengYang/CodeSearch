package donor.search;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.junit.Test;

import donor.main.Main;
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
//    @Test
//    public void test_time2() throws IOException{
//    	log.info("Testing time 2");
//    	String[] args2 = new String[] {
//    			"Time",
//    			"2",
//    			"/home/dale/d4j/fixed_bugs_dir/Time/Time_2",
//    			"/home/dale/d4j/fixed_bugs_dir/Time/Time_2",
//    			"/home/dale/d4j/fixed_bugs_dir/Time/Time_2",
//    			};
//    	Main.main(args2);
//    }
    
//    Mockito 
//    4
//    /home/dale/d4j/Mockito/Mockito_4 
//    /home/dale/d4j/fixed_bugs_dir/Mockito/Mockito_4  
//    /home/dale/d4j/fixed_bugs_dir/Mockito/Mockito_4
//    @Test
//    public void test_mock4() throws IOException{
//    	log.info("Testing mock 4");
//    	// /home/dale/ALL_APR_TOOLS/d4j-repo
//    	String[] args2 = new String[] {
//    			"Mockito",
//    		    "4",
//    		    "/home/dale/d4j/Mockito/Mockito_4", 
//    		    "/home/dale/d4j/fixed_bugs_dir/Mockito/Mockito_4",  
//    		    "/home/dale/d4j/Mockito/Mockito_4"
//    			};
//    	Main.main(args2);
//    }
//    
//    @Test
//    public void test_chart1() throws IOException{
//    	// /home/dale/ALL_APR_TOOLS/d4j-repo
//    	String proj = "Chart";
//    	String id = "1";
//    	String projId = proj + "_" + id;
//    	String[] args2 = new String[] {
//    			proj,
//    		    id,
//    		    "/home/dale/d4j/" + proj + "/" + projId, 
//    		    "/home/dale/d4j/fixed_bugs_dir/" + proj + "/" + projId,  
//    		    "/home/dale/d4j/" + proj + "/" + projId
//    			};
//    	Main.main(args2);
//    }
    
//    @Test
//    public void test_chart2() throws IOException{
//    	// /home/dale/ALL_APR_TOOLS/d4j-repo
//    	String proj = "Chart";
//    	String id = "2";
//    	String projId = proj + "_" + id;
//    	String[] args2 = new String[] {
//    			proj,
//    		    id,
//    		    "/home/dale/d4j/" + proj + "/" + projId, 
//    		    "/home/dale/d4j/fixed_bugs_dir/" + proj + "/" + projId,  
//    		    "/home/dale/d4j/" + proj + "/" + projId
//    			};
//    	Main.main(args2);
//    }
    
	// dale: simple run
	public static String shellRun2(String cmd) throws IOException {
        Process process= Runtime.getRuntime().exec(cmd);
        String results = getShellOut(process);
        return results;
	}
	// dale: simple run
	public static String shellRun2(String[] cmd) throws IOException {
		Process process= Runtime.getRuntime().exec(cmd);
		String results = getShellOut(process);
		return results;
	}
	
	private static String getShellOut(Process process) {
		ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(new ReadShellProcess(process));
        String returnString = "";
        try {
            returnString = future.get(10800L, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            future.cancel(true);
            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (TimeoutException e){
            future.cancel(true);
            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (ExecutionException e){
            future.cancel(true);
            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } finally {
            shutdownProcess(service, process);
        }
        return returnString;
	}
	
	private static void shutdownProcess(ExecutorService service, Process process) {
		service.shutdownNow();
        try {
			process.getErrorStream().close();
			process.getInputStream().close();
	        process.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        process.destroy();
	}
    
    public void testProjId(String proj, String id) throws IOException{
    	// /home/dale/ALL_APR_TOOLS/d4j-repo
//    	String proj = "Chart";
//    	String id = "10";
    	String projId = proj + "_" + id;
    	
    	String repoBuggy = "/home/deheng/ALL_APR_TOOLS/d4j-repo/";
		String repoFixed = "/home/deheng/ALL_APR_TOOLS/d4j-repo/fixed_bugs_dir/";
		String[] cmd = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "/bin/bash single-download.sh "
				+ proj + " " + id + " 1"};
		shellRun2(cmd);
		
		String[] cmd2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash  fixed_single_download.sh "
				+ proj + " " + id + " 1"};
		shellRun2(cmd2);
    	
    	String[] args2 = new String[] {
    			proj,
    		    id,
    		    "/home/deheng/ALL_APR_TOOLS/d4j-repo/" + proj + "/" + projId, 
    		    "/home/deheng/ALL_APR_TOOLS/d4j-repo/fixed_bugs_dir/" + proj + "/" + projId,  
    		    "/home/deheng/ALL_APR_TOOLS/d4j-repo/" + proj + "/" + projId
    			};
    	Main.main(args2);
    }
    
    public void testProjIdOutSide(String proj, String id, String proj2) throws IOException{
    	// /home/dale/ALL_APR_TOOLS/d4j-repo
//    	String proj = "Chart";
//    	String id = "10";
    	String projId = proj + "_" + id;
    	String id2 = "1";
    	
    	String repoBuggy = "/home/deheng/ALL_APR_TOOLS/d4j-repo/";
		String repoFixed = "/home/deheng/ALL_APR_TOOLS/d4j-repo/fixed_bugs_dir/";
		String[] cmd = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "/bin/bash single-download.sh "
				+ proj + " " + id + " 1"};
		shellRun2(cmd);
		
		String[] cmdProj2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash fixed_single_download.sh "
				+ proj2 + " " + id2 + " 1"};
		shellRun2(cmdProj2);
		
		String[] cmd2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash  fixed_single_download.sh "
				+ proj + " " + id + " 1"};
		shellRun2(cmd2);
    	
    	String[] args2 = new String[] {
    			proj,
    		    id,
    		    "/home/deheng/ALL_APR_TOOLS/d4j-repo/" + proj + "/" + projId, 
    		    "/home/deheng/ALL_APR_TOOLS/d4j-repo/fixed_bugs_dir/" + proj + "/" + projId,  
    		    "/home/deheng/ALL_APR_TOOLS/d4j-repo/fixed_bugs_dir/" + proj2 + "/" + proj2 + "_" + id2
    			};
    	Main.main(args2);
    }
    
    
    @Test
    public void test() throws IOException{
//    	testProjId("Time","1");
//    	testProjIdOutSide("Time","1","Chart");
//    	testProjId("Time","2");
//    	testProjId("Time","3");
//    	testProjId("Time","4");
//    	testProjId("Closure","1");
//    	testProjId("Closure","2");
//    	testProjId("Closure","3");
//    	testProjId("Closure","89");
//    	testProjId("Closure","90");
//    	testProjId("Mockito","1");
//    	testProjId("Mockito","2");
//    	testProjId("Mockito","4");
//    	testProjId("Math","4");
//    	testProjIdOutSide("Math","4","Chart");
//    	testProjIdOutSide("Math","4","Mockito");
//    	testProjIdOutSide("Math","4","Time");
//    	testProjIdOutSide("Math","4","Closure");
//    	testProjIdOutSide("Math","4","Lang");
//    	testProjIdOutSide("Math","4","Math");
    	
//    	testProjId("Math","5");
//    	testProjIdOutSide("Math","5","Math");
    	// TODO: similarity metric should be improved! as in this case, return INF; ranks not the highest, besides, with many ties.
    
//    	testProjId("Math","6");
//    	testProjId("Chart","1");
    	
    	testProjId("Chart","3");
    }
    
}

class ReadShellProcess implements Callable<String> {
    public Process process;

    public ReadShellProcess(Process p) {
        this.process = p;
    }

    public synchronized String call() {
        StringBuilder sb = new StringBuilder();
        BufferedInputStream in = null;
        BufferedReader br = null;
        try {
            String s;
            in = new BufferedInputStream(process.getInputStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (sb.length() < 1000000){
                    if (Thread.interrupted()){
                        return sb.toString();
                    }
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
            in = new BufferedInputStream(process.getErrorStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (Thread.interrupted()){
                    return sb.toString();
                }
                if (sb.length() < 1000000){
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (br != null){
                try {
                    br.close();
                } catch (IOException e){
                }
            }
            if (in != null){
                try {
                    in.close();
                } catch (IOException e){
                }
            }
            process.destroy();
        }
        return sb.toString();
    }
}

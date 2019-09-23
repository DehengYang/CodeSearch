package dale.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;

import dale.parser.Subject;
import dale.parser.ProjectInfo;
import dale.parser.NodeUtils;
import dale.search.CodeBlock;
import dale.search.SimpleFilter;

public class MainTry {
	private static String project_dir = System.getProperty("user.dir");
	private static String logfile = project_dir + "/codesearch.log";

	// args
	private static String proj = "";
	private static String id = "";
	//		private static String line = "";
	private static String src_path = "";
	private static String fixed_src_path = "";
	private static String target_source_path = "";

	// code block line range
	private static int snippet_line_range = 5;
	private static int MAX_LESS_THRESHOLD = 3;
	private static int MAX_MORE_THRESHOLD = 5;

	public static void main(String[] args) throws IOException{
		proj = args[0];
		id = args[1];
		src_path = args[2];
		fixed_src_path = args[3];
		target_source_path = args[4];

		// init subject & search log
		proj = proj.toLowerCase();
		Subject subject = Main.getSubject(proj, Integer.parseInt(id));
		ProjectInfo.init(subject);

		init();

		// obtain source code path (recipient code) & target path(for code search)
		src_path = Main.get_source_path(src_path);
		fixed_src_path = Main.get_source_path(fixed_src_path);
		target_source_path = Main.get_source_path(target_source_path);

		List<String> fixed_lines = Main.readFile(proj, id, "fixed");

		List<List<String>> fixed_chunks = getFixedChunks(fixed_lines);

		for (List<String> fixed_chunk : fixed_chunks){
			codeSearchForLine(fixed_chunk, fixed_src_path, "fixed");
		}
	}

	private static List<List<String>> getFixedChunks(List<String> fixed_lines) {
		// sort
		Collections.sort(fixed_lines, new Comparator<String>() {
			@Override
			public int compare(String  o1, String o2) {
				String clazz1 = o1.split(":")[0];
				int lineNo1 = Integer.parseInt(o1.split(":")[1]);

				String clazz2 = o2.split(":")[0];
				int lineNo2 = Integer.parseInt(o2.split(":")[1]);

				// if clazz1 greater than 2, compareTo return 1. 
				if(clazz1.compareTo(clazz2) == 0){
					if(lineNo1 < lineNo2){
						return -1;
					}else if(lineNo1 > lineNo2){
						return 1;
					}else{
						return 0;
					}
				}
				else if (clazz1.compareTo(clazz2) > 0){
					return 1;
				}else{
					return -1;
				}
			}
		});


		List<List<String>> chunks = new ArrayList<>();
		for(String line : fixed_lines){
			String clazz = line.split(":")[0];
			int lineNo = Integer.parseInt(line.split(":")[1]);
			if (chunks.isEmpty()){
				List<String> first_chunk = new ArrayList<>();
				first_chunk.add(line);
				chunks.add(first_chunk);
				continue;
			}

			// traveser the chunks nested list
			int flag = 0;
			for(List<String> chunk : chunks){
				int len = chunk.size();
				for(int i = 0; i < len; i++){
					String lineInChunk = chunk.get(i);
					String clazz_chunk = lineInChunk.split(":")[0];
					int lineNo_chunk = Integer.parseInt(lineInChunk.split(":")[1]);
					// judge if in the same chunk 
					if(clazz_chunk.equals(clazz) && Math.abs(lineNo_chunk - lineNo) == 1){
						chunk.add(line);
						flag = 1;
						break;
					}
				}
				if(flag ==1){
					break;
				}
			}
			// add new chunk
			if(flag == 0){
				List<String> new_chunk = new ArrayList<>();
				new_chunk.add(line);
				chunks.add(new_chunk);
			}
		}

		return chunks;
	}

	private static void codeSearchForLine(List<String> line_chunk, String src_path, String flag) {
		// get compilation unit for the line
		String first_line = line_chunk.get(0);
		String last_line = line_chunk.get(line_chunk.size() - 1);
		String line_path = src_path + "/" + first_line.split(":")[0].replace(".", "/") + ".java";
		print(line_path);
		CompilationUnit unit = (CompilationUnit) Main.genASTFromFile(line_path, ASTParser.K_COMPILATION_UNIT);
		//				print(unit.getRoot().toString());

		// get code snippet
		int first_lineNo = Integer.parseInt(first_line.split(":")[1]);
		int last_lineNo = Integer.parseInt(last_line.split(":")[1]);
		// for test: change 5 to 1
		CodeSnippet codeSnippet = new CodeSnippet(unit, first_lineNo, last_lineNo, snippet_line_range, null, MAX_LESS_THRESHOLD, MAX_MORE_THRESHOLD);

		// start to parse line node based on KPar
//		SuspiciousPosition suspiciousCode = new SuspiciousPosition();
		
		
		
		
		// get code block
		CodeBlock codeBlock = new CodeBlock(line_path, unit, codeSnippet.getASTNodes());
		Integer methodID = codeBlock.getWrapMethodID();
		if(methodID == null){
			LocalLog.log("Find no block");
		}
		List<CodeBlock> buggyBlockList = new LinkedList<>();
		//			buggyBlockList.addAll(codeBlock.reduce());
		buggyBlockList.add(codeBlock);
		Set<String> haveTryBuggySourceCode = new HashSet<>();
		for(CodeBlock oneBuggyBlock : buggyBlockList){
			// get all values for the line.
			
			

			String currentBlockString = oneBuggyBlock.toSrcString().toString();
			if(currentBlockString == null || currentBlockString.length() <= 0){
				continue;
			}
			// skip duplicated source code
			if(haveTryBuggySourceCode.contains(currentBlockString)){
				continue;
			}
			haveTryBuggySourceCode.add(currentBlockString);

			// get all variables can be used at buggy line
			// TODO:
			Map<String, Type> usableVars = new HashMap<String, Type>();
			for(int line_no = first_lineNo; line_no <= last_lineNo; line_no ++){
				usableVars.putAll(NodeUtils.getUsableVarTypes(line_path, line_no));
			}

			// search candidate similar code block
			SimpleFilter simpleFilter = new SimpleFilter(oneBuggyBlock);

			List<Triple<CodeBlock, Double, String>> candidates = simpleFilter.filter(target_source_path, 0.3);

		}
	}

	private static void init() {
		logfile = "./search-log/" + proj + '/' + id + '/' ;
		File dir = new File(logfile);
		if (dir.exists() && dir.isDirectory()) {
			for (File file : dir.listFiles()){
				file.delete();
				print(file.getName() + " exists, and now clear it at the beginning of the process.");
			}
		}
	}

	public static void print(String str){
		System.out.println(str);
	}
}

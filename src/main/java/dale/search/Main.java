package dale.search;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;

import dale.parser.Subject;
import dale.parser.ProjectInfo;
import dale.modify.Revision;
import dale.search.Node;
import dale.modify.Modification;
import dale.parser.NodeUtils;
import dale.search.CodeBlock;
import dale.search.SimpleFilter;
import dale.parser.Constant;

import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Main {
	private static Logger logger = Logger.getLogger(Main.class.getName());
	private static String project_dir = System.getProperty("user.dir");
	private static String logfile = project_dir + "/codesearch.log";
	
	// args
	private static String proj = "";
	private static String id = "";
//	private static String line = "";
	private static String src_path = "";
	private static String fixed_src_path = "";
	private static String target_source_path = "";
	
	// code block line range
	private static int snippet_line_range = 5;
	private static int MAX_LESS_THRESHOLD = 3;
	private static int MAX_MORE_THRESHOLD = 5;
	
	public static void main(String[] args) throws IOException{
		// args: 
		// Chart 1 /home/dale/d4j/Chart/Chart_1 /home/dale/d4j/fixed_bugs_dir/Chart/Chart_1  /home/dale/d4j/Math/Math_1
		// Chart 3 /home/dale/d4j/Chart/Chart_3 /home/dale/d4j/fixed_bugs_dir/Chart/Chart_3  /home/dale/d4j/Math/Math_1
		
		// conduct code search for buggy lines
		proj = args[0];
		id = args[1];
		src_path = args[2];
		fixed_src_path = args[3];
		target_source_path = args[4];
		List<String> lines = readFile(proj, id, "buggy");
		
		// init subject & search log
		proj = proj.toLowerCase();
		Subject subject = getSubject(proj, Integer.parseInt(id));
		ProjectInfo.init(subject);
		
		init();
		
		// obtain source code path (recipient code) & target path(for code search)
		src_path = get_source_path(src_path);
		fixed_src_path = get_source_path(fixed_src_path);
		target_source_path = get_source_path(target_source_path);

		for (String line : lines){
//			codeSearchForLine(line, src_path, "buggy");
		}
		
		List<String> fixed_lines = readFile(proj, id, "fixed");
		// do code search for a single fixed line.
//		for (String line : fixed_lines){
//			codeSearchForLine(line, fixed_src_path, "fixed");	
//		}
		// now to do code search for a chunk.
		// 1) first get all chunks 
		List<List<String>> fixed_chunks = getFixedChunks(fixed_lines);
		//codeSearchForLine("org.jfree.data.general.DatasetUtilities:1242", fixed_src_path, "fixed");	
//		codeSearchForLine("org.jfree.data.time.TimeSeries:825", fixed_src_path, "fixed");
		
//		codeSearchForLine("org.jfree.chart.plot.XYPlot:4493", fixed_src_path, "fixed");
		for (List<String> fixed_chunk : fixed_chunks){
			codeSearchForLine(fixed_chunk, fixed_src_path, "fixed");
		}
		
		
		// ----------------
		// conduct code search for fixed lines
		
		
		// args: 
		// org.jfree.chart.renderer.category.AbstractCategoryItemRenderer:1797 
		// /home/dale/d4j/Chart/Chart_1/   
		// /home/dale/d4j/Chart/Chart_1
		//
		// parse arguments
		// #1 specified line
		// #2 program path
		// e.g., 
		// org.jfree.chart.renderer.category.AbstractCategoryItemRenderer:1797
		// /home/dale/d4j/Chart/Chart_1
		// #3 target source path: /home/dale/d4j/Math/Math_1/
//		line = args[0];
//		path = args[1];
//		target_source_path = args[2];
//		print("The arguments:");
//		for(String arg : args){
//			print(arg);
//		}
//		print("");
//		
//		// pre-process
//		init();
//		int len = path.length();
//		if (path.substring(len-1,len).equals("/")){
//			path = path.substring(0, len-1);
//		}
//		String  proj_id = path.split("/")[path.split("/").length - 1];
//		String projName = proj_id.split("_")[0];
//		projName = projName.toLowerCase();
//		int id = Integer.parseInt( proj_id.split("_")[1]);
//		Subject subject = getSubject(projName, id);
//		ProjectInfo.init(subject);
//		
//		// obtain source code path
//		String src_path = get_source_path(path);
//		target_source_path = get_source_path(target_source_path);
//		
//		// get compilation unit for the line
//		String line_path = src_path + "/" + line.split(":")[0].replace(".", "/") + ".java";
//		print(line_path);
//		CompilationUnit unit = (CompilationUnit)genASTFromFile(line_path, ASTParser.K_COMPILATION_UNIT);
////		print(unit.getRoot().toString());
//		
//		// get code snippet
//		int lineNo = Integer.parseInt(line.split(":")[1]);
//		CodeSnippet codeSnippet = new CodeSnippet(unit, lineNo, 5, null, 3);
//	
//		// get code block
//		CodeBlock codeBlock = new CodeBlock(line_path, unit, codeSnippet.getASTNodes());
//		Integer methodID = codeBlock.getWrapMethodID();
//		if(methodID == null){
//			LocalLog.log("Find no block");
//		}
//		List<CodeBlock> buggyBlockList = new LinkedList<>();
//		buggyBlockList.addAll(codeBlock.reduce());
//		buggyBlockList.add(codeBlock);
//		Set<String> haveTryBuggySourceCode = new HashSet<>();
//		for(CodeBlock oneBuggyBlock : buggyBlockList){
//			String currentBlockString = oneBuggyBlock.toSrcString().toString();
//			if(currentBlockString == null || currentBlockString.length() <= 0){
//				continue;
//			}
//			// skip duplicated source code
//			if(haveTryBuggySourceCode.contains(currentBlockString)){
//				continue;
//			}
//			haveTryBuggySourceCode.add(currentBlockString);
//			Set<String> haveTryPatches = new HashSet<>();
//			
//			// get all variables can be used at buggy line
//			Map<String, Type> usableVars = NodeUtils.getUsableVarTypes(line_path, lineNo);
//			
//			// search candidate similar code block
//			SimpleFilter simpleFilter = new SimpleFilter(oneBuggyBlock);
//			
//			List<Triple<CodeBlock, Double, String>> candidates = simpleFilter.filter(target_source_path, 0.3);
//			List<String> source = null;
//			LocalLog.log("print candidates ---");
//			
//			getAllPatches(oneBuggyBlock, candidates,
//					usableVars, currentBlockString, haveTryPatches);
//			
//			
//		}
		
	}
	
	private static List<List<String>> getFixedChunks(List<String> fixed_lines) {
		// sort
//		String a = "hello";
//		String b = "iello";
//		int c = a.compareTo(b);
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
//				else if(o1.getSecond() > o2.getSecond()){
//					return -1;
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

	private static void codeSearchForLine(String line, String src_path, String flag) {
		// get compilation unit for the line
		String line_path = src_path + "/" + line.split(":")[0].replace(".", "/") + ".java";
		print(line_path);
		CompilationUnit unit = (CompilationUnit)genASTFromFile(line_path, ASTParser.K_COMPILATION_UNIT);
		//				print(unit.getRoot().toString());

		// get code snippet
		int lineNo = Integer.parseInt(line.split(":")[1]);
		// for test: change 5 to 1
		CodeSnippet codeSnippet = new CodeSnippet(unit, lineNo, snippet_line_range, null, MAX_LESS_THRESHOLD, MAX_MORE_THRESHOLD);

		// get code block
		CodeBlock codeBlock = new CodeBlock(line_path, unit, codeSnippet.getASTNodes());
		Integer methodID = codeBlock.getWrapMethodID();
		if(methodID == null){
			LocalLog.log("Find no block");
		}
		List<CodeBlock> buggyBlockList = new LinkedList<>();
//		buggyBlockList.addAll(codeBlock.reduce());
		buggyBlockList.add(codeBlock);
		Set<String> haveTryBuggySourceCode = new HashSet<>();
		for(CodeBlock oneBuggyBlock : buggyBlockList){
			String currentBlockString = oneBuggyBlock.toSrcString().toString();
			if(currentBlockString == null || currentBlockString.length() <= 0){
				continue;
			}
			// skip duplicated source code
			if(haveTryBuggySourceCode.contains(currentBlockString)){
				continue;
			}
			haveTryBuggySourceCode.add(currentBlockString);
			Set<String> haveTryPatches = new HashSet<>();

			// get all variables can be used at buggy line
			Map<String, Type> usableVars = NodeUtils.getUsableVarTypes(line_path, lineNo);

			// search candidate similar code block
			SimpleFilter simpleFilter = new SimpleFilter(oneBuggyBlock);

			List<Triple<CodeBlock, Double, String>> candidates = simpleFilter.filter(target_source_path, 0.3);
//			List<String> source = null;
			LocalLog.log("print candidates ---");

			// each line with a specified log
			logfile = "./search-log/" + proj + '/' + id + '/' + line + "_" + flag + ".log";
			getAllPatches(oneBuggyBlock, candidates,
					usableVars, currentBlockString, haveTryPatches, logfile, flag);
		}
		
	}

	private static List<String> readFile(String proj, String id, String flag) throws IOException {
		String file_path = "";
		proj = upperFirstCase(proj);
		if (flag.equals("buggy")){
			file_path = "buggy_locs/" + proj + '/' + proj + '_' + id + ".txt";
		}else if(flag.equals("fixed")){
			file_path = "buggy_locs/" + proj + '/' + proj + '_' + id + "_fixed.txt";
		}else{
			print("Invalid flag:" + flag);
		}
		
		List<String> lines = new ArrayList<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file_path));
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		
		return lines;
	}
	
	/*
	 * to capitalize the first letter of a string
	 */
	private static String upperFirstCase(String str){
		if (str.length() > 0){
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		}
		return null;
	}

	private static void getAllPatches(CodeBlock oneBuggyBlock, List<Triple<CodeBlock, Double, String>> candidates,
			Map<String, Type> usableVars, String currentBlockString, Set<String> haveTryPatches,
			String logfile, String flag){
		
		// save original code snippet
		writeStringToFile(logfile, "-------- Original Code ---------\n",true);
		writeStringToFile(logfile, currentBlockString, true);
		
		//		int i = 1;
		for(Triple<CodeBlock, Double, String> similar : candidates){
			// only consider top 20 candidates
//			if (i > 2000){
//				break;
//			}
//			i++;
			
			writeStringToFile(logfile, "\n-------- Similar Code ---------\n",true);
			writeStringToFile(logfile,
					similar.getFirst().toString() 
					+ "\n" + similar.getSecond() 
					+ "\n" + similar.getThird()
					+ "\n",true);
			
			// compute transformation
			List<Modification> modifications = CodeBlockMatcher.match(oneBuggyBlock, similar.getFirst(), usableVars);
			if (modifications.size() == 0 && flag.equals("buggy")){
				writeStringToFile(logfile, "\n-------- No Patch ---------\n\n",true);
			}
			
			Map<String, Set<Node>> already = new HashMap<>();
			// try each transformation first
			List<Set<Integer>> list = new ArrayList<>();
			list.addAll(consistentModification(modifications));
			modifications = removeDuplicateModifications(modifications);
			for(int index = 0; index < modifications.size(); index++){
				Modification modification = modifications.get(index);
				String modify = modification.toString();
				Set<Node> tested = already.get(modify);
				if(tested != null){
					if(tested.contains(modification.getSrcNode())){
//						continue;
						print("continue");
					} else {
						tested.add(modification.getSrcNode());
					}
				} else {
					tested = new HashSet<>();
					tested.add(modification.getSrcNode());
					already.put(modify, tested);
				}
				Set<Integer> set = new HashSet<>();
				set.add(index);
				list.add(set);
			}
			
			// also save repeated patches
//			while(true){
//				for(Set<Integer> modifySet : list){
//					for(Integer index : modifySet){
//						modifications.get(index).apply(usableVars);
//					}
//					
//					String replace = oneBuggyBlock.toSrcString().toString();
//					if(replace.equals(currentBlockString)) {
//						for(Integer index : modifySet){
//							modifications.get(index).restore();
//						}
//						continue;
//					}
//					System.out.println("========");
//					System.out.println(replace);
//					System.out.println("========");
//					
//					writeStringToFile(logfile, "\n\n-------- Patch ---------\n",true);
//					writeStringToFile(logfile, replace,true);
//				}
//				break;
//			}
			
			
			//
			List<Modification> legalModifications = new ArrayList<>();
			while(true){
				for(Set<Integer> modifySet : list){
					for(Integer index : modifySet){
						modifications.get(index).apply(usableVars);
					}
					
					String replace = oneBuggyBlock.toSrcString().toString();
					if(replace.equals(currentBlockString)) {
						for(Integer index : modifySet){
							modifications.get(index).restore();
						}
						continue;
					}
					if(haveTryPatches.contains(replace)){
						if(flag.equals("buggy")){
							// also save repeated patch
							writeStringToFile(logfile, "\n\n-------- Repeated Patch ---------\n",true);
							writeStringToFile(logfile, replace, true);
						}
						
//						System.out.println("already try ...");
						for(Integer index : modifySet){
							modifications.get(index).restore();
						}
						if(legalModifications != null){
							for(Integer index : modifySet){
								legalModifications.add(modifications.get(index));
							}
						}
						continue;
					}
					
					System.out.println("========");
					System.out.println(replace);
					System.out.println("========");
					
					if(flag.equals("buggy")){
						writeStringToFile(logfile, "\n\n-------- Patch ---------\n",true);
						writeStringToFile(logfile, replace,true);
					}
					
					haveTryPatches.add(replace);
//					try {
//						JavaFile.sourceReplace(file, source, range.getFirst(), range.getSecond(), replace);
//					} catch (IOException e) {
//						System.err.println("Failed to replace source code.");
//						continue;
//					}
//					try {
//						FileUtils.forceDelete(new File(binFile));
//					} catch (IOException e) {
//					}
					for(Integer index : modifySet){
						modifications.get(index).restore();
					}
				}
				if(legalModifications == null){
					break;
				}
				list = combineModification(legalModifications);
				modifications = legalModifications;
				legalModifications = null;
			}
		}
	}
	
	public static Subject getSubject(String name, int id){
		String fileName = Constant.PROJ_INFO + "/" + name + "/" + id + ".txt";
		File file = new File(fileName);
		if(!file.exists()){
			System.out.println("File : " + fileName + " does not exist!");
			return null;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = null;
		List<String> source = new ArrayList<>();
		try {
			while((line = br.readLine()) != null){
				source.add(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(source.size() < 4){
			System.err.println("PROJEC INFO CONFIGURE ERROR !");
			System.exit(0);
		}
		
		String ssrc = source.get(0);
		String sbin = source.get(1);
		String tsrc = source.get(2);
		String tbin = source.get(3);
		
		Subject subject = new Subject(name, id, ssrc, tsrc, sbin, tbin);
		return subject;
	}
	
	private static List<Set<Integer>> combineModification(List<Modification> modifications){
		List<Set<Integer>> list = new ArrayList<>();
		int length = modifications.size();
		if(length == 0){
			return list;
		}
		int[][] incompatibleMap = new int[length][length];
		for(int i = 0; i < length; i++){
			for(int j = i; j < length; j++){
				if(i == j){
					incompatibleMap[i][j] = 1;
				} else if(modifications.get(i).compatible(modifications.get(j))){
					incompatibleMap[i][j] = 0;
					incompatibleMap[j][i] = 0;
				} else {
					incompatibleMap[i][j] = 1;
					incompatibleMap[i][j] = 1;
				}
			}
		}
		List<Set<Integer>> baseSet = new ArrayList<>();
		for(int i = 0; i < modifications.size(); i++){
			Set<Integer> set = new HashSet<>();
			set.add(i);
			baseSet.add(set);
		}
		
//		List<Set<Integer>> expanded = expand(incompatibleMap, baseSet, 2, 3);
//		for(Set<Integer> set : expanded){
//			Set<Modification> combinedModification = new HashSet<>();
//			for(Integer integer : set){
//				combinedModification.add(modifications.get(integer));
//			}
//			list.add(combinedModification);
//		}
		list.addAll(expand(incompatibleMap, baseSet, 2, 4));
		
		return list;
	}
	
	private static List<Set<Integer>> expand(int[][] incompatibleTabe, List<Set<Integer>> baseSet, int currentSize, int upperbound){
		List<Set<Integer>> rslt = new LinkedList<>();
		if(currentSize > upperbound){
			return rslt;
		}
		while(baseSet.size() > 1000){
			baseSet.remove(baseSet.size() - 1);
		}
		int length = incompatibleTabe.length;
		for(Set<Integer> base : baseSet){
			int minIndex = 0;
			for(Integer integer : base){
				if(integer > minIndex){
					minIndex = integer;
				}
			}
			
			for(minIndex ++; minIndex < length; minIndex ++){
				boolean canExd = true;
				for(Integer integer : base){
					if(incompatibleTabe[minIndex][integer] == 1){
						canExd = false;
						break;
					}
				}
				if(canExd){
					Set<Integer> expanded = new HashSet<>(base);
					expanded.add(minIndex);
					rslt.add(expanded);
				}
			}
		}
		
		if(rslt.size() > 0){
			rslt.addAll(expand(incompatibleTabe, rslt, currentSize + 1, upperbound));
		}
		
		return rslt;
	}
	
	private static List<Set<Integer>> consistentModification(List<Modification> modifications){
		List<Set<Integer>> result = new LinkedList<>();
		String regex = "[A-Za-z_][0-9A-Za-z_.]*";
		Pattern pattern = Pattern.compile(regex);
		for(int i = 0; i < modifications.size(); i++){
			Modification modification = modifications.get(i);
			if(modification instanceof Revision){
				Set<Integer> consistant = new HashSet<>();
				consistant.add(i);
				for(int j = i + 1; j < modifications.size(); j++){
					Modification other = modifications.get(j);
					if(other instanceof Revision){
						if(modification.compatible(other) && modification.getTargetString().equals(other.getTargetString())){
							ASTNode node = Main.genASTFromSource(modification.getTargetString(), ASTParser.K_EXPRESSION);
							if(node instanceof Name || node instanceof FieldAccess || pattern.matcher(modification.getTargetString()).matches()){
								consistant.add(j);
							}
						}
					}
				}
				if(consistant.size() > 1){
					result.add(consistant);
				}
			}
		}
		
		return result;
	}
	
	private static List<Modification> removeDuplicateModifications(List<Modification> modifications){
		//remove duplicate modifications
		List<Modification> unique = new LinkedList<>();
		for (Modification modification : modifications) {
			boolean exist = false;
			for (Modification u : unique) {
				if (u.getRevisionTypeID() == modification.getRevisionTypeID()
						&& u.getSourceID() == modification.getSourceID()
						&& u.getTargetString().equals(modification.getTargetString())
						&& u.getSrcNode().toSrcString().toString().equals(modification.getSrcNode())) {
					exist = true;
					break;
				}
			}
			if(!exist){
				unique.add(modification);
			}
		}
		return unique;
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

	//
	public static CompilationUnit genASTFromFile(String fileName){
		return (CompilationUnit)genASTFromSource(readFileToString(fileName), ASTParser.K_COMPILATION_UNIT);
	}
	
	public static ASTNode genASTFromSource(String icu, int type) {
		ASTParser astParser = ASTParser.newParser(AST.JLS8);
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		astParser.setCompilerOptions(options);
		astParser.setSource(icu.toCharArray());
		astParser.setKind(type);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(true);
		return astParser.createAST(null);
	}
	
	// generate ast from file
	public static ASTNode genASTFromFile(String line_path, int type) {
		String icu = readFileToString(line_path);
		ASTParser astParser = ASTParser.newParser(AST.JLS8);
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		astParser.setCompilerOptions(options);
		astParser.setSource(icu.toCharArray());
		astParser.setKind(type);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(true);
		return astParser.createAST(null);
	}
	
	/**
	 * iteratively search files with the root as {@code file}
	 * 
	 * @param file
	 *            : root file of type {@code File}
	 * @param fileList
	 *            : list to save all the files
	 * @return : a list of all files
	 */
	public static List<File> ergodic(File file, List<File> fileList) {
		if (file == null) {
			logger.info("ergodic Illegal input file : null.");
			return fileList;
		}
		File[] files = file.listFiles();
		if (files == null)
			return fileList;
		for (File f : files) {
			if (f.isDirectory()) {
				ergodic(f, fileList);
			} else if (f.getName().endsWith(".java"))
				fileList.add(f);
		}
		return fileList;
	}
	
	/**
	 * iteratively search the file in the given {@code directory}
	 * 
	 * @param directory
	 *            : root directory
	 * @param fileList
	 *            : list of file
	 * @return : a list of file
	 */
	public static List<String> ergodic(String directory, List<String> fileList) {
		if (directory == null) {
			logger.info("ergodic Illegal input file : null.");
			return fileList;
		}
		File file = new File(directory);
		File[] files = file.listFiles();
		if (files == null)
			return fileList;
		for (File f : files) {
			if (f.isDirectory()) {
				ergodic(f.getAbsolutePath(), fileList);
			} else if (f.getName().endsWith(Constant.SOURCE_FILE_SUFFIX))
				fileList.add(f.getAbsolutePath());
		}
		return fileList;
	}
	
	// read file string
	public static String readFileToString(String filePath) {
		if (filePath == null) {
			logger.info("readFileToString Illegal input file path : null.");
			return new String();
		}
		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			logger.info("readFileToString Illegal input file path : " + filePath);
			return new String();
		}
		return readFileToString(file);
	}
	
	// read file
	public static String readFileToString(File file) {
		if (file == null) {
			logger.info("readFileToString Illegal input file : null.");
			return new String();
		}
		StringBuffer stringBuffer = new StringBuffer();
		InputStream in = null;
		InputStreamReader inputStreamReader = null;
		try {
			in = new FileInputStream(file);
			inputStreamReader = new InputStreamReader(in, "UTF-8");
			char[] ch = new char[1024];
			int readCount = 0;
			while ((readCount = inputStreamReader.read(ch)) != -1) {
				stringBuffer.append(ch, 0, readCount);
			}
			inputStreamReader.close();
			in.close();

		} catch (Exception e) {
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				} catch (IOException e1) {
					return new String();
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
					return new String();
				}
			}
		}
		return stringBuffer.toString();
	}
	
	
	// obtain source code path
	private static String get_source_path(String path){	
		String src_path = "";
		final File folder = new File(path);
		for(File fileEntry : folder.listFiles()){
			if(fileEntry.isDirectory()){
				// try to find source, src, src/java, src/main/java
				// TODO: find . -name AbstractCategoryItemRenderer.java
				// ./source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
				// print(fileEntry.toString()); // return absolute path
				// print(fileEntry.getName());
				if (fileEntry.getName().equals("source")){
					src_path = fileEntry.getPath();
					break;
				}else if(fileEntry.getName().equals("src")) {
					// check whether src/main/java
					String src_path_tmp = fileEntry.getPath() + "/main/java";
					File src_path_tmp_file = new File(src_path_tmp);

					// check whether src/java
					String src_path_tmp_2 = fileEntry.getPath() + "/java";
					File src_path_tmp_file_2 = new File(src_path_tmp_2);

					// check src/main/java
					if (src_path_tmp_file.isDirectory()){
						src_path = src_path_tmp;
					}else if(src_path_tmp_file_2.isDirectory()){
						// check src/java
						src_path = src_path_tmp_2;
					}else{
						// check src
						src_path = fileEntry.getPath();
					}
					break;
				}

			}
		}
		print("src_path: \n"+ src_path + '\n');
		
		return src_path;
	}
	
	public static void print(String str){
		System.out.println(str);
	}

	/**
	 * write {@code string} into file with mode as "not append"
	 * 
	 * @param filePath
	 *            : path of file
	 * @param string
	 *            : message
	 * @return
	 */
	public static boolean writeStringToFile(String filePath, String string) {
		return writeStringToFile(filePath, string, false);
	}

	/**
	 * write {@code string} to file with mode as "not append"
	 * 
	 * @param file
	 *            : file of type {@code File}
	 * @param string
	 *            : message
	 * @return
	 */
	public static boolean writeStringToFile(File file, String string) {
		return writeStringToFile(file, string, false);
	}

	/**
	 * write {@code string} into file with specific mode
	 * 
	 * @param filePath
	 *            : file path
	 * @param string
	 *            : message
	 * @param append
	 *            : writing mode
	 * @return
	 */
	public static boolean writeStringToFile(String filePath, String string, boolean append) {
		if (filePath == null) {
			LocalLog.log("#writeStringToFile Illegal file path : null.");
			return false;
		}
		File file = new File(filePath);
		return writeStringToFile(file, string, append);
	}

	/**
	 * write {@code string} into file with specific mode
	 * 
	 * @param file
	 *            : file of type {@code File}
	 * @param string
	 *            : message
	 * @param append
	 *            : writing mode
	 * @return
	 */
	public static boolean writeStringToFile(File file, String string, boolean append) {
		if (file == null || string == null) {
			LocalLog.log("#writeStringToFile Illegal arguments : null.");
			return false;
		}
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				LocalLog.log("#writeStringToFile Create new file failed : " + file.getAbsolutePath());
				return false;
			}
		}
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			bufferedWriter.write(string);
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

//	private void listFilesForFolder(final File folder) {
//	    for (final File fileEntry : folder.listFiles()) {
//	        if (fileEntry.isDirectory()) {
//	            listFilesForFolder(fileEntry);
//	        } else {
//	            System.out.println(fileEntry.getName());
//	        }
//	    }
//	}
//	final File folder = new File("/home/you/Desktop");
//	listFilesForFolder(folder);
}

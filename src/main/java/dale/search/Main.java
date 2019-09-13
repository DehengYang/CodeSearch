package dale.search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;

import dale.modify.Revision;
import dale.search.Node;
import dale.modify.Modification;
import dale.search.Pair;
import dale.parser.NodeUtils;
import dale.search.CodeBlock;
import dale.search.SimpleFilter;
import dale.parser.Constant;

import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Main {
	private static Logger logger = Logger.getLogger(Main.class.getName());
	private static String path = System.getProperty("user.dir");
	private static String logfile = path + "/codesearch.log";
	
	public static void main(String[] args){
		// parse arguments
		// #1 specified line
		// #2 program path
		// e.g., 
		// org.jfree.chart.renderer.category.AbstractCategoryItemRenderer:1797
		// /home/dale/d4j/Chart/Chart_1
		// #3 target source path: /home/dale/d4j/Math/Math_1/
		String line = args[0];
		String path = args[1];
		String target_source_path = args[2];
		print("The arguments:");
		for(String arg : args){
			print(arg);
		}
		print("");
		
		// pre-process
		init();
		
		// obtain source code path
		String src_path = get_source_path(path);
		target_source_path = get_source_path(target_source_path);
		
		// get compilation unit for the line
		String line_path = src_path + "/" + line.split(":")[0].replace(".", "/") + ".java";
		print(line_path);
		CompilationUnit unit = (CompilationUnit)genASTFromFile(line_path, ASTParser.K_COMPILATION_UNIT);
//		print(unit.getRoot().toString());
		
		// get code snippet
		int lineNo = Integer.parseInt(line.split(":")[1]);
		CodeSnippet codeSnippet = new CodeSnippet(unit, lineNo, 5, null, 3);
	
		// get code block
		CodeBlock codeBlock = new CodeBlock(line_path, unit, codeSnippet.getASTNodes());
		Integer methodID = codeBlock.getWrapMethodID();
		if(methodID == null){
			LocalLog.log("Find no block");
		}
		List<CodeBlock> buggyBlockList = new LinkedList<>();
		buggyBlockList.addAll(codeBlock.reduce());
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
			List<String> source = null;
			LocalLog.log("print candidates ---");
			
			int i = 1;
			for(Triple<CodeBlock, Double, String> similar : candidates){
				// only consider top 20 candidates
				if (i > 20){
					break;
				}
				i++;
				
				writeStringToFile(logfile, "-------- Similar Code ---------\n",true);
				writeStringToFile(logfile,
						similar.getFirst().toString() 
						+ "\n" + similar.getSecond() 
						+ "\n" + similar.getThird()
						+ "\n\n",true);
				
				// compute transformation
				List<Modification> modifications = CodeBlockMatcher.match(oneBuggyBlock, similar.getFirst(), usableVars);
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
							continue;
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
						
						writeStringToFile(logfile, "\n\n-------- Patch ---------\n",true);
						writeStringToFile(logfile, replace,true);
						
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
		File file = new File(logfile);
		if (file.exists()) {
			file.delete();
			print(logfile + " exists, and now clear it at the beginning of the process.");
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

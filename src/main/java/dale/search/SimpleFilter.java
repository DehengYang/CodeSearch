/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package dale.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;

import dale.parser.Constant;
import dale.search.Pair;
import dale.search.CodeBlockMatcher;
import dale.metric.CondStruct;
import dale.metric.MethodCall;
import dale.metric.OtherStruct;
import dale.metric.Variable;
import dale.parser.NodeUtils;
import dale.parser.ProjectInfo;
import dale.search.CodeBlock;

/**
 * @author Jiajun
 * @date Jun 29, 2017
 */
public class SimpleFilter {
	// record candidates
	private List<CodeBlock> _candidates = new ArrayList<>();
	// record candidate file path
//	private List<String> _candidates_paths = new ArrayList<>();
	
	private CodeBlock _buggyCode = null;
	private Set<Variable> _variables = null;
	private Set<CondStruct.KIND> _condStruct = null;
	private Set<OtherStruct.KIND> _otherStruct = null;
	private Set<String> _methods = null;
	private int _max_line = 0;
	private int DELTA_LINE = 10;
	
	public SimpleFilter(CodeBlock buggyCode) {
		_buggyCode = buggyCode;
		_variables = new HashSet<>(buggyCode.getVariables());
		_condStruct = new HashSet<>();
		for(CondStruct condStruct : buggyCode.getCondStruct()){
			_condStruct.add(condStruct.getKind());
		}
		_otherStruct = new HashSet<>();
		for(OtherStruct otherStruct : buggyCode.getOtherStruct()){
			_otherStruct.add(otherStruct.getKind());
		}
		_methods = new HashSet<>();
		for(MethodCall call : _buggyCode.getMethodCalls()){
			_methods.add(call.getName());
		}
		_max_line = _buggyCode.getCurrentLine() + DELTA_LINE;
	}
	
	public List<Triple<CodeBlock, Double, String>> filter(String srcPath, double guard){
		// get all files in the src path
		List<String> files = Main.ergodic(srcPath, new ArrayList<String>());
		// pair of codeBlock and Similarity
		List<Triple<CodeBlock, Double, String>> filtered = new ArrayList<>();
		CollectorVisitor collectorVisitor = new CollectorVisitor();
		// for each file, start to find similar code
		for(String file : files){
			CompilationUnit unit = Main.genASTFromFile(file);
			collectorVisitor.setUnit(file, unit);
			unit.accept(collectorVisitor);
			filtered = filter(filtered, guard);
		}
		
		Set<String> exist = new HashSet<>();
		for(Triple<CodeBlock, Double, String> pair : filtered){
			if(exist.contains(pair.getFirst().toSrcString().toString())){
				continue;
			}
			exist.add(pair.getFirst().toSrcString().toString());
			double similarity = CodeBlockMatcher.getRewardSimilarity(_buggyCode, pair.getFirst()) + pair.getSecond();
			pair.setSecond(similarity);
		}
		
		Collections.sort(filtered, new Comparator<Triple<CodeBlock, Double, String>>() {
			@Override
			public int compare(Triple<CodeBlock, Double, String> o1, Triple<CodeBlock, Double, String> o2) {
				if(o1.getSecond() < o2.getSecond()){
					return 1;
				} else if(o1.getSecond() > o2.getSecond()){
					return -1;
				} else {
					return 0;
				}
			}
		});
		
		return filtered;
	}
	
	private List<Triple<CodeBlock, Double, String>> filter(List<Triple<CodeBlock, Double, String>> filtered, double guard){
//		List<Triple<CodeBlock, Double, String>> filtered = new ArrayList<>();
		int delta = Constant.MAX_BLOCK_LINE - _buggyCode.getCurrentLine();
		delta = delta > 0 ? delta : 0;
		guard = guard + ((0.7 - guard) * delta / Constant.MAX_BLOCK_LINE ); // 0.9
//		System.out.println("Real guard value : " + guard);
		Set<String> codeRec = new HashSet<>();
		for(CodeBlock block : _candidates){
			if(_otherStruct.size() + _condStruct.size() > 0){
				if((block.getCondStruct().size() + block.getOtherStruct().size()) == 0){
					continue;
				}
			}
			Double similarity = CodeBlockMatcher.getSimilarity(_buggyCode, block);
//			System.out.println(block.toSrcString().toString());
			if(similarity < guard){
//				System.out.println("Filtered by similiraty value : " + similarity);
				continue;
			}
//			similarity += CodeBlockMatcher.getRewardSimilarity(_buggyCode, block);
//			if (codeRec.contains(block.toSrcString().toString()) || _buggyCode.hasIntersection(block)) {
//				System.out.println("Duplicate >>>>>>>>>>>>>>>>");
//			} else {
				if(block.getCurrentLine() == 1 && _buggyCode.getCurrentLine() != 1){
					continue;
				}
				int i = 0;
				boolean hasIntersection = false;
				int replace = -1;
				for(; i < filtered.size(); i++){
					Triple<CodeBlock, Double, String> pair = filtered.get(i);
					if(pair.getFirst().hasIntersection(block)){
						hasIntersection = true;
						if(similarity > pair.getSecond()){
							replace = i;
						}
						break;
					}
				}
				
				if(hasIntersection){
					if(replace != -1){
						filtered.remove(replace);
						codeRec.add(block.toSrcString().toString());
						filtered.add(new Triple<CodeBlock, Double, String>(block, similarity, block.getFileName() + block.getCodeRange().toString() ));
					}
				} else {
					codeRec.add(block.toSrcString().toString());
					filtered.add(new Triple<CodeBlock, Double, String>(block, similarity, block.getFileName() + block.getCodeRange().toString() ));
				}
//			}
		}
		
		Collections.sort(filtered, new Comparator<Triple<CodeBlock, Double, String>>() {
			@Override
			public int compare(Triple<CodeBlock, Double, String> o1, Triple<CodeBlock, Double, String> o2) {
				if(o1.getSecond() < o2.getSecond()){
					return 1;
				} else if(o1.getSecond() > o2.getSecond()){
					return -1;
				} else {
					return 0;
				}
			}
		});
		_candidates = new ArrayList<>();
		if(filtered.size() > 1000){
			for(int i = filtered.size() - 1; i > 1000; i--){
				filtered.remove(i);
			}
		}
		return filtered;
	}
	
	class CollectorVisitor extends ASTVisitor{
		
		private CompilationUnit _unit = null;
		private String _fileName = null;
		
		public void setUnit(String fileName, CompilationUnit unit){
			// file name and relevant CU
			_fileName = fileName;
			_unit = unit;
		}
		
		@Override
		public boolean visit(SimpleName node) {
			// SimpleName class: AST node for a simple name. A simple name is an identifier other than a keyword, boolean literal ("true", "false") or null literal ("null"). 
			// getFullyQualifiedName(): Returns the standard dot-separated representation of this name. If the name is a simple name, the result is the name's identifier. If the name is a qualified name, the result is the name of the qualifier (as computed by this method) followed by "." followed by the name's identifier.
			String name = node.getFullyQualifiedName();
			if(Character.isUpperCase(name.charAt(0))){
				return true;
			}
			Pair<String, String> classAndMethodName = NodeUtils.getTypeDecAndMethodDec(node);
			Type type = ProjectInfo.getVariableType(classAndMethodName.getFirst(), classAndMethodName.getSecond(), name);
			Variable variable = new Variable(null, name, type);
			boolean match = false;
			if(_variables.contains(variable) || _methods.contains(name) || sameStructure(node)){
				match = true;
			} else {
				ASTNode parent = node.getParent();
				while(parent != null && !(parent instanceof Statement)){
					if(parent instanceof MethodInvocation){
						if(_methods.contains(((MethodInvocation) parent).getName().getFullyQualifiedName())){
							match = true;
						}
						break;
					}
					parent = parent.getParent();
				}
			}
			if(match){
				ASTNode parent = node.getParent();
				Statement statement = null;
				while(parent != null && !(parent instanceof MethodDeclaration)){
					parent = parent.getParent();
					if(statement == null && parent instanceof Statement){
						statement = (Statement) parent;
					}
				}
				// filter out anonymous classes
				if(parent != null && !(parent.getParent() instanceof AnonymousClassDeclaration)){
					int line = _unit.getLineNumber(node.getStartPosition());
					CodeSnippet codeSnippet = new CodeSnippet(_unit, line, _buggyCode.getCurrentLine(), statement);
					CodeBlock codeBlock = new CodeBlock(_fileName, _unit, codeSnippet.getASTNodes());
					if(codeBlock.getCurrentLine() < _max_line){
						_candidates.add(codeBlock);
//						_candidates_paths.add(_fileName);
					}
				}
			}
			return true;
		}
		
		private boolean sameStructure(SimpleName name){
			return false;
//			if(_condStruct.size() == 0 && _otherStruct.size() == 0){
//				return false;
//			}
//			ASTNode parent = name.getParent();
//			Object kind = null;
//			while(parent != null){
//				if(parent instanceof MethodDeclaration){
//					break;
//				} else if(parent instanceof IfStatement){
//					kind = CondStruct.KIND.IF;
//					break;
//				} else if(parent instanceof SwitchStatement){
//					kind = CondStruct.KIND.SC;
//					break;
//				} else if(parent instanceof ReturnStatement){
//					kind = OtherStruct.KIND.RETURN;
//					break;
//				} else if(parent instanceof ConditionalExpression){
//					kind = CondStruct.KIND.CE;
//					break;
//				} else if(parent instanceof ThrowStatement){
//					kind = OtherStruct.KIND.THROW;
//					break;
//				}
//				parent = parent.getParent();
//			}
//			if(kind == null){
//				return false;
//			}
//			if(_condStruct.contains(kind) || _otherStruct.contains(kind)){
//				return true;
//			}
//			return false;
		}
	}
	
}
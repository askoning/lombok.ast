/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.Block;
import lombok.ast.Case;
import lombok.ast.Catch;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.If;
import lombok.ast.Node;
import lombok.ast.Statement;
import lombok.ast.Switch;
import lombok.ast.SyntaxProblem;
import lombok.ast.Try;
import lombok.ast.TypeDeclaration;
import lombok.ast.VariableDeclaration;
import lombok.ast.While;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class StatementChecks {
	private final List<SyntaxProblem> problems;
	
	public StatementChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void checkNotLoneTry(Try node) {
		if (node.rawCatches().size() == 0 && node.getRawFinally() == null) {
			problems.add(new SyntaxProblem(node, "try statement with no catches and no finally"));
		}
	}
	
	public void checkDeclarationsAsDirectChildWhile(While node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	public void checkDeclarationsAsDirectChildDo(DoWhile node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	public void checkDeclarationsAsDirectChildForEach(ForEach node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	public void checkDeclarationsAsDirectChildIf(If node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
		checkDeclarationsAsDirectChild(node, node.getRawElseStatement());
	}
	
	public void checkDeclarationsAsDirectChildFor(For node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	private void checkDeclarationsAsDirectChild(Node n, Node c) {
		if (c instanceof VariableDeclaration) {
			problems.add(new SyntaxProblem(c, "Variable declarations only make sense in the context of a block."));
		}
		
		if (c instanceof TypeDeclaration) {
			problems.add(new SyntaxProblem(c, "Type declarations only make sense in the context of a block or other type."));
		}
	}
	
	public void checkVarDefOfCatch(Catch node) {
		BasicChecks.checkVarDefIsSimple(problems, node, node.getRawExceptionDeclaration(), "catch blocks", "exception");
	}
	
	public void checkVarDefOfForEach(ForEach node) {
		BasicChecks.checkVarDefIsSimple(problems, node, node.getRawVariable(), "for-each statements", "loop");
	}
	
	public void checkCaseChildOfSwitch(Case node) {
		checkChildOfSwitch(node, "case");
	}
	
	public void checkDefaultChildOfSwitch(Default node) {
		checkChildOfSwitch(node, "default");
	}
	
	private void checkChildOfSwitch(Node node, String desc) {
		Node p = node.getParent();
		Node gp = p == null ? null : p.getParent();
		boolean genError = false;
		
		if (!(p instanceof Statement)) return;
		
		genError = !(p instanceof Block);
		genError |= gp != null && !(gp instanceof Switch);
		
		problems.add(new SyntaxProblem(node, desc + " statements are only legal directly inside switch statements."));
	}
	
	public void checkSwitchStartsWithDefaultOrCase(Switch node) {
		Node rawBody = node.getRawBody();
		if (rawBody instanceof Block) {
			Node first = ((Block)rawBody).rawContents().first();
			if (first != null && !(first instanceof Case) && !(first instanceof Default)) {
				problems.add(new SyntaxProblem(node, "switch statements should start with a default or case statement."));
			}
		}
	}
}

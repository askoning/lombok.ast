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
package lombok.ast.grammar;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.ast.AstException;
import lombok.ast.Node;
import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.StructureFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RunForEachFileInDirRunner.class)
public class PositionTest extends RunForEachFileInDirRunner.SourceFileBasedTester {
	@Override
	protected Collection<DirDescriptor> getDirDescriptors() {
		return Collections.singleton(DirDescriptor.of(new File("test/resources/idempotency"), true));
	}
	
	@Test
	public void testPositions(Source source) throws IOException {
		source.parseCompilationUnit();
		
		if (!source.getProblems().isEmpty()) {
			fail(source.getProblems().get(0).toString());
		}
		
		Node node = source.getNodes().get(0);
		PositionCheckingFormatter formatter = new PositionCheckingFormatter(source);
		node.accept(new SourcePrinter(formatter));
		List<AstException> problems = formatter.getProblems();
		try {
			if (!problems.isEmpty()) fail("position error: " + problems.get(0));
		} catch (AssertionError e) {
			System.out.println("-------PARSED-PRINTED:");
			StructureFormatter formatter2 = StructureFormatter.formatterWithPositions();
			node.accept(new SourcePrinter(formatter2));
			System.out.println(formatter2.finish());
			System.out.println("--------------PRINTED:");
			System.out.println(formatter.finish());
			System.out.println("----------------------");
			throw e;
		}
	}
}

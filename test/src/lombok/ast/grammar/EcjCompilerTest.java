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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RunForEachFileInDirRunner.class)
public class EcjCompilerTest extends RunForEachFileInDirRunner.SourceFileBasedTester {
	private static final boolean EXTENDED = System.getProperty("lombok.ast.test.extended") != null;
	
	@Override
	protected Collection<DirDescriptor> getDirDescriptors() {
		return Arrays.asList(
				DirDescriptor.of(new File("test/resources/idempotency"), true),
				DirDescriptor.of(new File("test/resources/alias"), true));
	}
	
	protected CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = new CompilerOptions();
		options.complianceLevel = ClassFileConstants.JDK1_6;
		options.sourceLevel = ClassFileConstants.JDK1_6;
		options.targetJDK = ClassFileConstants.JDK1_6;
		options.parseLiteralExpressionsAsConstants = true;
		return options;
	}
	
	@Test
	public boolean testEcjCompiler(File file) throws IOException {
		if (!EXTENDED) return false;
		org.eclipse.jdt.internal.compiler.batch.Main main =
			new org.eclipse.jdt.internal.compiler.batch.Main(
					new PrintWriter(System.out), new PrintWriter(System.err),
					false, null, null);
		File tempDir = getTempDir();
		tempDir.mkdirs();
		String[] argv = {
				"-d", tempDir.getAbsolutePath(),
				"-encoding", "UTF-8",
				"-proc:none",
				"-1.6", "-nowarn", "-enableJavadoc",
				file.getAbsolutePath()
		};
		main.compile(argv);
		assertEquals("Errors occurred while compiling this file with ecj", 0, main.globalErrorsCount);
		return true;
	}
	
	private File getTempDir() {
		String[] rawDirs = {
				System.getProperty("java.io.tmpdir"),
				"/tmp",
				"C:\\Windows\\Temp"
		};
		
		for (String dir : rawDirs) {
			if (dir == null) continue;
			File f = new File(dir);
			if (!f.isDirectory()) continue;
			return new File(f, "lombok.ast-test");
		}
		
		return new File(getDirDescriptors().iterator().next().getDirectory(), "tmp");
	}
}

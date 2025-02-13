/*
 * Copyright © 2010-2011 Reinier Zwitserloot, Roel Spilker and Robbert Jan Grootjans.
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
package lombok.ast.javac;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import lombok.ast.AlternateConstructorInvocation;
import lombok.ast.Annotation;
import lombok.ast.AnnotationDeclaration;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationMethodDeclaration;
import lombok.ast.AnnotationValueArray;
import lombok.ast.ArrayAccess;
import lombok.ast.ArrayCreation;
import lombok.ast.ArrayDimension;
import lombok.ast.ArrayInitializer;
import lombok.ast.Assert;
import lombok.ast.AstException;
import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Block;
import lombok.ast.BooleanLiteral;
import lombok.ast.Break;
import lombok.ast.Case;
import lombok.ast.Cast;
import lombok.ast.Catch;
import lombok.ast.CharLiteral;
import lombok.ast.ClassDeclaration;
import lombok.ast.ClassLiteral;
import lombok.ast.Comment;
import lombok.ast.CompilationUnit;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Continue;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.EmptyDeclaration;
import lombok.ast.EmptyStatement;
import lombok.ast.EnumConstant;
import lombok.ast.EnumDeclaration;
import lombok.ast.EnumTypeBody;
import lombok.ast.Expression;
import lombok.ast.ExpressionStatement;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.If;
import lombok.ast.ImportDeclaration;
import lombok.ast.InlineIfExpression;
import lombok.ast.InstanceInitializer;
import lombok.ast.InstanceOf;
import lombok.ast.IntegralLiteral;
import lombok.ast.InterfaceDeclaration;
import lombok.ast.JavadocContainer;
import lombok.ast.KeywordModifier;
import lombok.ast.LabelledStatement;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.NullLiteral;
import lombok.ast.PackageDeclaration;
import lombok.ast.Position;
import lombok.ast.Return;
import lombok.ast.Select;
import lombok.ast.Statement;
import lombok.ast.StaticInitializer;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;
import lombok.ast.Super;
import lombok.ast.SuperConstructorInvocation;
import lombok.ast.Switch;
import lombok.ast.Synchronized;
import lombok.ast.This;
import lombok.ast.Throw;
import lombok.ast.Try;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;
import lombok.ast.While;
import lombok.ast.WildcardKind;
import lombok.ast.grammar.Source;
import lombok.ast.grammar.SourceStructure;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Name.Table;

import static lombok.ast.ConversionPositionInfo.getConversionPositionInfo;

/**
 * Turns {@code lombok.ast} based ASTs into javac's {@code JCTree} model.
 */
public class JcTreeBuilder {
	private final TreeMaker treeMaker;
	private final Table table;
	private final Map<Node, Collection<SourceStructure>> sourceStructures;
	private final Map<JCTree, Integer> endPosTable;
	
	private List<? extends JCTree> result = null;
	
	public JcTreeBuilder() {
		this(null, createNewContext());
	}
	
	private static Context createNewContext() {
		Context c = new Context();
		// Older javacs such as the 1.6 of apple has DefaultFileManager. Newer ones have JavacFileManager.
		// As javac6 might be on the classpath, JavacFileManager will probably exist but its initialization will fail.
		// Initializing both is as far as I know not an issue. -ReinierZ
		try {
			Method m = Class.forName("com.sun.tools.javac.util.DefaultFileManager").getDeclaredMethod("preRegister", Context.class);
			m.invoke(null, c);
		} catch (Throwable t) {
			// intentional do nothing
		}
		try {
			Method m = Class.forName("com.sun.tools.javac.util.JavacFileManager").getDeclaredMethod("preRegister", Context.class);
			m.invoke(null, c);
		} catch (Throwable t) {
			// intentional do nothing
		}
		// DefaultFileManager.preRegister(c);
		// JavacFileManager.preRegister(c);
		return c;
	}
	
	public JcTreeBuilder(Source source, Context context) {
		this(source == null ? null : source.getSourceStructures(), TreeMaker.instance(context), Name.Table.instance(context), Maps.<JCTree, Integer>newHashMap());
	}
	
	private JcTreeBuilder(Map<Node, Collection<SourceStructure>> structures, TreeMaker treeMaker, Table nameTable, Map<JCTree, Integer> endPosTable) {
		if (treeMaker == null) throw new NullPointerException("treeMaker");
		if (nameTable == null) throw new NullPointerException("nameTable");
		this.treeMaker = treeMaker;
		this.table = nameTable;
		this.sourceStructures = structures;
		this.endPosTable = endPosTable;
	}
	
	private Name toName(Identifier identifier) {
		if (identifier == null) return null;
		return table.fromString(identifier.astValue());
	}
	
	private JCTree toTree(Node node) {
		if (node == null) return null;
		JcTreeBuilder builder = create();
		node.accept(builder.visitor);
		try {
			return builder.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private JCExpression toExpression(Node node) {
		return (JCExpression)toTree(node);
	}
	
	private JCStatement toStatement(Node node) {
		return (JCStatement)toTree(node);
	}
	
	private <T extends JCTree> List<T> toList(Class<T> type, StrictListAccessor<?, ?> accessor) {
		List<T> result = List.nil();
		for (Node node : accessor) {
			JcTreeBuilder builder = create();
			node.accept(builder.visitor);
			
			List<? extends JCTree> values;
			
			try {
				values = builder.getAll();
				if (values.size() == 0) throw new RuntimeException();
			} catch (RuntimeException e) {
				System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
				throw e;
			}
			
			for (JCTree value : values) {
				if (value != null && !type.isInstance(value)) {
					throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + type.getName());
				}
				result = result.append(type.cast(value));
			}
		}
		return result;
	}
	
	private <T extends JCTree> List<T> toList(Class<T> type, Node node) {
		if (node == null) return List.nil();
		JcTreeBuilder builder = create();
		node.accept(builder.visitor);
		@SuppressWarnings("unchecked")
		List<T> all = (List<T>)builder.getAll();
		return List.<T>nil().appendList(all);
	}
	
	public void visit(Node node) {
		node.accept(visitor);
	}
	
	public JCTree get() {
		if (result.size() > 1) {
			throw new RuntimeException("Expected only one result but got " + result.size());
		}
		return result.head;
	}
	
	public List<? extends JCTree> getAll() {
		return result;
	}
	
	private boolean set(Node node, JCTree value) {
		if (result != null) throw new IllegalStateException("result is already set");
		JCTree actualValue = value;
		if (node instanceof Expression) {
			for (int i = 0; i < ((Expression)node).getIntendedParens(); i++) {
				actualValue = treeMaker.Parens((JCExpression)actualValue);
				posParen(node, i, ((Expression)node).astParensPositions(), actualValue);
			}
		}
		result = List.of(actualValue);
		return true;
	}
	
	private void posParen(Node node, int iteration, java.util.List<Position> parenPositions, JCTree jcTree) {
		Position p = null;
		if (parenPositions.size() > iteration) p = parenPositions.get(iteration);
		int start = (p == null || p.isUnplaced() || p.getStart() < 0) ? node.getPosition().getStart() - 1 - iteration : p.getStart();
		int end = (p == null || p.isUnplaced() || p.getEnd() < 0) ? node.getPosition().getEnd() + 1 + iteration : p.getEnd();
		setPos(start, end, jcTree);
	}
	
	private boolean set(List<? extends JCTree> values) {
		if (result != null) throw new IllegalStateException("result is already set");
		result = values;
		return true;
	}
	
	private JcTreeBuilder create() {
		return new JcTreeBuilder(sourceStructures, treeMaker, table, endPosTable);
	}
	
	private static boolean hasConversionStructureInfo(Node node, String key) {
		return Position.UNPLACED == getConversionPositionInfo(node, key);
	}
	
	private final AstVisitor visitor = new ForwardingAstVisitor() {
		@Override
		public boolean visitNode(Node node) {
			throw new UnsupportedOperationException(String.format("Unhandled node '%s' (%s)", node, node.getClass().getSimpleName()));
		}
		
		@Override
		public boolean visitCompilationUnit(CompilationUnit node) {
			List<JCTree> preamble = toList(JCTree.class, node.astPackageDeclaration());
			List<JCTree> imports = toList(JCTree.class, node.astImportDeclarations());
			List<JCTree> types = toList(JCTree.class, node.astTypeDeclarations());
			
			List<JCAnnotation> annotations = List.nil();
			JCExpression pid = null;
			
			for (JCTree elem : preamble) {
				if (elem instanceof JCAnnotation) {
					annotations = annotations.append((JCAnnotation)elem);
				} else if (elem instanceof JCExpression && pid == null) {
					pid = (JCExpression) elem;
				} else {
					throw new RuntimeException("Unexpected element in preamble: " + elem);
				}
			}
			
			JCCompilationUnit topLevel = treeMaker.TopLevel(annotations, pid, imports.appendList(types));
			topLevel.endPositions = endPosTable;
			if (hasConversionStructureInfo(node, "converted")) return posSet(node, topLevel);
			
			int start = Integer.MAX_VALUE;
			int end = node.getPosition().getEnd();
			if (node.astPackageDeclaration() != null) start = Math.min(start, node.astPackageDeclaration().getPosition().getStart());
			if (!node.astImportDeclarations().isEmpty()) start = Math.min(start, node.rawImportDeclarations().first().getPosition().getStart());
			if (!node.astTypeDeclarations().isEmpty()) start = Math.min(start, node.rawTypeDeclarations().first().getPosition().getStart());
			if (start == Integer.MAX_VALUE) start = node.getPosition().getStart();
			return set(node, setPos(start, end, topLevel));
		}
		
		@Override
		public boolean visitPackageDeclaration(PackageDeclaration node) {
			List<JCTree> defs = List.nil();
			
			for (Annotation annotation : node.astAnnotations()) {
				defs = defs.append(toTree(annotation));
			}
			
			//Actual package declaration
			defs = defs.append(chain(node.astParts()));
			
			set(defs);
			return true;
		}
		
		@Override
		public boolean visitImportDeclaration(ImportDeclaration node) {
			JCExpression name = chain(node.astParts());
			if (node.astStarImport()) {
				int start, end;
				Position jcDotStarPos = getConversionPositionInfo(node, ".*");
				start = jcDotStarPos == null ? posOfStructure(node, ".", true) : jcDotStarPos.getStart();
				end = jcDotStarPos == null ? posOfStructure(node, "*", false) : jcDotStarPos.getEnd();
				name = setPos(start, end, treeMaker.Select(name, table.asterisk));
			}
			return posSet(node, treeMaker.Import(name, node.astStaticImport()));
		}
		
		@Override
		public boolean visitClassDeclaration(ClassDeclaration node) {
			int start = posOfStructure(node, "class", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end, treeMaker.ClassDef(
					(JCModifiers) toTree(node.astModifiers()),
					toName(node.astName()),
					toList(JCTypeParameter.class, node.astTypeVariables()),
					toTree(node.astExtending()),
					toList(JCExpression.class, node.astImplementing()),
					node.astBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.astBody().astMembers())
			)));
		}
		
		@Override
		public boolean visitInterfaceDeclaration(InterfaceDeclaration node) {
			JCModifiers modifiers = (JCModifiers) toTree(node.astModifiers());
			modifiers.flags |= Flags.INTERFACE;
			int start = posOfStructure(node, "interface", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end, treeMaker.ClassDef(
					modifiers,
					toName(node.astName()),
					toList(JCTypeParameter.class, node.astTypeVariables()),
					null,
					toList(JCExpression.class, node.astExtending()),
					node.astBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.astBody().astMembers())
			)));
		}
		
		public boolean visitEmptyStatement(EmptyStatement node) {
			return posSet(node, treeMaker.Skip());
		}
		
		@Override
		public boolean visitEnumDeclaration(EnumDeclaration node) {
			JCModifiers modifiers = (JCModifiers) toTree(node.astModifiers());
			modifiers.flags |= Flags.ENUM;
			int start = posOfStructure(node, "enum", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end, treeMaker.ClassDef(
					modifiers,
					toName(node.astName()),
					List.<JCTypeParameter>nil(),
					null,
					toList(JCExpression.class, node.astImplementing()),
					node.astBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.astBody())
			)));
		}
		
		@Override
		public boolean visitEnumTypeBody(EnumTypeBody node) {
			List<JCTree> constants = toList(JCTree.class, node.astConstants());
			List<JCTree> members = toList(JCTree.class, node.astMembers());
			
			set(List.<JCTree>nil().appendList(constants).appendList(members));
			return true;
		}
		
		private static final long ENUM_CONSTANT_FLAGS = Flags.PUBLIC | Flags.STATIC | Flags.FINAL | Flags.ENUM;
		
		@Override
		public boolean visitEnumConstant(EnumConstant node) {
			JCIdent parentType1 = treeMaker.Ident(toName(((EnumDeclaration)node.getParent().getParent()).astName()));
			JCIdent parentType2 = treeMaker.Ident(toName(((EnumDeclaration)node.getParent().getParent()).astName()));
			JCClassDecl body = (JCClassDecl) toTree(node.astBody());
			if (body != null) body.mods.flags |= Flags.STATIC | Flags.ENUM;
			JCNewClass newClass = treeMaker.NewClass(
					null, 
					List.<JCExpression>nil(),
					parentType1,
					toList(JCExpression.class, node.astArguments()),
					body
			);
			
			int start, end;
			Position jcNewClassPos = getConversionPositionInfo(node, "newClass");
			start = jcNewClassPos == null ? posOfStructure(node, "(", true) : jcNewClassPos.getStart();
			end = jcNewClassPos == null ? (body != null ? node.getPosition().getEnd() : posOfStructure(node, ")", false)) : jcNewClassPos.getEnd();
			boolean posIsSet = false;
			
			if (jcNewClassPos == null && start > node.astName().getPosition().getStart()) {
				setPos(start, end, newClass);
				posIsSet = true;
			}
			
			if (jcNewClassPos != null && start != node.getPosition().getStart()) {
				setPos(start, end, newClass);
				posIsSet = true;
			}
			
			if (body != null) body.pos = node.getPosition().getStart();
			if (!posIsSet && body != null) setPos(node.astBody(), newClass);
			
			JCModifiers mods = treeMaker.Modifiers(ENUM_CONSTANT_FLAGS, toList(JCAnnotation.class, node.astAnnotations()));
			if (!node.astAnnotations().isEmpty()) {
				int modStart = node.astAnnotations().first().getPosition().getStart();
				int modEnd = node.astAnnotations().last().getPosition().getEnd();
				setPos(modStart, modEnd, mods);
			}
			return posSet(node, treeMaker.VarDef(mods, toName(node.astName()), parentType2, newClass));
		}
		
		@Override
		public boolean visitNormalTypeBody(NormalTypeBody node) {
			return posSet(node, treeMaker.ClassDef(treeMaker.Modifiers(0), table.empty,
					List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), toList(JCTree.class, node.astMembers())));
		}
		
		@Override
		public boolean visitExpressionStatement(ExpressionStatement node) {
			return posSet(node, treeMaker.Exec(toExpression(node.astExpression())));
		}
		
		@Override
		public boolean visitIntegralLiteral(IntegralLiteral node) {
			if (node.astMarkedAsLong()) {
				return posSet(node, treeMaker.Literal(TypeTags.LONG, node.astLongValue()));
			}
			return posSet(node, treeMaker.Literal(TypeTags.INT, node.astIntValue()));
		}
		
		@Override
		public boolean visitFloatingPointLiteral(FloatingPointLiteral node) {
			if (node.astMarkedAsFloat()) {
				return posSet(node, treeMaker.Literal(TypeTags.FLOAT, node.astFloatValue()));
			}
			return posSet(node, treeMaker.Literal(TypeTags.DOUBLE, node.astDoubleValue()));
		}
		
		@Override
		public boolean visitBooleanLiteral(BooleanLiteral node) {
			return posSet(node, treeMaker.Literal(TypeTags.BOOLEAN, node.astValue() ? 1 : 0));
		}
		
		@Override
		public boolean visitCharLiteral(CharLiteral node) {
			return posSet(node, treeMaker.Literal(TypeTags.CHAR, (int)node.astValue()));
		}
		
		@Override
		public boolean visitNullLiteral(NullLiteral node) {
			return posSet(node, treeMaker.Literal(TypeTags.BOT, null));
		}
		
		@Override
		public boolean visitStringLiteral(StringLiteral node) {
			return posSet(node, treeMaker.Literal(TypeTags.CLASS, node.astValue()));
		}
		
		@Override public boolean visitIdentifier(Identifier node) {
			return posSet(node, treeMaker.Ident(toName(node)));
		}
		
		@Override
		public boolean visitVariableReference(VariableReference node) {
			return posSet(node, treeMaker.Ident(toName(node.astIdentifier())));
		}
		
		@Override
		public boolean visitCast(Cast node) {
			return posSet(node, treeMaker.TypeCast(toTree(node.rawTypeReference()), toExpression(node.astOperand())));
		}
		
		@Override
		public boolean visitConstructorInvocation(ConstructorInvocation node) {
			JCNewClass jcnc = setPos(node, treeMaker.NewClass(
					toExpression(node.astQualifier()), 
					toList(JCExpression.class, node.astConstructorTypeArguments()), 
					toExpression(node.astTypeReference()), 
					toList(JCExpression.class, node.astArguments()), 
					(JCClassDecl)toTree(node.astAnonymousClassBody())
			));
			if (node.astQualifier() != null) {
				int start = posOfStructure(node, "new", true);
				setPos(start, node.getPosition().getEnd(), jcnc);
			}
			return set(node, jcnc);
		}
		
		@Override
		public boolean visitSelect(Select node) {
			int start = posOfStructure(node.astIdentifier(), ".", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end,
					treeMaker.Select(toExpression(node.astOperand()), toName(node.astIdentifier()))));
		}
		
		@Override
		public boolean visitUnaryExpression(UnaryExpression node) {
			Expression operand = node.astOperand();
			UnaryOperator operator = node.astOperator();
			if (operator == UnaryOperator.UNARY_MINUS && operand instanceof IntegralLiteral) {
				JCLiteral result = (JCLiteral) toTree(operand);
				result.value = negative(result.value);
				return set(node, setPos(operand, result));
			}
			
			int start = node.getPosition().getStart();
			int end = node.getPosition().getEnd();
			
			/*
			 * The pos of "++x" is the entire thing, but the pos of "x++" is only the symbol.
			 * I guess the javac guys think consistency is overrated :(
			 */
			if (hasSourceStructures()) {
				switch (operator) {
				case POSTFIX_DECREMENT:
				case POSTFIX_INCREMENT:
					start = posOfStructure(node, node.astOperator().getSymbol(), true);
					end = posOfStructure(node, node.astOperator().getSymbol(), false);
				}
			}
			
			return set(node, setPos(start, end, treeMaker.Unary(UNARY_OPERATORS.get(operator), toExpression(operand))));
		}
		
		@Override
		public boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node) {
			int thisStart, thisEnd;
			
			Position jcThisPos = getConversionPositionInfo(node, "this");
			thisStart = jcThisPos == null ? (!node.astConstructorTypeArguments().isEmpty() ? posOfStructure(node, "<", true) : posOfStructure(node, "this", true)) : jcThisPos.getStart();
			thisEnd = jcThisPos == null ? posOfStructure(node, "this", false) : jcThisPos.getEnd();
			
			JCMethodInvocation invoke = treeMaker.Apply(
					toList(JCExpression.class, node.astConstructorTypeArguments()), 
					setPos(thisStart, thisEnd,
							treeMaker.Ident(table._this)),
					toList(JCExpression.class, node.astArguments()));
			int start, end;
			if (hasSourceStructures()) {
				start = posOfStructure(node, "(", true);
				end = posOfStructure(node, ")", false);
			} else {
				start = node.getPosition().getStart();
				end = node.getPosition().getEnd();
			}
			
			JCExpressionStatement exec = treeMaker.Exec(setPos(start, end, invoke));
			Position jcExecPos = getConversionPositionInfo(node, "exec");
			if (jcExecPos != null) {
				setPos(jcExecPos.getStart(), jcExecPos.getEnd(), exec);
			} else {
				setPos(node, exec);
			}
			
			return set(node, exec);
		}
		
		@Override
		public boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
			JCExpression methodId;
			if (node.astQualifier() == null) {
				methodId = treeMaker.Ident(table._super);
				Position ecjSuperPos = getConversionPositionInfo(node, "super");
				methodId.pos = ecjSuperPos == null ? posOfStructure(node, "super", true) : ecjSuperPos.getStart();
			} else {
				methodId = treeMaker.Select(
						toExpression(node.astQualifier()),
						table._super);
				Position ecjSuperPos = getConversionPositionInfo(node, "super");
				if (ecjSuperPos == null) {
					setPos(posOfStructure(node, ".", true), posOfStructure(node, "super", false), methodId);
				} else {
					setPos(ecjSuperPos.getStart(), ecjSuperPos.getEnd(), methodId);
				}
			}
			
			JCMethodInvocation invoke = treeMaker.Apply(
					toList(JCExpression.class, node.astConstructorTypeArguments()), 
					methodId, 
					toList(JCExpression.class, node.astArguments()));
			int start, end;
			if (hasSourceStructures()) {
				start = posOfStructure(node, "(", Integer.MAX_VALUE, true);
				end = posOfStructure(node, ")", Integer.MAX_VALUE, false);
			} else {
				start = node.getPosition().getStart();
				end = node.getPosition().getEnd();
			}
			
			JCExpressionStatement exec = treeMaker.Exec(setPos(start, end, invoke));
			Position jcExecPos = getConversionPositionInfo(node, "exec");
			if (jcExecPos == null) {
				setPos(node, exec);
			} else {
				setPos(jcExecPos.getStart(), jcExecPos.getEnd(), exec);
			}
			
			return set(node, exec);
		}
		
		@Override
		public boolean visitSuper(Super node) {
			JCTree tree;
			int start, end;
//			end = node.getPosition().getEnd();
			end = -1;
			if (node.astQualifier() != null) {
				tree = treeMaker.Select((JCExpression) toTree(node.astQualifier()), table._super);
				start = posOfStructure(node, ".", true);
				end = posOfStructure(node, "super", false);
			} else {
				tree = treeMaker.Ident(table._super);
				start = posOfStructure(node, "super", true);
			}
			
			Position jcSuperPos = getConversionPositionInfo(node, "super");
			if (jcSuperPos != null) {
				start = jcSuperPos.getStart();
				end = jcSuperPos.getEnd();
			}
			
			return set(node, setPos(start, end, tree));
		}
		
		@Override
		public boolean visitBinaryExpression(BinaryExpression node) {
			BinaryOperator operator = node.astOperator();
			int start = posOfStructure(node, node.rawOperator(), true);
			int end = node.getPosition().getEnd();
			
			if (operator == BinaryOperator.PLUS) {
				if (tryStringCombine(node)) return true;
			}
			
			JCExpression lhs = toExpression(node.astLeft());
			JCExpression rhs = toExpression(node.astRight());
			
			if (operator == BinaryOperator.ASSIGN) {
				return set(node, setPos(start, end, treeMaker.Assign(lhs, rhs)));
			}
			
			if (operator.isAssignment()) {
				return set(node, setPos(start, end, treeMaker.Assignop(BINARY_OPERATORS.get(operator), lhs, rhs)));
			}
			
			return set(node, setPos(start, end, treeMaker.Binary(BINARY_OPERATORS.get(operator), lhs, rhs)));
		}
		
		private boolean tryStringCombine(BinaryExpression node) {
			if (node.getParens() > 0) {
				;
			} else if (node.getParent() instanceof BinaryExpression) {
				try {
					if (!((BinaryExpression)node.getParent()).astOperator().isAssignment()) return false;
				} catch (AstException ignore) {
					return false;
				}
			} else if (node.getParent() instanceof InstanceOf) {
				return false;
			}
			
			java.util.List<String> buffer = Lists.newArrayList();
			BinaryExpression current = node;
			int start = Integer.MAX_VALUE;
			while (true) {
				start = Math.min(start, posOfStructure(current, "+", true));
				if (current.rawRight() instanceof StringLiteral && current.astRight().getParens() == 0) {
					buffer.add(((StringLiteral)current.rawRight()).astValue());
				} else {
					return false;
				}
				
				if (current.rawLeft() instanceof BinaryExpression) {
					current = (BinaryExpression) current.rawLeft();
					try {
						if (current.astOperator() != BinaryOperator.PLUS || current.getParens() > 0) return false;
					} catch (AstException e) {
						return false;
					}
				} else if (current.rawLeft() instanceof StringLiteral && current.astLeft().getParens() == 0) {
					buffer.add(((StringLiteral)current.rawLeft()).astValue());
					break;
				} else {
					return false;
				}
			}
			
			StringBuilder out = new StringBuilder();
			for (int i = buffer.size() - 1; i >= 0; i--) out.append(buffer.get(i));
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end, treeMaker.Literal(TypeTags.CLASS, out.toString())));
		}
		
		@Override
		public boolean visitInstanceOf(InstanceOf node) {
			int start = posOfStructure(node, "instanceof", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end,
					treeMaker.TypeTest(
							toExpression(node.astObjectReference()),
							toExpression(node.astTypeReference()))));
		}
		
		@Override
		public boolean visitInlineIfExpression(InlineIfExpression node) {
			int start = posOfStructure(node, "?", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end, treeMaker.Conditional(
					toExpression(node.astCondition()), 
					toExpression(node.astIfTrue()), 
					toExpression(node.astIfFalse()))));
		}
		
		@Override
		public boolean visitMethodInvocation(MethodInvocation node) {
			JCExpression methodId;
			if (node.astOperand() == null) {
				methodId = (JCExpression) toTree(node.astName());
			} else {
				int start = hasSourceStructures() ? posOfStructure(node, ".", true) : node.astName().getPosition().getStart();
				int end = node.astName().getPosition().getEnd();
				methodId = setPos(start, end, treeMaker.Select(
						toExpression(node.astOperand()),
						toName(node.astName())));
			}
			
			int start = posOfStructure(node, "(", true);
			int end = node.getPosition().getEnd();
			
			return set(node, setPos(start, end, treeMaker.Apply(
					toList(JCExpression.class, node.astMethodTypeArguments()), 
					methodId, 
					toList(JCExpression.class, node.astArguments())
			)));
		}
		
		@Override public boolean visitAnnotationValueArray(AnnotationValueArray node) {
			return posSet(node, treeMaker.NewArray(
					null,
					List.<JCExpression>nil(),
					toList(JCExpression.class, node.astValues())
			));
		}
		
		@Override
		public boolean visitArrayInitializer(ArrayInitializer node) {
			return posSet(node, treeMaker.NewArray(
					null,
					List.<JCExpression>nil(),
					toList(JCExpression.class, node.astExpressions())
			));
		}
		
		@Override
		public boolean visitArrayCreation(ArrayCreation node) {
			java.util.List<Integer> typeTrees = Lists.newArrayList();
			int endPosOfTypeTree = 0;
			List<JCExpression> dims = List.nil();
			for (ArrayDimension dim : node.astDimensions()) {
				JCExpression e = toExpression(dim);
				if (e == null) {
					Position p = dim.getPosition();
					typeTrees.add(p.getStart());
					endPosOfTypeTree = Math.max(endPosOfTypeTree, p.getEnd());
				} else {
					dims = dims.append(e);
				}
			}
			
			Collections.reverse(typeTrees);
			
			List<JCExpression> init;
			if (node.astInitializer() == null) {
				init = null;
			} else {
				init = toList(JCExpression.class, node.astInitializer().astExpressions());
				typeTrees.remove(typeTrees.size()-1); //javac sees this as new TYPE[] {}, with both 'new' and the last [] as structure.
			}
			
			JCExpression elementType = toExpression(node.astComponentTypeReference());
			for (Integer start : typeTrees) {
				elementType = setPos(start, endPosOfTypeTree, treeMaker.TypeArray(elementType));
			}
			return posSet(node, treeMaker.NewArray(elementType, dims, init));
		}
		
		@Override
		public boolean visitArrayDimension(ArrayDimension node) {
			return set(node, toTree(node.astDimension()));
		}
		
		@Override
		public boolean visitAssert(Assert node) {
			return posSet(node, treeMaker.Assert(toExpression(node.astAssertion()), toExpression(node.astMessage())));
		}
		
		@Override
		public boolean visitBreak(Break node) {
			return posSet(node, treeMaker.Break(toName(node.astLabel())));
		}
		
		@Override
		public boolean visitContinue(Continue node) {
			return posSet(node, treeMaker.Continue(toName(node.astLabel())));
		}
		
		private JCExpression reParen(Node node, JCExpression expr) {
			int start, end;
			
			Position jcParensPos = getConversionPositionInfo(node, "()");
			start = jcParensPos == null ? posOfStructure(node, "(", true) : jcParensPos.getStart();
			end = jcParensPos == null ? posOfStructure(node, ")", false) : jcParensPos.getEnd();
			return setPos(start, end, treeMaker.Parens(expr));
		}
		
		@Override
		public boolean visitDoWhile(DoWhile node) {
			JCExpression expr = reParen(node, toExpression(node.astCondition()));
			return posSet(node, treeMaker.DoLoop(toStatement(node.astStatement()), expr));
		}
		
		@Override
		public boolean visitFor(For node) {
			List<JCStatement> inits;
			List<JCExpressionStatement> updates;
			
			if (node.isVariableDeclarationBased()) {
				inits = toList(JCStatement.class, node.astVariableDeclaration());
			} else {
				inits = List.nil();
				for (Expression init : node.astExpressionInits()) {
					JCExpressionStatement exec = treeMaker.Exec(toExpression(init));
					Position jcExecPos = getConversionPositionInfo(init, "exec");
					if (jcExecPos == null) {
						setPos(init, exec);
					} else {
						setPos(jcExecPos.getStart(), jcExecPos.getEnd(), exec);
					}
					inits = inits.append(exec);
				}
			}
			
			updates = List.nil();
			for (Expression update : node.astUpdates()) {
				JCExpressionStatement exec = treeMaker.Exec(toExpression(update));
				Position jcExecPos = getConversionPositionInfo(update, "exec");
				if (jcExecPos == null) {
					setPos(update, exec);
				} else {
					setPos(jcExecPos.getStart(), jcExecPos.getEnd(), exec);
				}
				updates = updates.append(exec);
			}
			
			return posSet(node, treeMaker.ForLoop(inits, toExpression(node.astCondition()), updates, toStatement(node.astStatement())));
		}
		
		@Override
		public boolean visitForEach(ForEach node) {
			return posSet(node, treeMaker.ForeachLoop((JCVariableDecl) toTree(node.astVariable()), toExpression(node.astIterable()), toStatement(node.astStatement())));
		}
		
		@Override
		public boolean visitIf(If node) {
			JCExpression expr = reParen(node, toExpression(node.astCondition()));
			return posSet(node, treeMaker.If(expr, toStatement(node.astStatement()), toStatement(node.astElseStatement())));
		}
		
		@Override
		public boolean visitLabelledStatement(LabelledStatement node) {
			return posSet(node, treeMaker.Labelled(toName(node.astLabel()), toStatement(node.astStatement())));
		}
		
		@Override
		public boolean visitModifiers(Modifiers node) {
			JCModifiers mods = treeMaker.Modifiers(node.getExplicitModifierFlags(), toList(JCAnnotation.class, node.astAnnotations()));
			
			Comment javadoc = null;
			
			if (node.getParent() instanceof JavadocContainer) {
				javadoc = ((JavadocContainer)node.getParent()).astJavadoc();
			} else if (node.getParent() instanceof VariableDefinition && node.getParent().getParent() instanceof VariableDeclaration) {
				javadoc = ((VariableDeclaration)node.getParent().getParent()).astJavadoc();
			}
			
			if (javadoc != null && javadoc.isMarkedDeprecated()) mods.flags |= Flags.DEPRECATED;
			
			if (node.isEmpty() && !hasConversionStructureInfo(node, "converted")) {
				//Workaround for a javac bug; start (but not end!) gets set of an empty modifiers object,
				//but only if these represent the modifiers of a constructor or method that has type variables.
				if (
						(node.getParent() instanceof MethodDeclaration && ((MethodDeclaration)node.getParent()).astTypeVariables().size() > 0) ||
						(node.getParent() instanceof ConstructorDeclaration && ((ConstructorDeclaration)node.getParent()).astTypeVariables().size() > 0)) {
					
					mods.pos = node.getParent().getPosition().getStart();
				}
				return set(node, mods);
			} else {
				return posSet(node, mods);
			}
		}
		
		@Override
		public boolean visitKeywordModifier(KeywordModifier node) {
			return set(node, treeMaker.Modifiers(getModifier(node)));
		}
		
		@Override
		public boolean visitInstanceInitializer(InstanceInitializer node) {
			return set(node, toTree(node.astBody()));
		}
		
		@Override
		public boolean visitStaticInitializer(StaticInitializer node) {
			JCBlock block = (JCBlock) toTree(node.astBody());
			block.flags |= Flags.STATIC; 
			return posSet(node, block);
		}
		
		@Override
		public boolean visitBlock(Block node) {
			return posSet(node, treeMaker.Block(0, toList(JCStatement.class, node.astContents())));
		}
		
		@Override
		public boolean visitVariableDeclaration(VariableDeclaration node) {
			List<JCVariableDecl> list = toList(JCVariableDecl.class, node.astDefinition());
			JCVariableDecl last = list.get(list.size() -1);
			endPosTable.put(last, node.getPosition().getEnd());
			return set(list);
		}
		
		@Override
		public boolean visitVariableDefinition(VariableDefinition node) {
			JCModifiers mods = (JCModifiers) toTree(node.astModifiers());
			JCExpression vartype = toExpression(node.astTypeReference());
			
			if (node.astVarargs()) {
				mods.flags |= Flags.VARARGS;
				vartype = addDimensions(node, vartype, 1);
				Position jcEllipsisPos = getConversionPositionInfo(node, "...");
				if (jcEllipsisPos == null) {
					setPos(posOfStructure(node, "...", true), posOfStructure(node, "...", false), vartype);
				} else {
					setPos(jcEllipsisPos.getStart(), jcEllipsisPos.getEnd(), vartype);
				}
			}
			
			List<JCVariableDecl> defs = List.nil();
			for (VariableDefinitionEntry e : node.astVariables()) {
				defs = defs.append(setPos(
						e,
						treeMaker.VarDef(mods, toName(e.astName()),
								addDimensions(e, vartype, e.astArrayDimensions()), toExpression(e.astInitializer()))));
			}
			
			/* the endpos when multiple nodes are generated is after the comma for all but the last item, for some reason. */ {
				if (hasSourceStructures()) {
					for (int i = 0; i < defs.size() -1; i++) {
						endPosTable.put(defs.get(i), posOfStructure(node, ",", i, false));
					}
				}
			}
			
			if (defs.isEmpty()) throw new RuntimeException("Empty VariableDefinition node");
			set(defs);
			return true;
		}
		
		@Override
		public boolean visitAnnotationDeclaration(AnnotationDeclaration node) {
			JCModifiers modifiers = (JCModifiers) toTree(node.astModifiers());
			modifiers.flags |= Flags.INTERFACE | Flags.ANNOTATION;
			int start = posOfStructure(node, "interface", true);
			int end = node.getPosition().getEnd();
			if (hasSourceStructures()) {
				if (modifiers.pos == -1) modifiers.pos = posOfStructure(node, "@", true);
				endPosTable.put(modifiers, posOfStructure(node, "@", false));
			}
			
			return set(node, setPos(start, end, treeMaker.ClassDef(
					modifiers,
					toName(node.astName()),
					List.<JCTypeParameter>nil(),
					null,
					List.<JCExpression>nil(),
					node.astBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.astBody().astMembers())
			)));
		}
		
		@Override
		public boolean visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {
			JCMethodDecl methodDef = treeMaker.MethodDef(
					(JCModifiers)toTree(node.astModifiers()), 
					toName(node.astMethodName()), 
					toExpression(node.astReturnTypeReference()), 
					List.<JCTypeParameter>nil(),
					List.<JCVariableDecl>nil(),
					List.<JCExpression>nil(),
					null,
					toExpression(node.astDefaultValue())
			);
			
			int start = node.astMethodName().getPosition().getStart();
			int end = node.getPosition().getEnd();
			
			return set(node, setPos(start, end, methodDef));
		}
		
		@Override
		public boolean visitClassLiteral(ClassLiteral node) {
			int start = posOfStructure(node, ".", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end,
					treeMaker.Select((JCExpression) toTree(node.astTypeReference()), table._class)));
		}
		
		@Override
		public boolean visitAnnotationElement(AnnotationElement node) {
			JCExpression arg = toExpression(node.astValue());
			if (node.astName() != null) {
				arg = setPos(node.astValue(), treeMaker.Assign((JCIdent) toTree(node.astName()), arg));
			}
			return set(node, arg);
		}
		
		@Override public boolean visitAnnotation(Annotation node) {
			int start = node.getPosition().getStart();
			int end = node.getPosition().getEnd(); // Newer javacs
			//int end = node.astAnnotationTypeReference().getPosition().getEnd(); // Older javacs
			
			return set(node, setPos(start, end,
					treeMaker.Annotation(toTree(node.astAnnotationTypeReference()), toList(JCExpression.class, node.astElements()))));
		}
		
		@Override
		public boolean visitTypeReference(TypeReference node) {
			WildcardKind wildcard = node.astWildcard();
			if (wildcard == WildcardKind.UNBOUND) {
				return posSet(node, treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null));
			}
			
			JCExpression result = plainTypeReference(node);
			
			result = addWildcards(node, result, wildcard);
			result = addDimensions(node, result, node.astArrayDimensions());
			
			return set(node, result);
		}
		
		@Override
		public boolean visitArrayAccess(ArrayAccess node) {
			int start = posOfStructure(node, "[", true);
			int end = node.getPosition().getEnd();
			return set(node, setPos(start, end,
					treeMaker.Indexed(toExpression(node.astOperand()), toExpression(node.astIndexExpression()))));
		}
		
		private JCExpression addDimensions(Node node, JCExpression type, int dimensions) {
			JCExpression resultingType = type;
			for (int i = 0; i < dimensions; i++) {
				int start, end;
				int currentDim = dimensions - i - 1;
				Position jcBracketPos = getConversionPositionInfo(node, "[]" + i);
				if (jcBracketPos == null) {
					start = posOfStructure(node, "[", currentDim, true);
					end = posOfStructure(node, "]", false);
				} else {
					start = jcBracketPos.getStart();
					end = jcBracketPos.getEnd();
				}
				resultingType = setPos(start, end, treeMaker.TypeArray(resultingType));
			}
			return resultingType;
		}
		
		private JCExpression plainTypeReference(TypeReference node) {
			if (node.isPrimitive() || node.isVoid() || node.astParts().size() == 1) {
				int end = node.getPosition().getEnd();
				if (node.astArrayDimensions() > 0) {
					end = node.astParts().last().getPosition().getEnd();
				}
				if (end == node.getPosition().getStart()) end = node.getPosition().getEnd();
				
				Identifier identifier = node.astParts().first().astIdentifier();
				int typeTag = primitiveTypeTag(identifier.astValue());
				if (typeTag > 0) return setPos(node.getPosition().getStart(), end, treeMaker.TypeIdent(typeTag));
			}
			
			JCExpression current = null;
			for (TypeReferencePart part : node.astParts()) {
				JCExpression expr = (JCExpression) toTree(part);
				if (current == null) {
					current = expr;
					continue;
				}
				if (expr instanceof JCIdent) {
					current = treeMaker.Select(current, ((JCIdent)expr).name);
					setPos(posOfStructure(part, ".", true), part.getPosition().getEnd(), current);
				} else if (expr instanceof JCTypeApply) {
					JCTypeApply apply = (JCTypeApply)expr;
					apply.clazz = treeMaker.Select(current, ((JCIdent)apply.clazz).name);
					setPos(posOfStructure(part, ".", true), part.astIdentifier().getPosition().getEnd(), apply.clazz);
					current = apply;
				} else {
					throw new IllegalStateException("Didn't expect a " + expr.getClass().getName() + " in " + node);
				}
			}
			
			//TODO add a lot more tests involving 'void', apparently we're missing a bunch.
			
			return current;
		}
		
		private JCExpression addWildcards(Node node, JCExpression type, WildcardKind wildcardKind) {
			TypeBoundKind typeBoundKind;
			switch (wildcardKind) {
			case NONE:
				return type;
			case EXTENDS:
				typeBoundKind = treeMaker.TypeBoundKind(BoundKind.EXTENDS);
				Position jcExtendsPos = getConversionPositionInfo(node, "extends");
				if (jcExtendsPos == null) {
					setPos(posOfStructure(node, "extends", true), posOfStructure(node, "extends", false), typeBoundKind);
				} else {
					setPos(jcExtendsPos.getStart(), jcExtendsPos.getEnd(), typeBoundKind);
				}
				return setPos(type.pos, endPosTable.get(type), treeMaker.Wildcard(typeBoundKind, type));
			case SUPER:
				typeBoundKind = treeMaker.TypeBoundKind(BoundKind.SUPER);
				Position jcSuperPos = getConversionPositionInfo(node, "super");
				if (jcSuperPos == null) {
					setPos(posOfStructure(node, "super", true), posOfStructure(node, "super", false), typeBoundKind);
				} else {
					setPos(jcSuperPos.getStart(), jcSuperPos.getEnd(), typeBoundKind);
				}
				return setPos(type.pos, endPosTable.get(type), treeMaker.Wildcard(typeBoundKind, type));
			default:
				throw new IllegalStateException("Unexpected unbound wildcard: " + wildcardKind);
			}
		}
		
		@Override
		public boolean visitTypeReferencePart(TypeReferencePart node) {
			JCIdent ident = (JCIdent) toTree(node.astIdentifier());
			
			List<JCExpression> typeArguments = toList(JCExpression.class, node.astTypeArguments());
			if (typeArguments.isEmpty()) {
				return set(node, ident);
			} else {
				JCTypeApply typeApply = treeMaker.TypeApply(ident, typeArguments);
				Position jcOpenBracketPos = getConversionPositionInfo(node, "<");
				if (jcOpenBracketPos == null) {
					setPos(posOfStructure(node, "<", true), node.getPosition().getEnd(), typeApply);
				} else {
					setPos(jcOpenBracketPos.getStart(), node.getPosition().getEnd(), typeApply);
				}
				return set(node, typeApply);
			}
		}
		
		@Override
		public boolean visitTypeVariable(TypeVariable node) {
			return posSet(node, treeMaker.TypeParameter(toName(node.astName()), toList(JCExpression.class, node.astExtending())));
		}
		
		@Override
		public boolean visitMethodDeclaration(MethodDeclaration node) {
			JCMethodDecl methodDef = treeMaker.MethodDef(
					(JCModifiers)toTree(node.astModifiers()), 
					toName(node.astMethodName()), 
					toExpression(node.astReturnTypeReference()), 
					toList(JCTypeParameter.class, node.astTypeVariables()), 
					toList(JCVariableDecl.class, node.astParameters()), 
					toList(JCExpression.class, node.astThrownTypeReferences()), 
					(JCBlock)toTree(node.astBody()), 
					null
			);
			for (JCVariableDecl decl : methodDef.params) {
				decl.mods.flags |= Flags.PARAMETER;
			}
			
			int start = node.astMethodName().getPosition().getStart();
			int end = node.getPosition().getEnd();
			
			return set(node, setPos(start, end, methodDef));
		}
		
		@Override
		public boolean visitConstructorDeclaration(ConstructorDeclaration node) {
			JCMethodDecl constrDef = treeMaker.MethodDef(
					(JCModifiers)toTree(node.astModifiers()),
					table.init, null,
					toList(JCTypeParameter.class, node.astTypeVariables()),
					toList(JCVariableDecl.class, node.astParameters()),
					toList(JCExpression.class, node.astThrownTypeReferences()),
					(JCBlock)toTree(node.astBody()),
					null
			);
			for (JCVariableDecl decl : constrDef.params) {
				decl.mods.flags |= Flags.PARAMETER;
			}
			
			int start = node.astTypeName().getPosition().getStart();
			int end = node.getPosition().getEnd();
			
			return set(node, setPos(start, end, constrDef));
		}
		
		@Override
		public boolean visitReturn(Return node) {
			return posSet(node, treeMaker.Return(toExpression(node.astValue())));
		}
		
		@Override
		public boolean visitSwitch(Switch node) {
			List<JCCase> cases = List.nil();
			
			JCExpression currentPat = null;
			Node currentNode = null;
			List<JCStatement> stats = null;
			boolean preamble = true;
			
			for (Statement s : node.astBody().astContents()) {
				if (s instanceof Case || s instanceof Default) {
					JCExpression newPat = (s instanceof Default) ? null : toExpression(((Case)s).astCondition());
					if (preamble) {
						preamble = false;
					} else {
						cases = addCase(cases, currentPat, currentNode, stats);
					}
					stats = List.nil();
					currentPat = newPat;
					currentNode = s;
				} else {
					if (preamble) {
						throw new RuntimeException("switch body does not start with default/case.");
					}
					stats = stats.append(toStatement(s));
				}
			}
			
			if (!preamble) cases = addCase(cases, currentPat, currentNode, stats);
			
			JCExpression expr = reParen(node, toExpression(node.astCondition()));
			return posSet(node, treeMaker.Switch(expr, cases));
		}
	
		private List<JCCase> addCase(List<JCCase> cases, JCExpression currentPat, Node currentNode, List<JCStatement> stats) {
			JCStatement last = stats.last();
			int start = currentNode.getPosition().getStart();
			int end = last == null ? currentNode.getPosition().getEnd() : endPosTable.get(last);
			cases = cases.append(setPos(start, end, treeMaker.Case(currentPat, stats)));
			return cases;
		}
		
		@Override
		public boolean visitSynchronized(Synchronized node) {
			JCExpression expr = reParen(node, toExpression(node.astLock()));
			return posSet(node, treeMaker.Synchronized(expr, (JCBlock)toTree(node.astBody())));
		}
		
		@Override
		public boolean visitThis(This node) {
			JCTree tree;
			int start, end;
			end = node.getPosition().getEnd();
			if (node.astQualifier() != null) {
				tree = treeMaker.Select((JCExpression) toTree(node.astQualifier()), table._this);
				start = posOfStructure(node, ".", true);
			} else {
				tree = treeMaker.Ident(table._this);
				start = node.getPosition().getStart();
			}
			
			Position jcThisPos = getConversionPositionInfo(node, "this");
			if (jcThisPos != null) {
				start = jcThisPos.getStart();
				end = jcThisPos.getEnd();
			}
			return set(node, setPos(start, end, tree));
		}
		
		@Override
		public boolean visitTry(Try node) {
			List<JCCatch> catches = toList(JCCatch.class, node.astCatches());
			
			return posSet(node, treeMaker.Try((JCBlock) toTree(node.astBody()), catches, (JCBlock) toTree(node.astFinally())));
		}
		
		@Override
		public boolean visitCatch(Catch node) {
			JCVariableDecl exceptionDeclaration = (JCVariableDecl) toTree(node.astExceptionDeclaration());
			exceptionDeclaration.getModifiers().flags |= Flags.PARAMETER;
			return posSet(node, treeMaker.Catch(exceptionDeclaration, (JCBlock) toTree(node.astBody())));
		}
		
		@Override
		public boolean visitThrow(Throw node) {
			return posSet(node, treeMaker.Throw(toExpression(node.astThrowable())));
		}
		
		@Override
		public boolean visitWhile(While node) {
			JCExpression expr = reParen(node, toExpression(node.astCondition()));
			return posSet(node, treeMaker.WhileLoop(expr, toStatement(node.astStatement())));
		}
		
		@Override
		public boolean visitEmptyDeclaration(EmptyDeclaration node) {
			if (node.getParent() instanceof CompilationUnit) {
				return posSet(node, treeMaker.Skip());
			}
			return set(node, posNone(treeMaker.Block(0, List.<JCStatement>nil())));
		}
	};
	
	static final BiMap<UnaryOperator, Integer> UNARY_OPERATORS = ImmutableBiMap.<UnaryOperator, Integer>builder()
			.put(UnaryOperator.BINARY_NOT, JCTree.COMPL)
			.put(UnaryOperator.LOGICAL_NOT, JCTree.NOT)
			.put(UnaryOperator.UNARY_PLUS, JCTree.POS)
			.put(UnaryOperator.PREFIX_INCREMENT, JCTree.PREINC)
			.put(UnaryOperator.UNARY_MINUS, JCTree.NEG)
			.put(UnaryOperator.PREFIX_DECREMENT, JCTree.PREDEC)
			.put(UnaryOperator.POSTFIX_INCREMENT, JCTree.POSTINC)
			.put(UnaryOperator.POSTFIX_DECREMENT, JCTree.POSTDEC)
			.build();
	
	static final BiMap<BinaryOperator, Integer> BINARY_OPERATORS = ImmutableBiMap.<BinaryOperator, Integer>builder()
			.put(BinaryOperator.PLUS_ASSIGN, JCTree.PLUS_ASG)
			.put(BinaryOperator.MINUS_ASSIGN, JCTree.MINUS_ASG)
			.put(BinaryOperator.MULTIPLY_ASSIGN, JCTree.MUL_ASG)
			.put(BinaryOperator.DIVIDE_ASSIGN, JCTree.DIV_ASG)
			.put(BinaryOperator.REMAINDER_ASSIGN, JCTree.MOD_ASG)
			.put(BinaryOperator.AND_ASSIGN, JCTree.BITAND_ASG)
			.put(BinaryOperator.XOR_ASSIGN, JCTree.BITXOR_ASG)
			.put(BinaryOperator.OR_ASSIGN, JCTree.BITOR_ASG)
			.put(BinaryOperator.SHIFT_LEFT_ASSIGN, JCTree.SL_ASG)
			.put(BinaryOperator.SHIFT_RIGHT_ASSIGN, JCTree.SR_ASG)
			.put(BinaryOperator.BITWISE_SHIFT_RIGHT_ASSIGN, JCTree.USR_ASG)
			.put(BinaryOperator.LOGICAL_OR, JCTree.OR)
			.put(BinaryOperator.LOGICAL_AND, JCTree.AND)
			.put(BinaryOperator.BITWISE_OR, JCTree.BITOR)
			.put(BinaryOperator.BITWISE_XOR, JCTree.BITXOR)
			.put(BinaryOperator.BITWISE_AND, JCTree.BITAND)
			.put(BinaryOperator.EQUALS, JCTree.EQ)
			.put(BinaryOperator.NOT_EQUALS, JCTree.NE)
			.put(BinaryOperator.GREATER, JCTree.GT)
			.put(BinaryOperator.GREATER_OR_EQUAL, JCTree.GE)
			.put(BinaryOperator.LESS, JCTree.LT)
			.put(BinaryOperator.LESS_OR_EQUAL, JCTree.LE)
			.put(BinaryOperator.SHIFT_LEFT, JCTree.SL)
			.put(BinaryOperator.SHIFT_RIGHT, JCTree.SR)
			.put(BinaryOperator.BITWISE_SHIFT_RIGHT, JCTree.USR)
			.put(BinaryOperator.PLUS, JCTree.PLUS)
			.put(BinaryOperator.MINUS, JCTree.MINUS)
			.put(BinaryOperator.MULTIPLY, JCTree.MUL)
			.put(BinaryOperator.DIVIDE, JCTree.DIV)
			.put(BinaryOperator.REMAINDER, JCTree.MOD)
			.build();
	
	static final BiMap<String, Integer> PRIMITIVES = ImmutableBiMap.<String, Integer>builder()
			.put("byte", TypeTags.BYTE)
			.put("char", TypeTags.CHAR)
			.put("short", TypeTags.SHORT)
			.put("int", TypeTags.INT)
			.put("long", TypeTags.LONG)
			.put("float", TypeTags.FLOAT)
			.put("double", TypeTags.DOUBLE)
			.put("boolean", TypeTags.BOOLEAN)
			.put("void", TypeTags.VOID)
			.build();
	
	static int primitiveTypeTag(String typeName) {
		Integer primitive = PRIMITIVES.get(typeName);
		return primitive == null ? 0 : primitive;
	}
	
	private long getModifier(KeywordModifier keyword) {
		return keyword.asReflectModifiers();
	}
	
	private JCExpression chain(Iterable<Identifier> parts) {
		JCExpression previous = null;
		for (Identifier part : parts) {
			Name next = toName(part);
			if (previous == null) {
				previous = setPos(part, treeMaker.Ident(next));
			} else {
				previous = setPos(posOfStructure(part, ".", true), part.getPosition().getEnd(), treeMaker.Select(previous, next));
			}
		}
		return previous;
	}
	
	private int posOfStructure(Node node, String structure, boolean atStart) {
		return posOfStructure(node, structure, atStart ? 0 : Integer.MAX_VALUE, atStart);
	}
	
	private boolean hasSourceStructures() {
		return sourceStructures != null && !sourceStructures.isEmpty();
	}
	
	private int posOfStructure(Node node, String structure, int idx, boolean atStart) {
		int start = node.getPosition().getStart();
		
		if (sourceStructures != null && sourceStructures.containsKey(node)) {
			for (SourceStructure struct : sourceStructures.get(node)) {
				if (structure.equals(struct.getContent())) {
					start = atStart ? struct.getPosition().getStart() : struct.getPosition().getEnd();
					if (idx-- <= 0) break;
				}
			}
		}
		
		return start;
	}
	
	private static Object negative(Object value) {
		Number num = (Number)value;
		if (num instanceof Integer) return -num.intValue();
		if (num instanceof Long) return -num.longValue();
		if (num instanceof Float) return -num.floatValue();
		if (num instanceof Double) return -num.doubleValue();
		
		throw new IllegalArgumentException("value should be an Integer, Long, Float or Double, not a " + value.getClass().getSimpleName());
	}
	
	private boolean posSet(Node node, JCTree jcTree) {
		return set(node, setPos(node, jcTree));
	}
	
	private <T extends JCTree> T posNone(T jcTree) {
		jcTree.pos = -1;
		endPosTable.remove(jcTree);
		return jcTree;
	}
	
	private <T extends JCTree> T setPos(Node node, T jcTree) {
		return setPos(node.getPosition().getStart(), node.getPosition().getEnd(), jcTree);
	}
	
	private <T extends JCTree> T setPos(int start, int end, T jcTree) {
		jcTree.pos = start;
		endPosTable.put(jcTree, end);
		return jcTree;
	}
}

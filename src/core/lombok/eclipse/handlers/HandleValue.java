/*
 * Copyright (C) 2012 The Project Lombok Authors.
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
package lombok.eclipse.handlers;

import lombok.AccessLevel;
import lombok.experimental.Value;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.Value} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleValue extends EclipseAnnotationHandler<Value> {
	public void handle(AnnotationValues<Value> annotation, Annotation ast, EclipseNode annotationNode) {
		Value ann = annotation.getInstance();
		EclipseNode typeNode = annotationNode.up();
		
		TypeDeclaration typeDecl = null;
		if (typeNode.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) typeNode.get();
		int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;
		boolean notAClass = (modifiers &
				(ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation | ClassFileConstants.AccEnum)) != 0;
		
		if (typeDecl == null || notAClass) {
			annotationNode.addError("@Value is only supported on a class.");
			return;
		}
		
		//Careful: Generate the public static constructor (if there is one) LAST, so that any attempt to
		//'find callers' on the annotation node will find callers of the constructor, which is by far the
		//most useful of the many methods built by @Value. This trick won't work for the non-static constructor,
		//for whatever reason, though you can find callers of that one by focusing on the class name itself
		//and hitting 'find callers'.
		
		new HandleGetter().generateGetterForType(typeNode, annotationNode, AccessLevel.PUBLIC, true);
		new HandleWither().generateWitherForType(typeNode, annotationNode, AccessLevel.PUBLIC, true);
		new HandleEqualsAndHashCode().generateEqualsAndHashCodeForType(typeNode, annotationNode);
		new HandleToString().generateToStringForType(typeNode, annotationNode);
		new HandleFieldDefaults().generateFieldDefaultsForType(typeNode, annotationNode, AccessLevel.PRIVATE, true, true);
		new HandleConstructor().generateAllArgsConstructor(typeNode, AccessLevel.PUBLIC, ann.staticConstructor(), true, ast);
	}
}

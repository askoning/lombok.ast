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
package lombok.ast;

import java.util.NoSuchElementException;

public interface StrictListAccessor<T extends Node, P extends Node> extends Iterable<T> {
	P up();
	Node owner();
	void clear();
	boolean isEmpty();
	int size();
	T first();
	T last();
	boolean contains(Node source);
	P migrateAllFrom(StrictListAccessor<? extends T, ?> otherList);
	P addToStart(T... node);
	P addToEnd(T... node);
	P addBefore(Node ref, T... node);
	P addAfter(Node ref, T... node);
	void replace(Node source, T replacement) throws NoSuchElementException;
	void remove(Node source) throws NoSuchElementException;
	RawListAccessor<T, P> asRawAccessor();
}
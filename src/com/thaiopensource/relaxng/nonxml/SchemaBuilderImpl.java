package com.thaiopensource.relaxng.nonxml;

import java.util.Stack;
import java.util.Vector;

public class SchemaBuilderImpl implements SchemaBuilder {
  private Stack stack = new Stack();
  private String datatypeLibrary = null;
  private boolean haveInherit = false;
  private Vector bindings = new Vector();

  private Element pop() {
    return (Element)stack.pop();
  }

  private void push(Element e) {
    stack.push(e);
  }

  private Element peek() {
    return (Element)stack.peek();
  }

  public void startPrefixBinding(String prefix, String uri) {
    bindings.addElement(prefix);
    bindings.addElement(uri);
  }

  public void endPrefixBinding() {
  }

  public Element finish(String preferredNamespace) {
    Element e = pop();
    if (!stack.empty())
      throw new RuntimeException("stack should be empty but is not");
    e.addAttribute("xmlns", "http://relaxng.org/ns/structure/0.9");
    if (datatypeLibrary != null && !"".equals(datatypeLibrary))
      e.addAttribute("datatypeLibrary", datatypeLibrary);
    if (preferredNamespace != null && !haveInherit)
      e.addAttribute("ns", preferredNamespace);
    for (int i = 0; i < bindings.size(); i += 2)
      e.addAttribute("xmlns:" + (String)bindings.elementAt(i),
		     (String)bindings.elementAt(i + 1));
    return e.simplify();
  }

  // pattern pattern => choice
  public void choice() {
    Element e2 = pop();
    Element e1 = pop();
    if (!e1.getName().equals("choice"))
      e1 = new Element("choice", e1);
    e1.addChild(e2);
    push(e1);
  }
  // pattern pattern => interleave
  public void interleave() {
    Element e2 = pop();
    Element e1 = pop();
    if (!e1.getName().equals("interleave"))
      e1 = new Element("interleave", e1);
    e1.addChild(e2);
    push(e1);
  }
  // pattern pattern => group
  public void group() {
    Element e2 = pop();
    Element e1 = pop();
    if (!e1.getName().equals("group"))
      e1 = new Element("group", e1);
    e1.addChild(e2);
    push(e1);
  }
  // nameClass pattern => element
  public void element(Annotations a) {
    Element pattern = pop();
    Element nameClass = pop();
    push(merge(new Element("element", a, nameClass), "group", pattern));
  }
  // nameClass pattern => attribute
  public void attribute(Annotations a) {
    Element pattern = pop();
    Element nameClass = pop();
    Element result = new Element("attribute", a, nameClass);
    result.addChild(pattern);
    push(result);
  }
  // => empty
  public void empty(Annotations a) {
    push(new Element("empty", a));
  }
  // => notAllowed
  public void notAllowed(Annotations a) {
    push(new Element("notAllowed", a));
  }
  // => text
  public void text(Annotations a) {
    push(new Element("text", a));
  }
  // pattern => list
  public void list(Annotations a) {
    push(new Element("list", a, pop()));
  }
  // pattern => mixed
  public void mixed(Annotations a) {
    push(new Element("mixed", a, pop()));
  }
  // pattern => zeroOrMore
  public void zeroOrMore() {
    push(new Element("zeroOrMore", pop()));
  }
  // pattern => oneOrMore
  public void oneOrMore() {
    push(new Element("oneOrMore", pop()));
  }
  // pattern => optional
  public void optional() {
    push(new Element("optional", pop()));
  }
  // pattern => pattern
  public void annotate(Annotations a) {
    if (a.empty())
      return;
    Element child = pop();
    String childName = child.getName();
    if ((childName.equals("choice")
	 || childName.equals("group")
	 || childName.equals("interleave")
	 || childName.equals("oneOrMore")
	 || childName.equals("zeroOrMore")
	 || childName.equals("optional"))
	&& !child.hasAttributes()
	&& !child.hasElementAnnotations()) {
      Element tem = new Element(childName, a);
      tem.mergeChildren(child);
      push(tem);
    }
    else
      push(new Element("group", a, child));
  }
  // => grammar
  public void grammar(Annotations a) {
    push(new Element("grammar", a));
  }
  // grammar pattern => grammar
  public void define(String name, Annotations a, int combine) {
    Element def = new Element("define", a);
    def.addAttribute("name", name);
    addCombineAttribute(def, combine);
    merge(def, "group", pop());
    peek().addChild(def);
  }
  // grammar pattern => grammar
  public void start(Annotations a, int combine) {
    Element def = new Element("start", a, pop());
    addCombineAttribute(def, combine);
    peek().addChild(def);
  }
  private void addCombineAttribute(Element e, int combine) {
    switch (combine) {
    case COMBINE_INTERLEAVE:
      e.addAttribute("combine", "interleave");
      break;
    case COMBINE_CHOICE:
      e.addAttribute("combine", "choice");
      break;
    }
  }
  // grammar => grammar
  public void include(String uri, String baseUri, String ns, Annotations a) {
    Element e = new Element("include", a);
    e.addAttribute("href", uri);
    addNs(e, ns);
    push(e);
  }
  // grammar => grammar
  public void finishInclude() {
    Element inc = pop();
    peek().addChild(inc);
  }
  // grammar => grammar
  public void finishGrammar() { }
  // => ref
  public void ref(String name, Annotations a) {
    Element e = new Element("ref", a);
    e.addAttribute("name", name);
    push(e);
  }
  // => parentRef
  public void parentRef(String name, Annotations a) {
    Element e = new Element("parentRef", a);
    e.addAttribute("name", name);
    push(e);
  }
  // => value
  public void value(String datatypeLibrary,
		    String type,
		    String value,
		    String ns,
		    Annotations a) {
    Element e = new Element("value", a);
    if (!"".equals(datatypeLibrary))
      addNs(e, ns);
    if (!"".equals(datatypeLibrary) || !"token".equals(type)) {
      e.addAttribute("type", type);
      addDatatypeLibrary(e, datatypeLibrary);
    }
    e.addChild(value);
    push(e);
  }
  // => data
  public void data(String datatypeLibrary,
		   String type,
		   Annotations a) {
    Element e = new Element("data", a);
    addDatatypeLibrary(e, datatypeLibrary);
    e.addAttribute("type", type);
    push(e);
  }
  // data => data
  public void param(String name, Annotations a, String value) {
    Element e = new Element("param", a);
    e.addAttribute("name", name);
    e.addChild(value);
    peek().addChild(e);
  }
  // data pattern => data
  public void dataExcept() {
    Element e = merge(new Element("except"), "choice", pop());
    peek().addChild(e);
  }
  // data => data
  public void finishData() { }
  // => anyName
  public void anyName() {
    push(new Element("anyName"));
  }
  // => pattern
  public void externalRef(String uri, String baseUri, String ns,
			  Annotations a) {
    Element e = new Element("externalRef", a);
    e.addAttribute("href", uri);
    addNs(e, ns);
    push(e);
  }
  // nameClass => anyNameExcept
  public void anyNameExcept() {
    push(new Element("anyName", merge(new Element("except"), "choice", pop())));
  }
  // => nsName
  public void nsName(String ns) {
    Element e = new Element("nsName");
    addNs(e, ns);
    push(e);
  }
  // nameClass => nsNameExcept
  public void nsNameExcept(String ns) {
    Element e = new Element("nsName");
    addNs(e, ns);
    e.addChild(merge(new Element("except"), "choice", pop()));
    push(e);
  }
  // => name
  public void name(String ns, String localName) {
    Element e = new Element("name");
    addNs(e, ns);
    e.addChild(localName);
    push(e);
  }
  // => name
  public void prefixedName(String ns, String prefixedName) {
    Element e = new Element("name");
    e.addChild(prefixedName);
    push(e);
  }
  // nameClass nameClass => nameClassChoice
  public void nameClassChoice() {
    choice();
  }

  private void addNs(Element e, String ns) {
    if (ns != null)
      e.addAttribute("ns", ns);
    else
      haveInherit = true;
  }

  private void addDatatypeLibrary(Element e, String uri) {
    if (datatypeLibrary == null)
      datatypeLibrary = uri;
    else if (!datatypeLibrary.equals(uri))
      e.addAttribute("datatypeLibrary", uri);
  }

  private Element merge(Element parent, String op, Element child) {
    if (child.getName().equals(op)
	&& !child.hasAttributes()
	&& !child.hasElementAnnotations())
      parent.mergeChildren(child);
    else
      parent.addChild(child);
    return parent;
  }
}

package com.thaiopensource.relaxng.nonxml;

import java.util.Vector;
import java.io.Writer;
import java.io.IOException;

public class Element {
  public String name;
  public Vector attributes = new Vector();
  public Vector children = new Vector();
  public Vector childAnnotations = null;
  public Vector followingAnnotations = null;

  Element(String name) {
    this.name = name;
  }

  Element(String name, Annotations a) {
    this.name = name;
    for (int i = 0, len = a.getAttributeCount(); i < len; i++)
      addAttribute(a.getAttributeName(i), a.getAttributeValue(i));
    for (int i = 0, len = a.getChildElementCount(); i < len; i++)
      addChildAnnotation(a.getChildElement(i));
    for (int i = 0, len = a.getFollowingElementCount(); i < len; i++)
      addFollowingAnnotation(a.getFollowingElement(i));
  }

  Element(String name, Element child) {
    this(name);
    addChild(child);
  }

  Element(String name, Annotations a, Element child) {
    this(name, a);
    addChild(child);
  }

  String getName() {
    return name;
  }

  boolean hasAttributes() {
    return attributes.size() > 0;
  }

  boolean hasElementAnnotations() {
    return childAnnotations != null || followingAnnotations != null;
  }

  void addAttribute(String name, String value) {
    attributes.addElement(name);
    attributes.addElement(value);
  }
  void addChild(Element e) {
    if (e == null)
      throw new NullPointerException();
    children.addElement(e);
  }
  void mergeChildren(Element e) {
    for (int i = 0, n = e.children.size(); i < n; i++)
      children.addElement(e.children.elementAt(i));
  }
  void addChild(String s) {
    children.addElement(s);
  }

  void addFollowingAnnotation(String s) {
    if (s == null)
      throw new NullPointerException();
    if (followingAnnotations == null)
      followingAnnotations = new Vector();
    followingAnnotations.addElement(s);
  }

  void addChildAnnotation(String s) {
    if (s == null)
      throw new NullPointerException();
    if (childAnnotations == null)
      childAnnotations = new Vector();
    childAnnotations.addElement(s);
  }

  public void dump(Writer w, String charset) throws IOException {
    xmlDeclaration(w, charset);
    String lineSep = System.getProperty("line.separator");
    w.write(lineSep);
    dump(w, lineSep, 0);
  }
  
  private static void xmlDeclaration(Writer w, String charset) throws IOException {
    w.write("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>");
  }

  private static void indent(Writer w, int level) throws IOException {
    for (int i = 0; i < level; i++)
      w.write("  ");
  }

  private void dump(Writer w, String lineSep, int level) throws IOException {
    indent(w, level);
    w.write('<');
    w.write(name);
    int n = attributes.size();
    for (int i = 0; i < n; i += 2) {
      w.write(' ');
      w.write((String)attributes.elementAt(i));
      w.write('=');
      w.write('"');
      attributeValue(w, (String)attributes.elementAt(i + 1));
      w.write('"');
    }
    n = children.size();
    if (n == 0 && childAnnotations == null) {
      w.write("/>");
    }
    else {
      w.write('>');
      if (children.elementAt(0) instanceof String)
	data(w, (String)children.elementAt(0), lineSep);
      else {
	w.write(lineSep);
	if (childAnnotations != null) {
	  for (int i = 0, len = childAnnotations.size(); i < len; i++) {
	    indent(w, level + 1);
	    markup(w, (String)childAnnotations.elementAt(i), lineSep);
	    w.write(lineSep);
	  }
	}
	for (int i = 0; i < n; i++)
	  ((Element)children.elementAt(i)).dump(w, lineSep, level + 1);
	indent(w, level);
      }
      w.write("</");
      w.write(name);
      w.write('>');
    }
    w.write(lineSep);
    if (followingAnnotations != null) {
      for (int i = 0, len = followingAnnotations.size(); i < len; i++) {
	indent(w, level);
	markup(w, (String)followingAnnotations.elementAt(i), lineSep);
	w.write(lineSep);
      }
    }
  }

  private static void markup(Writer w, String s, String lineSep) throws IOException {
    int i = 0;
    for (;;) {
      int j = s.indexOf('\n', i);
      if (j < 0)
	break;
      if (j > i)
	w.write(s.substring(i, j));
      i = j + 1;
      w.write(lineSep);
    }
    w.write(i == 0 ? s : s.substring(i));
  }

  private static void data(Writer w, String s, String lineSep) throws IOException {
    int n = s.length();
    for (int i = 0; i < n; i++) {
      switch (s.charAt(i)) {
      case '<':
	w.write("&lt;");
	break;
      case '>':
	w.write("&gt;");
	break;
      case '&':
	w.write("&amp;");
	break;
      case '\r':
	w.write("&#xD;");
	break;
      case '\n':
	w.write(lineSep);
	break;
      default:
	w.write(s.charAt(i));
	break;
      }
    }
  }

  private static void attributeValue(Writer w, String s) throws IOException {
    int n = s.length();
    for (int i = 0; i < n; i++) {
      switch (s.charAt(i)) {
      case '<':
	w.write("&lt;");
	break;
      case '"':
	w.write("&quot;");
	break;
      case '&':
	w.write("&amp;");
	break;
      case '\t':
	w.write("&#9;");
	break;
      case '\r':
	w.write("&#xD;");
	break;
      case '\n':
	w.write("&#xA;");
	break;
      default:
	w.write(s.charAt(i));
	break;
      }
    }
  }

  Element simplify() {
    simplify(null);
    return this;
  }

  private void simplify(String ns) {
    String inheritNs = ns;
    for (int i = 0; i < attributes.size(); i += 2) {
      if (attributes.elementAt(i).equals("ns")) {
	inheritNs = (String)attributes.elementAt(i + 1);
	if (inheritNs.equals(ns)) {
	  attributes.removeElementAt(i);
	  attributes.removeElementAt(i);
	}
	break;
      }
    }
    if (canLiftName(inheritNs)) {
      this.addAttribute("name",
			(String)((Element)children.elementAt(0)).children.elementAt(0));
      children.removeElementAt(0);
    }
    for (int i = 0; i < children.size(); i++) {
      Object child = children.elementAt(i);
      if (child instanceof Element)
	((Element)child).simplify(inheritNs);
    }
  }

  private boolean canLiftName(String ns) {
    if (children.size() == 0)
      return false;
    if (!(children.elementAt(0) instanceof Element))
      return false;
    Element firstChild = (Element)children.elementAt(0);
    if (!firstChild.name.equals("name"))
      return false;
    boolean isElement;
    if (this.name.equals("element"))
      isElement = true;
    else if (this.name.equals("attribute"))
      isElement = false;
    else
      return false;
    if (((String)firstChild.children.elementAt(0)).indexOf(':') >= 0)
      return true;
    if (firstChild.attributes.size() == 0)
      return isElement;
    if (firstChild.attributes.size() > 2
	|| !firstChild.attributes.elementAt(0).equals("ns"))
      return false;
    return firstChild.attributes.elementAt(1).equals(isElement ? ns : "");
  }
}


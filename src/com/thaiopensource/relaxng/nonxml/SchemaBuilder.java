package com.thaiopensource.relaxng.nonxml;

interface SchemaBuilder {
  static final int COMBINE_NONE = 0;
  static final int COMBINE_CHOICE = 1;
  static final int COMBINE_INTERLEAVE = 2;

  // pattern pattern => choice
  void choice();
  // pattern pattern => interleave
  void interleave();
  // pattern pattern => group
  void group();
  // nameClass pattern => element
  void element(Annotations a);
  // nameClass pattern => attribute
  void attribute(Annotations a);
  // => empty
  void empty(Annotations a);
  // => notAllowed
  void notAllowed(Annotations a);
  // => text
  void text(Annotations a);
  // pattern => list
  void list(Annotations a);
  // pattern => mixed
  void mixed(Annotations a);
  // pattern => zeroOrMore
  void zeroOrMore();
  // pattern => oneOrMore
  void oneOrMore();
  // pattern => optional
  void optional();
  // pattern => pattern
  void annotate(Annotations a);
  // => grammar
  void grammar(Annotations a);
  // grammar pattern => grammar
  void define(String name, Annotations a, int combine);
  // grammar pattern => grammar
  void start(Annotations a, int combine);
  // grammar => grammar
  void include(String uri, String baseUri, String ns, Annotations a);
  // grammar => grammar
  void finishInclude();
  // grammar => grammar
  void finishGrammar();
  // => ref
  void ref(String name, Annotations a);
  // => parentRef
  void parentRef(String name, Annotations a);
  // => value
  void value(String datatypeLibrary,
	     String type,
	     String value,
	     String ns,
	     Annotations a);
  // => data
  void data(String datatypeLibrary,
	    String type,
	    Annotations a);
  // data => data
  void param(String name, Annotations a, String value);
  // data pattern => data
  void dataExcept();
  // data => data
  void finishData();
  // => pattern
  void externalRef(String uri, String baseUri, String ns, Annotations a);
  // => anyName
  void anyName();
  // nameClass => anyNameExcept
  void anyNameExcept();
  // => nsName
  void nsName(String ns);
  // nameClass => nsNameExcept
  void nsNameExcept(String ns);
  // => name
  void name(String ns, String localName);
  // => name
  void prefixedName(String ns, String prefixedName);
  // nameClass nameClass => nameClassChoice
  void nameClassChoice();

  void startPrefixBinding(String prefix, String uri);
  void endPrefixBinding();
}

package com.thaiopensource.relaxng.nonxml;

interface Annotations {
  boolean empty();
  int getAttributeCount();
  String getAttributeName(int i);
  String getAttributeNamespace(int i);
  String getAttributeValue(int i);
  int getFollowingElementCount();
  String getFollowingElement(int i);
  int getChildElementCount();
  String getChildElement(int i);
}

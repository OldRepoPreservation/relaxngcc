package com.thaiopensource.relaxng.nonxml;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;

public class Driver {
  public static void main(String args[]) throws ParseException, IOException, SyntaxException {
    String charset = null;
    if (args.length == 0 || args.length > 2) {
      System.err.println("usage: java com.thaiopensource.relaxng.nonxml.Driver input [encoding]");
      return;
    }
    InputStream in = new BufferedInputStream(new FileInputStream(args[0]));
    if (args.length > 1)
      charset = args[1];
    else
      charset = "ISO-8859-1";
    NonXmlSyntax parser = new NonXmlSyntax(new InputStreamReader(in, charset));
    SchemaBuilderImpl sb = new SchemaBuilderImpl();
    parser.Input(sb);
    Writer w = new OutputStreamWriter(System.out, charset);
    sb.finish(parser.getPreferredNamespace()).dump(w, charset);
    w.flush();
  }
}

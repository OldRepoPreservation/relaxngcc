package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * Formats a code DOM and produces a source code.
 * 
 * This object controls the formatting (such as indentation)
 * and handles language-specific construct.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class Formatter {
    public Formatter( Writer _writer ) {
        this.writer = _writer;
    }
    
    /** Output will be sent to this object. */
    private final Writer writer;
    
    
    
    
    /** current indent. */
    private int indent;
    
    /** Indent. */
    public Formatter in()  { indent++; return this; }
    /** Unindent. */
    public Formatter out() { indent--; return this; }



    /**
     * Clears the current line and this method also prints indentation.
     * 
     * @return "this."
     */
    public Formatter nl() throws IOException {
        writer.write(System.getProperty("line.separator"));
        newLine = true;
        return this;
    }
    
    /** This flag is set to true after a line is cleared. */
    private boolean newLine = false;
    
    /**
     * Outputs a new token.
     * 
     * Sooner or later all the methods come down to this method
     * for output.
     * 
     * @return "this"
     */
    public Formatter p( String token ) throws IOException {
        if(token.length()!=0) {
            if(newLine) {
                // print indent
                for(int i=0; i<indent; i++)
                    writer.write("    ");
                newLine = false;
            } else {
                if(spaceNeeded(token.charAt(0)))
                    writer.write(' ');
            }
            writer.write(token);
            lastChar = token.charAt(token.length()-1);
        }
        return this;
    }
    
    /**
     * Last character of the last token.
     * Used by the spaceIfNeeded method.
     */
    private char lastChar;
    
    /**
     * Outputs a whitespace if it is necessary to
     * separate the new token from the previous token.
     */
    private boolean spaceNeeded( char ch ) throws IOException {
        if(ch==';')                     return false;
        if(ch=='=' || lastChar=='=')    return true;
        
        if(Character.isJavaIdentifierPart(ch)
        && Character.isJavaIdentifierPart(lastChar))
            return true;
        
        if(ch=='{') return true;
        
        return false;
    }



    
    /**
     * Outputs a new token.
     * 
     * @return "this."
     */
    public Formatter p( char ch ) throws IOException {
        return p(new String(new char[]{ch}));
    }
    
    
    
    /** Prints expression. */
    public Formatter express( Expression exp ) throws IOException {
        exp.express(this);
        return this;
    }
    
    /** Prints a statement. */
    public Formatter state( Statement s ) throws IOException {
        s.state(this);
        return this;
    }
    
    /** Outputs a type object. */
    public final Formatter type( TypeDescriptor t ) throws IOException {
        t.writeTo(this);
        return this;
    }
    
    /** Outputs VariableDeclaration as a declaration. */
    public final Formatter declare( VariableDeclaration v ) throws IOException {
        v.declare(this);
        return this;
    }
    
    /** Outputs a language specific string. */
    public abstract Formatter write( LanguageSpecificString str ) throws IOException;
    
    /**
     * Marks the end of a statement.
     */
    public Formatter eos() throws IOException {
        p(';');
        return this;
    }
    
    

}

package relaxngcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import relaxngcc.builder.CodeWriter;
import relaxngcc.builder.NullableChecker;
import relaxngcc.builder.AutomatonBuilder;
import relaxngcc.builder.ScopeCollector;
import relaxngcc.builder.ScopeInfo;
import relaxngcc.grammar.Grammar;
import relaxngcc.grammar.PatternFunction;
import relaxngcc.grammar.Scope;
import relaxngcc.runtime.NGCCRuntime;

/**
 * Keeps information about the global setting effective
 * across the entire grammar.
 * <p>
 * A "grammar" in RELAX NG could be nested inside another grammar,
 * so we need a bit different name.
 * 
 * This class needs a better name.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NGCCGrammar {
    public NGCCGrammar( Grammar g, String pkgName, String runtime,
            String globalImport, String globalBody ) {
        this.grammar = g;
        this.packageName = pkgName;
        this.runtimeType = runtime;
        this.globalImportDecls = globalImport;
        this.globalBody = globalBody;
    }
    
    /** Parsed grammar. */
    public final Grammar grammar;
    
    /**
     * Package name to which generated classes should go.
     * The value will be the empty string if the classes will
     * be generated into the root package.
     */
    public final String packageName;
    
    /**
     * Type of the user-defined Runtime class.
     * Always a non-null valid string.
     */
    private final String runtimeType;

    /**
     * Gets the class name of the runtime type
     * alone without the package name.
     */
    public String getRuntimeTypeShortName() {
        int idx = runtimeType.lastIndexOf('.');
        if(idx<0)   return runtimeType;
        else        return runtimeType.substring(idx+1);
    }
    public String getRuntimeTypeFullName() {
        return runtimeType;
    }
    
    
    /** Map from {@link Scope} to {@link ScopeInfo} objects. */
    private final Map scopeInfos = new HashMap();
    
    public ScopeInfo getScopeInfo( Scope scope ) {
        return (ScopeInfo)scopeInfos.get(scope);
    }
    public Iterator iterateScopeInfos() {
        return scopeInfos.values().iterator();
    }
    
    
    /** globally effective import statements. */
    public final String globalImportDecls;
    
    /** globally effective &lt;java-body> statements. */
    public final String globalBody;
    
    
//
//
// Operations
//
//
    public void buildAutomaton() {
        // collect all scopes inside the grammar
        Set scopes = (Set)grammar.apply(new ScopeCollector());
        Iterator it;
        
        // create empty ScopeInfos
        it = scopes.iterator();
        while(it.hasNext()) {
            Scope s = (Scope)it.next();
            scopeInfos.put( s, new ScopeInfo(this,s) );
        }
        
        // build ScopeInfos
        it = scopes.iterator();
        while(it.hasNext()) {
            Scope s = (Scope)it.next();
            AutomatonBuilder.build(this,getScopeInfo(s));
        }
            
        NullableChecker.computeNullability(this);
    }
    
    //for debug
	public void dump(PrintStream strm) {
		Iterator it = scopeInfos.values().iterator();
		while (it.hasNext()) {
			ScopeInfo sci = (ScopeInfo) it.next();
			sci.dump(strm);
		}
	}
    
    /** generates automaton gif files. */
    public void dumpAutomata(File outDir) {
        try {
            Iterator it = scopeInfos.values().iterator();
            while(it.hasNext()) {
                ScopeInfo sci = (ScopeInfo)it.next();
                sci.dumpAutomaton(new File(outDir,sci.getClassName()+".gif"));
            }
        } catch( Exception e ) {
            System.out.println("failed to generate gif files");
            e.printStackTrace();
        }
    }

    /**
     * Generates the source code.
     * 
     * @return
     *      true if files are in fact generated.
     */
    public boolean output( Options opt, long sourceTimestamp ) throws IOException
    {
        boolean generated = false;
        
/*        //step1 datatypes
        if(_Options.style==Options.STYLE_TYPED_SAX && _DataTypes.size() > 0)
        {
            PrintStream s = new PrintStream(new FileOutputStream(
                new File(_Options.targetdir, "DataTypes.java")));
            printDataTypes(s);
        }*/
        //step2 scopes
        Iterator it = scopeInfos.values().iterator();
        while(it.hasNext()) {
            ScopeInfo si = (ScopeInfo)it.next();
            
//            if(!si.isLambda() && !si.isInline()) {
            CodeWriter w = new CodeWriter(this, si, opt);
            File f = new File(opt.targetdir, si.getClassName() + ".java");
            if(!f.exists() || f.lastModified()<=sourceTimestamp || !opt.smartOverwrite) {
                generated = true;
                f.delete();
                PrintStream out = new PrintStream(new FileOutputStream(f));
                w.output(out);
                out.close();
                f.setReadOnly();
            }
//            }
        }
        
        // copy runtime code if necessary
        if(opt.usePrivateRuntime && generated) {
            copyResourceAsFile("NGCCHandler.java",opt);
            copyResourceAsFile("AttributesImpl.java",opt);
            copyResourceAsFile("NGCCRuntime.java",opt);
        }
        
        return generated;
    }
    
    /**
     * Copies a resource file to the target directory.
     */
    private void copyResourceAsFile( String file, Options opt ) throws IOException {
        File out = new File(opt.targetdir,file);
        
        BufferedReader in = new BufferedReader(
            new InputStreamReader(NGCCRuntime.class.getResourceAsStream(file)));
            
        PrintWriter os = new PrintWriter(new FileWriter(out));
        byte[] buf = new byte[256];
        
        String s;
        while((s=in.readLine())!=null) {
            if(s.startsWith("package ")) {
                if(packageName.length()!=0)
                    s = "package "+packageName+";";
                else
                    s="";
            }
            os.println(s);
        }
        
        in.close();
        os.close();
    }
}


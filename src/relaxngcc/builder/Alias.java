package relaxngcc.builder;

import relaxngcc.codedom.CDType;

/**
 * Alias that will be generated as a field.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Alias
{
    public Alias(CDType t, String n) { name=n; type=t; }

    public final CDType type;
    public final String name;
}


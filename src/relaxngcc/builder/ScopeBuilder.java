/*
 * ScopeBuilder.java
 *
 * Created on 2001/08/04, 22:15
 */

package relaxngcc.builder;
import java.util.Stack;
import java.util.Iterator;

import relaxngcc.automaton.Alphabet;
import relaxngcc.automaton.State;
import relaxngcc.automaton.Transition;
import relaxngcc.NGCCGrammar;
import relaxngcc.MetaDataType;
import relaxngcc.NGCCUtil;
import relaxngcc.NGCCException;
import relaxngcc.grammar.*;

/**
 * A ScopeBuilder constructs an automaton from a given root Element of scope in target grammar. 
 */
public class ScopeBuilder implements PatternFunction
{
/*	public static final int TYPE_ROOT = 0;
	public static final int TYPE_NORMAL = 1;
	public static final int TYPE_LAMBDA = 2;
	public static final int TYPE_COMBINED_CHOICE = 3;
	public static final int TYPE_COMBINED_INTERLEAVE = 4;
	
	private int _Type;
	private NGCCElement _Root;
	private ScopeInfo _ScopeInfo;
	private NGCCGrammar _Grammar;
	private Stack _Namespaces;
	private boolean _ExpandInline;
*/

	private int _ThreadCount;

    /**
     * Used to give order numbers to EnterAttribute alphabets.
     */
    private int _OrderCounter;
    
    /** actions are added to this buffer until it is processed */
    private StringBuffer preservedAction = new StringBuffer();

/*	
	//constructor must be called from following static methods
	private ScopeBuilder(int type, NGCCGrammar grm, String location, NGCCElement root)
	{
		_Type = type;
		_Root = root;
		_ThreadCount = 0;
		_Grammar = grm;
        
		_Namespaces = new Stack();
		
        String ns = root.getAttribute("ns",grm.getDefaultNSURI());
		_Namespaces.push(ns);
		
        _ExpandInline = "true".equals(root.attributeNGCC("inline",null));
        
		_ScopeInfo = new ScopeInfo(grm, _Type, location, _ExpandInline);
		_ScopeInfo.addNSURI(ns);
	}*/ 
	
	/** Creates new ScopeBuilder */
/*    public static ScopeBuilder create(NGCCGrammar grm, String location, NGCCElement root)
	{
		ScopeBuilder inst = new ScopeBuilder(TYPE_NORMAL, grm, location, root);
		
		String nameForTargetLang = root.attributeNGCC("class",
            root.getAttribute("name","RelaxNGCC_Result"));
        
        setScopeParameters(grm,inst,root,nameForTargetLang);
		return inst;
    }
    
    private static void setScopeParameters(
        NGCCGrammar grm, ScopeBuilder inst,NGCCElement root,String nameForTargetLang) {
        
        inst._ScopeInfo.setParameters(
            root.getAttribute("name"),
            nameForTargetLang,
            root.attributeNGCC("package", grm.getPackageName()),
            root.attributeNGCC("access",""),
            root.attributeNGCC("return-type",null),
            root.attributeNGCC("return-value","this"),
            root.attributeNGCC("params",null));
    }
*/	
	/** Creates new ScopeBuilder */
/*    public static ScopeBuilder createAsRoot(NGCCGrammar grm, String location, NGCCElement root)
	{
		ScopeBuilder inst = new ScopeBuilder(TYPE_ROOT, grm, location, root);
		
        setScopeParameters(grm,inst,root,
            root.attributeNGCC("class","RelaxNGCC_Result"));
		return inst;
    }
    
    //creates with specified lambda name
    public static ScopeBuilder createAsLambda(NGCCGrammar grm, NGCCElement root, String location, String lambda_name)
	{
		ScopeBuilder inst = new ScopeBuilder(TYPE_LAMBDA, grm, location, root);
		
		inst._ScopeInfo.setParameters(lambda_name,
            root.attributeNGCC("class",""),
            null, "", null, "this",null);
		return inst;
    }
*/

/*
	public void extend(NGCCElement otherDefine) throws NGCCException
	{
		String combineType = otherDefine.getAttribute("combine");
		if(_Type==TYPE_NORMAL)
		{
			_Type=combineType.equals("interleave")? TYPE_COMBINED_INTERLEAVE : TYPE_COMBINED_CHOICE;
			TemporaryElement newroot = new TemporaryElement(combineType);
			NGCCNodeList nl = _Root.getChildNodes();
			for(int i=0; i<nl.getLength(); i++)
				if(nl.item(i)!=null) newroot.addChild(nl.item(i));
			nl = otherDefine.getChildNodes();
			for(int i=0; i<nl.getLength(); i++)
				if(nl.item(i)!=null) newroot.addChild(nl.item(i));
			_Root = newroot;
		}
		else if(_Type==TYPE_COMBINED_CHOICE || _Type==TYPE_COMBINED_INTERLEAVE)
		{
			NGCCNodeList nl = otherDefine.getChildNodes();
			for(int i=0; i<nl.getLength(); i++)
				((TemporaryElement)_Root).addChild(nl.item(i));
		}
		else
			throw new NGCCException("expanding unknown type(" + _Type + ") scope is impossible");
	}
	
	public String getName() { return _ScopeInfo.getName(); }
	public ScopeInfo getScopeInfo() { return _ScopeInfo; }
*/	
	
    /** Builds ScopeInfo. */
    public static void build( NGCCGrammar grammar, ScopeInfo scope ) {
        new ScopeBuilder(grammar,scope).build();
    }
    
    private ScopeBuilder( NGCCGrammar grammar, ScopeInfo scope ) {
        this.grammar = grammar;
        this._ScopeInfo = scope;
    }
    
    private final NGCCGrammar grammar;
    private final ScopeInfo _ScopeInfo;
    
	public void build() {
		ctx = new ScopeBuildingContext();
		//starts from final state
	    destination = createState(null);
		destination.setAcceptable(true);

        State initial = (State)_ScopeInfo.scope.getPattern().apply(this);		
//		State initial = null;
//        if(_Type==TYPE_NORMAL || _Type==TYPE_ROOT)
//    		initial = traverseNodeList(_Root.getChildNodes(), ctx, finalstate);
//		else
//			initial = processRelaxNGNode(_Root, ctx, finalstate);
		
        _ScopeInfo.setThreadCount(_ThreadCount);
        // TODO: don't we need to reset the preservedAction variable? - Kohsuke
		_ScopeInfo.setInitialState(initial,
            (preservedAction.length()!=0)?
                _ScopeInfo.createAction(preservedAction):
                null);
        
        _ScopeInfo.copyAttributeHandlers();
        
        _ScopeInfo.minimizeStates();
	}
	
/*    
	private State traverseNodeList(NGCCNodeList nl, ScopeBuildingContext ctx, State destination)
	{
		int len = nl.getLength();
		for(int index=len-1; index>=0; index--)
		{
			NGCCElement child = nl.item(index);
			if(child==null) continue;
			
			destination = processNode(child, ctx, destination);
		}
		return destination;
	}
	private State processNode(NGCCElement child, ScopeBuildingContext ctx, State destination)
	{
		String uri  = child.getNamespaceURI();

		if(uri.equals(NGCCGrammar.NGCC_NSURI))
		{
			processRelaxNGCCNode(child);
		}
		else if(uri.equals(NGCCGrammar.RELAXNG_NSURI))
		{
			String name = child.getLocalName();
            //process lambda scope
            if(!name.equals("ref") && child.hasAttributeNGCC("class"))
            {
                String tempname = _Grammar.createLambdaName();
				ScopeBuilder b = ScopeBuilder.createAsLambda(_Grammar, child, _ScopeInfo.getLocation(), tempname);
                _Grammar.addLambdaScope(b);
                ScopeInfo info = b.getScopeInfo();
                _ScopeInfo.addChildScope(info);
                State head = createState(child, ctx);
                head.addTransition(createTransition(
                    new Alphabet.Ref(info,_OrderCounter++), destination));
                destination = head;
				
				b.buildAutomaton();
            }
			else
				destination = processRelaxNGNode(child, ctx, destination);
		}
		return destination;
	}
	private void processRelaxNGCCNode(NGCCElement child)
	{
		String name = child.getLocalName();
		if(name.equals("java"))
		{
			String code = child.getFullText();
			if(code!=null)
                preservedAction.append(code);
		}
		else if(name.equals("java-import"))
			_ScopeInfo.appendHeaderSection(child.getFullText());
		else if(name.equals("java-body"))
		{
			_ScopeInfo.appendBody(child.getFullText());
		}
	}
	
	private State processRelaxNGNode(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		String name = exp.getLocalName();
		if(name.equals("element"))
			destination = processElement(exp, ctx, destination);
		else if(name.equals("attribute"))
			destination = processAttribute(exp, ctx, destination);
		else if(name.equals("data"))
			destination = processData(exp, ctx, destination);
		else if(name.equals("text"))
			destination = processText(exp, ctx, destination);
		else if(name.equals("empty"))
			destination = processEmpty(exp, ctx, destination);
		else if(name.equals("notAllowed"))
			destination = processNotAllowed(exp, ctx, destination);
		else if(name.equals("group"))
		{
			destination = traverseNodeList(exp.getChildNodes(), ctx, destination); //group doesn't need special care
			addAction(destination,true);
		}
		else if(name.equals("interleave"))
			destination = processInterleave(exp, ctx, destination);
		else if(name.equals("choice"))
			destination = processChoice(exp, ctx, destination);
		else if(name.equals("oneOrMore"))
			destination = processOneOrMore(exp, ctx, destination);
		else if(name.equals("zeroOrMore"))
			destination = processZeroOrMore(exp, ctx, destination);
		else if(name.equals("optional"))
			destination = processOptional(exp, ctx, destination);
		else if(name.equals("ref"))
			destination = processRef(exp, ctx, destination);
		else if(name.equals("list"))
			destination = processList(exp, ctx, destination);
		else if(name.equals("value"))
			destination = processValue(exp, ctx, destination);
		else if(name.equals("name") || name.equals("nsName") || name.equals("anyName"))
			destination = destination;
		else
			System.err.println("[Warning] Unsupported RELAX NG element '" + name + "' is found in " + _ScopeInfo.getLocation() + ".");
		return destination;
	}
*/

/*	
	private State processElement(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		String ns = exp.hasAttribute("ns")? exp.getAttribute("ns") : null;
		if(ns!=null) _Namespaces.push(ns);
		
		String element_name = exp.getAttribute("name");
		NameClass nc;
		if(element_name.length()==0)
			nc = NameClass.fromNameClassElement(_ScopeInfo, exp.getFirstChild(), (String)_Namespaces.peek());
		else
			nc = NameClass.fromElementElement(_ScopeInfo, exp, (String)_Namespaces.peek());
		
		State tail = createState(exp, ctx);
		Transition te = createTransition(new Alphabet.LeaveElement(nc), destination);
		addAction(te,true);
		if(ctx.getInterleaveBranchRoot()!=null) te.setEnableState(ctx.getInterleaveBranchRoot());
		tail.addTransition(te);
		
		ScopeBuildingContext newctx = new ScopeBuildingContext(ctx);
		newctx.setInterleaveBranchRoot(null);
		State middle = traverseNodeList(exp.getChildNodes(), newctx, tail);
		State head = createState(exp, ctx);
		Transition ts = createTransition(new Alphabet.EnterElement(nc), middle);
		addAction(ts,true);
		if(ctx.getInterleaveBranchRoot()!=null) ts.setDisableState(ctx.getInterleaveBranchRoot());
		head.addTransition(ts);
		
		if(ns!=null) _Namespaces.pop();
		
		return head;
	}
*/
    private ScopeBuildingContext ctx;
    private State destination;
    
    public Object element( ElementPattern pattern ) {
        NameClass nc = pattern.name;
        
        State tail = createState(pattern);
        Transition te = createTransition(new Alphabet.LeaveElement(nc), destination);
        addAction(te,true);
        if(ctx.getInterleaveBranchRoot()!=null) te.setEnableState(ctx.getInterleaveBranchRoot());
        tail.addTransition(te);
        
        // start a new context
        ScopeBuildingContext oldContext = ctx;
        ctx = new ScopeBuildingContext(ctx);
        
        ctx.setInterleaveBranchRoot(null);

        // process descendants
        destination = tail;
        State middle = (State)pattern.body.apply(this);
        
        oldContext = ctx;
        
        State head = createState(pattern);
        Transition ts = createTransition(new Alphabet.EnterElement(nc), middle);
        addAction(ts,true);
        if(ctx.getInterleaveBranchRoot()!=null) ts.setDisableState(ctx.getInterleaveBranchRoot());
        head.addTransition(ts);
        
        return head;
    }

/*    
	private State processAttribute(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		String attr_name = exp.getAttribute("name");
		String ns = exp.hasAttribute("ns")? exp.getAttribute("ns") : "";
		
		NameClass nc;
		if(attr_name.length()==0)
			nc = NameClass.fromNameClassElement(_ScopeInfo, exp.getChildNodes().item(0), ns);
		else
			nc = NameClass.fromElementElement(_ScopeInfo, exp, ns);

        // I think I broke the code related to interleave handling.
        // I just don't know how to fix them.  - Kohsuke
        
        State tail = createState(exp, ctx);
        Transition te = createTransition(new Alphabet.LeaveAttribute(nc),
            destination /*createState(exp,ctx)*//*);
        addAction(te,true);
        tail.addTransition(te);
        
        State middle = traverseNodeList(exp.getChildNodes(), ctx/*newctx*//*, tail);
        
        State head = createState(exp, ctx);
        Transition ts = createTransition(
            new Alphabet.EnterAttribute(nc,_OrderCounter++),
            middle);
        addAction(ts,true);
//??        if(ctx.getInterleaveBranchRoot()!=null) ts.setDisableState(ctx.getInterleaveBranchRoot());

        // always treat attributes as optional,
        // otherwise we cannot properly handle things like:
        // <optional>
        //   <attribute> ..A1.. </attribute>
        // </optional>
        // <optional>
        //   <attribute> ..A2.. </attribute>
        // </optional>
        destination.addTransition(ts);
        return destination;
/*
        head.addTransition(ts);
		return head;
*//*
	}
*/
    public Object attribute( AttributePattern pattern ) {
        NameClass nc = pattern.name;
        State orgdest = destination;

        // I think I broke the code related to interleave handling.
        // I just don't know how to fix them.  - Kohsuke
        
        State tail = createState(pattern);
        Transition te = createTransition(new Alphabet.LeaveAttribute(nc),
            destination /*createState(exp,ctx)*/);
        addAction(te,true);
        tail.addTransition(te);
  
        destination = tail;
        State middle = (State)pattern.body.apply(this);      
//        State middle = traverseNodeList(exp.getChildNodes(), ctx/*newctx*/, tail);
        
        State head = createState(pattern);
        Transition ts = createTransition(
            new Alphabet.EnterAttribute(nc,_OrderCounter++),
            middle);
        addAction(ts,true);
//??        if(ctx.getInterleaveBranchRoot()!=null) ts.setDisableState(ctx.getInterleaveBranchRoot());

        // always treat attributes as optional,
        orgdest.addTransition(ts);
        return orgdest;
    }
/*
	private State processData(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		MetaDataType mdt = _Grammar.addDataType(exp);
		return processData(exp, ctx, destination, mdt);
	}
	private State processText(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		return processData(exp, ctx, destination, MetaDataType.STRING);
	}
    private State processData(NGCCElement exp, ScopeBuildingContext ctx, State destination, MetaDataType mdt)
    {
        String alias = exp.attributeNGCC("alias",null);
        if(alias!=null)   _ScopeInfo.addAlias(alias, mdt.getXSTypeName());
        
        State result = createState(exp, ctx);
        Transition t = createTransition(new Alphabet.DataText(mdt, alias), destination);
        addAction(t,false);
        result.addTransition(t);
        return result;
    }
*/
    public Object data( DataPattern pattern ) {
        State result = createState(pattern);
        if(pattern.alias!=null) _ScopeInfo.addUserDefinedAlias(pattern.alias,"String");
        Transition t = createTransition(
            new Alphabet.DataText(pattern.type,pattern.alias), destination);
        addAction(t,false);
        result.addTransition(t);
        return result;
    }
    
/*	private State processEmpty(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		return destination;
	}
	private State processNotAllowed(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		return createState(exp, ctx); //this returning State is not reachable
	}
*/
    public Object empty( EmptyPattern pattern ) {
        return destination;
    }
    public Object notAllowed( NotAllowedPattern pattern ) {
        // return a non-reachable state
        return createState(pattern);
    }

/*
	private State processValue(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
        String alias = exp.attributeNGCC("alias",null);
        
		if(alias!=null)	_ScopeInfo.addAlias(alias, "string");
        
		State result = createState(exp, ctx);
		Transition t = createTransition(new Alphabet.ValueText(exp.getFullText(), alias), destination);
		addAction(t,false);
		result.addTransition(t);
		return result;
	}
*/
    public Object value( ValuePattern pattern ) {
        if(pattern.alias!=null) _ScopeInfo.addUserDefinedAlias(pattern.alias,"String");
        
        State result = createState(pattern);
        Transition t = createTransition(
            new Alphabet.ValueText(pattern.value, pattern.alias), destination);
        addAction(t,false);
        result.addTransition(t);
        return result;
    }

/*
	private State processList(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
        String alias = exp.attributeNGCC("alias",null);
        
        if(alias!=null) {
			_ScopeInfo.addAlias(alias, "string");
			State result = createState(exp, ctx);
			Transition t = createTransition(
                new Alphabet.DataText(MetaDataType.STRING, alias), destination);
			addAction(t,true);
			result.addTransition(t);
			return result;
		} else {
			destination.setListMode(State.LISTMODE_OFF);
			State head = traverseNodeList(exp.getChildNodes(), ctx, destination);
			head.setListMode(State.LISTMODE_ON);
			return head;
		}
	}
*/
    public Object list( ListPattern pattern ) {
        if(pattern.alias!=null) {
            // don't treat this list as a structured text.
	        _ScopeInfo.addUserDefinedAlias(pattern.alias,"String");
	        
            State result = createState(pattern);
	        Transition t = createTransition(
                new Alphabet.DataText(
                    new MetaDataType("string"),
                    pattern.alias), destination);
	        addAction(t,false);
	        result.addTransition(t);
	        return result;
        } else {
            // treat this list as a structured text
            destination.setListMode(State.LISTMODE_OFF);
            State head = (State)pattern.p.apply(this);
            head.setListMode(State.LISTMODE_ON);
            return head;
        }
    }
/*    
	private State processChoice(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		NGCCNodeList nl = exp.getChildNodes();
		int len = nl.getLength();
		addAction(destination,true);
		State head = createState(exp, ctx);
		for(int index=len-1; index>=0; index--)
		{
			NGCCElement child = nl.item(index);
			if(child==null) continue;
			
            State member = processNode(child, ctx, destination);
            addAction(member,true);
			head.mergeTransitions(member);
            
            if(member.isAcceptable()) {
                // TODO: we need to copy exit actions from the member state
                // to the head state, but what if there already are some exit
                // actions?
                // this would happen for cases like
                // <choice>
                //   <group>
                //     <optional>...</optoinal>
                //     <cc:java> AAA </cc:java>
                //   </group>
                //   <group>
                //     <optional>...</optoinal>
                //     <cc:java> BBB </cc:java>
                //   </group>
                // </choice>
                //
                // this is a variation of ambiguity which we need to
                // detect.
                head.setAcceptable(true);
            }
		}
		return head;
	}
*/
    public Object javaBlock( JavaBlock block ) {
        preservedAction.append(block.code);
        return destination;
    }
    
    public Object group( GroupPattern pattern ) {
        // build automaton in a reverse order.
        // TODO: how about actions?
        destination = (State)pattern.p2.apply(this);
        return               pattern.p1.apply(this);
    }
    
    public Object choice( ChoicePattern pattern ) {
        
        State dest = destination;
        addAction(destination,true);
        
        State head = (State)pattern.p1.apply(this);
        
        destination = dest;
        State member = (State)pattern.p2.apply(this);
        
        head.mergeTransitions(member);
        
        if(member.isAcceptable()) {
            // TODO: we need to copy exit actions from the member state
            // to the head state, but what if there already are some exit
            // actions?
            // this would happen for cases like
            // <choice>
            //   <group>
            //     <optional>...</optoinal>
            //     <cc:java> AAA </cc:java>
            //   </group>
            //   <group>
            //     <optional>...</optoinal>
            //     <cc:java> BBB </cc:java>
            //   </group>
            // </choice>
            //
            // this is a variation of ambiguity which we need to
            // detect.
            head.setAcceptable(true);
        }
        
        return head;
    }
	
/*
	private State processInterleave(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		NGCCNodeList nl = exp.getChildNodes();
		addAction(destination,true);
		State head = createState(exp, ctx);
		ScopeBuildingContext newctx = new ScopeBuildingContext(ctx);
		newctx.setInterleaveBranchRoot(head);
		
		for(int index=nl.getLength()-1; index>=0; index--)
		{
			NGCCElement child = nl.item(index);
			if(child==null) continue;
			String uri = child.getNamespaceURI();
			if(uri.equals(NGCCGrammar.NGCC_NSURI))
			{
				processRelaxNGCCNode(child);
			}
			else if(uri.equals(NGCCGrammar.RELAXNG_NSURI))
			{
				newctx.setCurrentThreadIndex(_ThreadCount++);
				State meetingspot = createState(child, newctx);
				meetingspot.setMeetingDestination(destination);
				head.mergeTransitions(processNode(child, newctx, meetingspot));
				destination.addStateForWait(meetingspot);
			}
		}
		addAction(head,true);
		return head;
	}
*/
    public Object interleave( InterleavePattern pattern ) {
        State tail = destination;
        ScopeBuildingContext oldContext = ctx;
        
        addAction(destination,true);
        
        State head = createState(pattern);
        ctx = new ScopeBuildingContext(ctx);
        ctx.setInterleaveBranchRoot(head);

        tail.addStateForWait(processInterleaveBranch(pattern.p1,head));
        tail.addStateForWait(processInterleaveBranch(pattern.p2,head));
        
        addAction(head,true);
        ctx = oldContext;
        return head;
    }

    private State processInterleaveBranch( Pattern child, State head ) {
        ctx.setCurrentThreadIndex(_ThreadCount++);
        State meetingspot = createState(child);
        meetingspot.setMeetingDestination(destination);
        
        destination = meetingspot;
        head.mergeTransitions( (State)child.apply(this) );
        
        return meetingspot;
    }

/*
	private State processOneOrMore(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		addAction(destination,true);
		State head = traverseNodeList(exp.getChildNodes(), ctx, destination);
		addAction(head,true); //addAction must be before mergeTransition
		destination.mergeTransitions(head);
		return head;
	}
*/
    public Object oneOrMore(OneOrMorePattern pattern) {
        State tail = destination;
        addAction(destination,true);
        State head = (State)pattern.p.apply(this);
        addAction(head,true); //addAction must be before mergeTransition
        tail.mergeTransitions(head);
        return head;
    }
/*
	private State processZeroOrMore(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
        ScopeInfo.Action action_last = null;
        
        if(preservedAction.length()!=0)
            action_last = _ScopeInfo.createAction(preservedAction);
        // TODO: put those actions into the same Action object if applicable.
        
		addAction(destination,true);
		State head = traverseNodeList(exp.getChildNodes(), ctx, destination);
		addAction(head,true);

        State tmp = createState(exp,ctx);
        tmp.mergeTransitions(destination);
        
        destination.mergeTransitions(head);
        head.mergeTransitions(tmp);
        
        if(destination.isAcceptable()) {
            head.addActionsOnExit(destination.getActionsOnExit());
            head.setAcceptable(true);
        }
        // TODO: I suppose we also need to copy list state?
        
		return head;
	}

	private State processOptional(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
        addAction(destination,true);
        
//        ScopeInfo.Action action_last = null;
//        if(preservedAction.length()!=0)
//            action_last = _ScopeInfo.createAction(preservedAction);
        
        State tmp = createState(exp,ctx);
        tmp.mergeTransitions(destination);
            
        // any transition that leaves the destination state could be modified
        // while we process our descendant patterns.
        // therefore, we cannot simply do
        //      head.mergeTransitions(destination,action_last)
        // at the end of the function. Instead, we need to clone them
        // temporaily, so that changes made inside the traverseNodeList won't
        // affect us.
		State head = traverseNodeList(exp.getChildNodes(), ctx, destination);
		addAction(head,true);
        
        head.mergeTransitions(tmp);
        if(destination.isAcceptable()) {
            head.addActionsOnExit(destination.getActionsOnExit());
            head.setAcceptable(true);
        }
        return head;
	}
*/

/*
	private State processRef(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		ScopeBuilder target = _Grammar.getScopeBuilderByName(exp.getAttribute("name"));
		if(target._ExpandInline)
		{
			addAction(destination,true);
			State head = traverseNodeList(target._Root.getChildNodes(), ctx, destination);
			addAction(head,true);
			return head;
		}
		else
		{
//			ScopeInfo.Action action = null;
//            if(preservedAction.length()!=0)
//                action = _ScopeInfo.createAction(preservedAction);
            
			State head = createState(exp, ctx);
            
			String alias = exp.attributeNGCC("alias",null);
			if(alias!=null)
				_ScopeInfo.addUserDefinedAlias(alias,
                    target._ScopeInfo.getReturnType());
			
			Transition t = createTransition(new Alphabet.Ref(
                target.getScopeInfo(), alias,
                exp.attributeNGCC("with-params",null),_OrderCounter++),
                destination);
			head.addTransition(t);

            // add action as epilogue because code should be executed
            // *after* the transition is performed.
            addAction(t,false);
            
            
// this is a bug. even if the target is nullable, we need to call it
// because the parent scope is expecting to call it.
// it is the child scope's responsibility to revert to parent ASAP.
//			if(target.nullable()) head.mergeTransitions(destination, action);
			return head;
		}
	}
*/
    public Object ref( RefPattern pattern ) {
//      ScopeInfo.Action action = null;
//        if(preservedAction.length()!=0)
//            action = _ScopeInfo.createAction(preservedAction);
        
        State head = createState(pattern);
        
        ScopeInfo targetScope = grammar.getScopeInfo(pattern.target);
        
        String alias = pattern.param.alias;
        if(alias!=null)
            _ScopeInfo.addUserDefinedAlias(alias,
                targetScope.scope.getParam().returnType);
        
        Transition t = createTransition(new Alphabet.Ref(
            targetScope, alias, pattern.param.withParams, _OrderCounter++),
            destination);
        head.addTransition(t);

        // add action as epilogue because code should be executed
        // *after* the transition is performed.
        addAction(t,false);
        
        return head;
    }
    
    public Object scope( Scope scope ) {
        // we don't cross <ref> boundary, so this shouldn't be executed at all.
        throw new InternalError();
    }
	
    
	private State createState(Pattern source/*read from field --, ScopeBuildingContext ctx*/)
	{
		State s = new State(_ScopeInfo, ctx.getCurrentThreadIndex(), _ScopeInfo.getStateCount(), source);
		_ScopeInfo.addState(s);
		return s;
	}
	private Transition createTransition(Alphabet key, State destination)
	{
		Transition t = new Transition(key, destination);
		return t;
	}
	private void addAction(Transition t,boolean prologue)
	{
		if(preservedAction.length()!=0)
		{
            ScopeInfo.Action action = _ScopeInfo.createAction(preservedAction);
            preservedAction = new StringBuffer();
            
            if(prologue)    t.insertPrologueAction(action);
            else            t.insertEpilogueAction(action);
		}
	}
    /**
     * Adds the specified action as a prologue/epilogue action
     * to all the transitions that leave the given state.
     */
	private void addAction(State s,boolean prologue)
	{
        if(preservedAction.length()!=0) {
            ScopeInfo.Action act = _ScopeInfo.createAction(preservedAction);
            preservedAction = new StringBuffer();
            
			Iterator it = s.iterateTransitions();
			while(it.hasNext()) {
                Transition t = (Transition)it.next();
                if(prologue)    t.insertPrologueAction(act);
                else            t.insertEpilogueAction(act);
            }
            				
			s.addActionOnExit(act);
		}
	}
}

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
import relaxngcc.dom.NGCCElement;
import relaxngcc.dom.TemporaryElement;
import relaxngcc.dom.NGCCNodeList;

/**
 * A ScopeBuilder constructs an automaton from a given root Element of scope in target grammar. 
 */
public class ScopeBuilder
{
	public static final int TYPE_ROOT = 0;
	public static final int TYPE_NORMAL = 1;
	public static final int TYPE_LAMBDA = 2;
	public static final int TYPE_COMBINED_CHOICE = 3;
	public static final int TYPE_COMBINED_INTERLEAVE = 4;
	
	public static final int NULLABLE_UNKNOWN = -1;
	public static final int NULLABLE_FALSE = 0;
	public static final int NULLABLE_TRUE = 1;
	
	private int _Type;
	private NGCCElement _Root;
	private ScopeInfo _ScopeInfo;
	private NGCCGrammar _Grammar;
	private Stack _Namespaces;
	private boolean _ExpandInline;
	private int _Nullable;
	private int _ThreadCount;

    /**
     * Used to give order numbers to EnterAttribute alphabets.
     */
    private int _OrderCounter;
    
    /** actions are added to this buffer until it is processed */
    private StringBuffer preservedAction = new StringBuffer();
	
	//constructor must be called from following static methods
	private ScopeBuilder(int type, NGCCGrammar grm, String location, NGCCElement root)
	{
		_Type = type;
		_Root = root;
		_ThreadCount = 0;
		_Grammar = grm;
		_Nullable = NULLABLE_UNKNOWN;
        
		_Namespaces = new Stack();
		
        String ns = root.getAttribute("ns",grm.getDefaultNSURI());
		_Namespaces.push(ns);
		
        _ExpandInline = "true".equals(root.attributeNGCC("inline",null));
        
		_ScopeInfo = new ScopeInfo(grm, _Type, location, _ExpandInline);
		_ScopeInfo.addNSURI(ns);
	} 
	
	public boolean nullable() { return _Nullable==NULLABLE_TRUE; }
	
	/** Creates new ScopeBuilder */
    public static ScopeBuilder create(NGCCGrammar grm, String location, NGCCElement root)
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
	
	/** Creates new ScopeBuilder */
    public static ScopeBuilder createAsRoot(NGCCGrammar grm, String location, NGCCElement root)
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
	
	public void determineNullable() throws NGCCException
	{
		if(_Nullable != NULLABLE_UNKNOWN) return;
		NGCCNodeList nl = _Root.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			NGCCElement e = nl.item(i);
			if(e==null || !e.getNamespaceURI().equals(NGCCGrammar.RELAXNG_NSURI)) continue;
			_Nullable = determineNullable(e);
			_ScopeInfo.setNullable(_Nullable==NULLABLE_TRUE);
			return;
		}
		throw new NGCCException("failed to determine the scope[" + _ScopeInfo.getName() + "] is nullable or not");
	}
	private int determineNullable(NGCCElement elem) throws NGCCException
	{
		String name = elem.getLocalName();
		
		if(name.equals("element") || name.equals("attribute") || name.equals("data") || name.equals("text") || name.equals("value") || name.equals("notAllowed"))
			return NULLABLE_FALSE;
		else if(name.equals("zeroOrMore") || name.equals("optional") || name.equals("empty"))
			return NULLABLE_TRUE;
		else if(name.equals("oneOrMore") || name.equals("list") || name.equals("name") || name.equals("nsName") || name.equals("anyName"))
		{
			NGCCNodeList nl = elem.getChildNodes();
			for(int i=0; i<nl.getLength(); i++)
			{
				NGCCElement e = nl.item(i);
				if(e==null || !e.getNamespaceURI().equals(NGCCGrammar.RELAXNG_NSURI)) continue;
				return determineNullable(e);
			}
			throw new NGCCException("wrong syntax at "+name+" in scope[" + _ScopeInfo.getName() + "]");
		}
		else if(name.equals("group"))
		{
			NGCCNodeList nl = elem.getChildNodes();
			for(int i=0; i<nl.getLength(); i++)
			{
				NGCCElement e = nl.item(i);
				if(e==null || !e.getNamespaceURI().equals(NGCCGrammar.RELAXNG_NSURI)) continue;
				if(determineNullable(nl.item(i))==NULLABLE_FALSE) return NULLABLE_FALSE;
			}
			return NULLABLE_TRUE;
		}
		else if(name.equals("interleave") || name.equals("choice"))
		{
			NGCCNodeList nl = elem.getChildNodes();
			for(int i=0; i<nl.getLength(); i++)
			{
				NGCCElement e = nl.item(i);
				if(e==null || !e.getNamespaceURI().equals(NGCCGrammar.RELAXNG_NSURI)) continue;
				if(determineNullable(nl.item(i))==NULLABLE_TRUE) return NULLABLE_TRUE;
			}
			return NULLABLE_FALSE;
		}
		else if(name.equals("ref"))
		{
			ScopeBuilder sb = _Grammar.getScopeBuilderByName(elem.getAttribute("name"));
			sb.determineNullable();
			return sb._Nullable;
		}
		else
			throw new NGCCException("unknown element "+name+" is found in scope[" + _ScopeInfo.getName() + "]");
	}
	
	public void buildAutomaton()
	{
		ScopeBuildingContext ctx = new ScopeBuildingContext();
		//starts from final state
		State finalstate = createState(null, ctx);
		finalstate.setAcceptable(true);
		
		State initial = null;
		if(_Type==TYPE_NORMAL || _Type==TYPE_ROOT)
			initial = traverseNodeList(_Root.getChildNodes(), ctx, finalstate);
		else
			initial = processRelaxNGNode(_Root, ctx, finalstate);
		_ScopeInfo.setThreadCount(_ThreadCount);
        // TODO: don't we need to reset the preservedAction variable? - Kohsuke
		_ScopeInfo.setInitialState(initial,
            (preservedAction.length()!=0)?
                _ScopeInfo.createAction(preservedAction):
                null);
        
        _ScopeInfo.minimizeStates();
	}
	
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
    
	private State processAttribute(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		String attr_name = exp.getAttribute("name");
		String ns = exp.hasAttribute("ns")? exp.getAttribute("ns") : "";
		
		NameClass nc;
		if(attr_name.length()==0)
			nc = NameClass.fromNameClassElement(_ScopeInfo, exp.getChildNodes().item(0), ns);
		else
			nc = NameClass.fromElementElement(_ScopeInfo, exp, ns);

        
        State tail = createState(exp, ctx);
        Transition te = createTransition(new Alphabet.LeaveAttribute(nc), destination);
        addAction(te,true);
//??        if(ctx.getInterleaveBranchRoot()!=null) te.setEnableState(ctx.getInterleaveBranchRoot());
        tail.addTransition(te);
        
//??        ScopeBuildingContext newctx = new ScopeBuildingContext(ctx);
//??        newctx.setInterleaveBranchRoot(null);

//        State middle = traverseNodeList(exp.getChildNodes(), ctx/*newctx*/, tail);
        State middle = traverseNodeList(exp.getChildNodes(), ctx/*newctx*/,
            createState(exp,ctx));
        
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
*/
	}
	private State processData(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		MetaDataType mdt = _Grammar.addDataType(exp);
		return processData(exp, ctx, destination, mdt);
	}
	private State processText(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		return processData(exp, ctx, destination, MetaDataType.STRING);
	}
	private State processEmpty(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		return destination;
	}
	private State processNotAllowed(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		return createState(exp, ctx); //this returning State is not reachable
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
		}
		return head;
	}
	
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
	private State processOneOrMore(NGCCElement exp, ScopeBuildingContext ctx, State destination)
	{
		addAction(destination,true);
		State head = traverseNodeList(exp.getChildNodes(), ctx, destination);
		addAction(head,true); //addAction must be before mergeTransition
		destination.mergeTransitions(head);
		return head;
	}
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
        
        if(destination.isAcceptable()) head.setAcceptable(true);
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
        if(destination.isAcceptable())  head.setAcceptable(true);
        return head;
	}
	
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
	
	private State createState(NGCCElement corresponding, ScopeBuildingContext ctx)
	{
		State s = new State(_ScopeInfo, ctx.getCurrentThreadIndex(), _ScopeInfo.getStateCount(), corresponding);
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

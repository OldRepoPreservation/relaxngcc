/* this file is generated by RelaxNGCC */
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.XMLReader;

 class sample1 extends NGCCHandler {
private int _ngcc_current_state;
private String name;
private String number;
public sample1(NGCCHandler parent, NGCCRuntime _runtime, int cookie  ) {
    super(parent,cookie);
    this.runtime = _runtime;
    
	_ngcc_current_state=10;
}
public sample1( NGCCRuntime _runtime  ) {
    this(null,_runtime,-1);
}
    protected final NGCCRuntime runtime;
    public final NGCCRuntime getRuntime() { return runtime; }
void action0() throws SAXException {
System.out.println(name);
}
void action1() throws SAXException {
System.out.println(number);
}
    public static void main( String[] args ) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        NGCCRuntime runtime = new NGCCRuntime();
        reader.setContentHandler(runtime);
        for( int i=0; i<args.length; i++ ) {
            runtime.pushHandler(new sample1(runtime));
            reader.parse(args[i]);
            runtime.reset();
        }
    }
private String uri,localName,qname;
public boolean accepted() {
return _ngcc_current_state==0;
}

/* ------------ enterElement ------------ */
public void enterElement(String uri,String localName,String qname,Attributes atts) throws SAXException {
this.uri=uri;
this.localName=localName;
this.qname=qname;
if(_ngcc_current_state==0) {
{
runtime.revertToParentFromEnterElement(this,cookie, uri,localName,qname,atts);
}
}

else if(_ngcc_current_state==10) {
if((uri.equals("") && localName.equals("team"))) {
runtime.pushAttributes(atts);_ngcc_current_state=9;

}
else unexpectedEnterElement(qname);
}

else if(_ngcc_current_state==9) {
if((uri.equals("") && localName.equals("player"))) {
runtime.pushAttributes(atts);_ngcc_current_state=5;processAttribute();

}
else unexpectedEnterElement(qname);
}

else if(_ngcc_current_state==5) {
if((uri.equals("") && localName.equals("name"))) {
runtime.pushAttributes(atts);_ngcc_current_state=4;

}
else unexpectedEnterElement(qname);
}

else if(_ngcc_current_state==1) {
if((uri.equals("") && localName.equals("player"))) {
runtime.pushAttributes(atts);_ngcc_current_state=5;processAttribute();

}
else unexpectedEnterElement(qname);
}

else unexpectedEnterElement(qname);
}

/* ------------ leaveElement ------------ */
public void leaveElement(String uri,String localName,String qname) throws SAXException {
this.uri=uri;
this.localName=localName;
this.qname=qname;
if(_ngcc_current_state==0) {
{
runtime.revertToParentFromLeaveElement(this,cookie, uri,localName,qname);
}
}

else if(_ngcc_current_state==3) {
if((uri.equals("") && localName.equals("name"))) {
_ngcc_current_state=2;

}
else unexpectedLeaveElement(qname);
}

else if(_ngcc_current_state==2) {
if((uri.equals("") && localName.equals("player"))) {
_ngcc_current_state=1;

}
else unexpectedLeaveElement(qname);
}

else if(_ngcc_current_state==1) {
if((uri.equals("") && localName.equals("team"))) {
_ngcc_current_state=0;

}
else unexpectedLeaveElement(qname);
}

else unexpectedLeaveElement(qname);
}

/* ------------ enterAttribute ------------ */
public void enterAttribute(String uri,String localName,String qname) throws SAXException {
this.uri=uri;
this.localName=localName;
this.qname=qname;
if(_ngcc_current_state==0) {
{
runtime.revertToParentFromEnterAttribute(this,cookie, uri,localName,qname);
}
}

else if(_ngcc_current_state==5) {
if((uri.equals("") && localName.equals("number"))) {
_ngcc_current_state=11;

}
else unexpectedEnterAttribute(qname);
}

else unexpectedEnterAttribute(qname);
}

/* ------------ leaveAttribute ------------ */
public void leaveAttribute(String uri,String localName,String qname) throws SAXException {
this.uri=uri;
this.localName=localName;
this.qname=qname;
if(_ngcc_current_state==0) {
{
runtime.revertToParentFromLeaveAttribute(this,cookie, uri,localName,qname);
}
}

else if(_ngcc_current_state==12) {
if((uri.equals("") && localName.equals("number"))) {
_ngcc_current_state=5;processAttribute();

}
else unexpectedLeaveAttribute(qname);
}

else unexpectedLeaveAttribute(qname);
}

/* ------------ text ------------ */
public void text(String ___$value) throws SAXException
{
if(_ngcc_current_state==0) {
{
runtime.revertToParentFromText(this,cookie, ___$value);
}
}

else if(_ngcc_current_state==4) {
{
name=___$value;action0();_ngcc_current_state=3;

}
}

else if(_ngcc_current_state==11) {
{
number=___$value;action1();_ngcc_current_state=12;

}
}

}

/* ------------ attribute ------------ */
public void processAttribute() throws SAXException
{
int ai;
if(_ngcc_current_state==5) {
if((ai = runtime.getAttributeIndex("","number"))>=0) {
runtime.consumeAttribute(ai);

}
}

}

/* ------------ child completed ------------ */
public void onChildCompleted(Object result, int cookie,boolean needAttCheck) throws SAXException {
switch(cookie) {
default:
    throw new InternalError();
}
}

}

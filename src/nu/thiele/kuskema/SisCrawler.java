package nu.thiele.kuskema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.TreeMap;

import nu.thiele.kuskema.exceptions.NoFacultiesFoundException;
import nu.thiele.kuskema.ui.Tree;
import nu.thiele.kuskema.ui.Tree.Node;

public class SisCrawler extends Observable{
	public static final String root = "http://kurser.ku.dk/";
	public Node current;
	private Tree tree;
	
	public SisCrawler(){
		this.tree = new Tree();
		this.current = this.tree.getTop();
	}
	
	public static URLConnection connect(String site, boolean redirect) throws IOException{
		if(redirect) site = redirect(site);
		URL url = new URL(site);
        HttpURLConnection yc = (HttpURLConnection) url.openConnection();
        yc.setRequestMethod("GET");
        yc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en; rv:1.9.0.13) Gecko/2009073022 Firefox/4.0.1");
        yc.setReadTimeout(1000*15);
        yc.connect();
        return yc;
	}
	
	public static String redirect(String site) throws IOException{
		URL url = new URL(site);
		HttpURLConnection yc = (HttpURLConnection) url.openConnection();
		yc.setRequestMethod("HEAD");
		yc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en; rv:1.9.0.13) Gecko/2009073022 Firefox/4.0.1");
		if(yc.getHeaderFields().containsKey("Location")) return redirect(yc.getHeaderField("Location"));
		else return site;
	}
	
	public Node getCurrent(){
		return this.current;
	}
	
	public ArrayList<Node> hent(final Node n) throws Exception,IOException{
		if(n == null){ //Hvis et semester skal hentes
			hentFakulteter();
			return update(this.tree.getTop());
		}
		else if(this.tree.getTop().children.contains(n)){ //Semestre ikke loadet
			hentSemestre(n);
			return update(n); 
		}
		else if(n.children.size() > 0){ //Hvis allerede loadet
			return update(n);
		}
		String value = n.value;
		URLConnection yc = connect(root+value, true);
		InputStreamReader isr = new InputStreamReader(yc.getInputStream());
        BufferedReader br = new BufferedReader(isr);
		if(value.startsWith("lptree")){ //Parse lptree
	        String linje;
	        while((linje = br.readLine()) != null){
	        	if(linje.contains("Lektionsplanen - ")){
	        		linje = linje.substring(linje.indexOf("/></a></div><strong><a href="));
	        		String[] array = linje.split("</a></div><strong>");
	        		for(int i = 1; i < array.length; i++){
	        			String link = array[i].substring(array[i].indexOf("/"), array[i].indexOf("\">"));
	        			link = "vislpafsnit.aspx?snr="+link.substring(link.indexOf("=")+1, link.indexOf("&"))+"&sprog=1";
	        			String navn = array[i].substring(array[i].indexOf(">")+1, array[i].indexOf("</a>"));
	        			n.addChild(new Node(navn,link));
	        		}
	        		break;
	        	}
	        }
		}
		else if(value.startsWith("vislpafsnit")){ //Parse vislpafsnit
	        String linje;
	        while((linje = br.readLine()) != null){
	        	if(linje.contains("<a id=\"ctl00_cph_main_rpt_")){
	        		linje = linje.replace("http://sis.ku.dk/kurser/", "");
	        		linje = linje.substring(linje.indexOf("href"));
	        		linje = linje.substring(linje.indexOf("\"")+1);
	        		String link = linje.substring(0, linje.indexOf("\""));
	        		String navn = linje.substring(linje.indexOf(">")+1,linje.lastIndexOf("</a>"));
	        		Node add = new Node(navn,link);
	        		if(link.startsWith("viskursus")) add.type = Tree.Node.kursus;
	        		n.addChild(add);
	        	}
	        }
		}
		if(n.children.size() == 0) throw new Exception("Ingen data fundet. Er sis oppe?");
		//Frigør
		br.close();
		isr.close();
		//Opdater
        return update(n);
	}
		
	public void hentFakulteter() throws IOException,NoFacultiesFoundException,Exception{
		this.tree.getTop().children.clear(); //To be sure
		URLConnection yc = connect(root, true);
        InputStreamReader isr = new InputStreamReader(yc.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        String linje;
        StringBuilder sb = new StringBuilder();
        try{
        	while ((linje = br.readLine()) != null){
        		if(sb.length() > 0){
        			sb.append(linje);
        			if(linje.contains("</select>")) break;
        		}
        		if(linje.contains("id=\"faculty\"")) sb.append(linje);
        	}
        }
        catch(Exception e){
        	throw e;
        }
        finally{
            isr.close();
            br.close();
        }
        String[] faculties = sb.toString().split("</option><option");
        for(String f : faculties){
        	String value = f.substring(f.lastIndexOf("value=")+6+1);
        	if(value.length() == 0 || value.charAt(0) == '"'){
        		continue; //Ugly, but easier to read
        	}
        	String name = f.substring(f.indexOf(value));
        	name = name.substring(name.indexOf(">")+1);
        	if(name.contains("<")) name = name.substring(0, name.indexOf("<"));
        	if(name.length() > 0 && value.length() > 0) this.tree.addToTop(name,value);
        }
        if(this.tree.getTop().children.size() == 0) throw new NoFacultiesFoundException();
	}
	
	public TreeMap<String,String> hentKursusInfo(String link) throws Exception{
		TreeMap<String,String> retur = new TreeMap<String,String>();
    	URLConnection yc = SisCrawler.connect(SisCrawler.root+link, true);
    	InputStreamReader isr = new InputStreamReader(yc.getInputStream(), Charset.forName("ISO-8859-1"));
        BufferedReader br = new BufferedReader(isr);
        String linje;
        String institut = "Intet institut fundet for kurset";
        //Hent skema...
        boolean skema = false;
        while((linje = br.readLine()) != null){
        	if(linje.contains("lbl_Institutter")){ //Vil altid ligge før skemalinket
        		linje = linje.substring(linje.lastIndexOf("\">")+2);
        		linje = linje.substring(0, linje.indexOf("<"));
        		institut = linje;
        	}
        	else if(linje.contains("skema.ku.dk/")){
        		//Frigør
        		isr.close();
        		br.close();
        		//Åbn nyt
        		linje = linje.substring(linje.indexOf("<a "));
        		link = linje.substring(linje.indexOf("\"")+1, linje.indexOf(" title")-1);
        		link = link.replaceFirst("http", "https");
        		yc = connect(link, false);
        		isr = new InputStreamReader(yc.getInputStream(), Charset.forName("ISO-8859-1"));
        		br = new BufferedReader(isr);
                String dag = "";
                boolean gem = false;
                int count = 0;
                int tdcount = 0;
                String start = "";
                String slut = "";
                String type = "";
                
                while((linje = br.readLine()) != null){
        			if(linje.startsWith("<td>") && gem) tdcount++;
                	//Reset retur-infoene
                	if(linje.startsWith("<p><span class='labelone'>")){
                		linje = linje.substring(linje.indexOf("'>")+2, linje.indexOf("</span>"));
                		dag = linje.toLowerCase();
                		gem = true;
                		tdcount = 0;
                		count = 0;
                	}
                	else if(tdcount == 3){
                		type = tdremove(linje);
                	}
                	else if(tdcount == 4){
                		start = tdremove(linje);
                	}
                	else if(tdcount == 5){
                		slut = tdremove(linje);
                		if(!type.toLowerCase().equals("type")){
                			//Tilføj fag. Find bedst-matchende type, for at øge modstandsdygtigheden
                			String v = findtype(type)+"#"+dag+"#"+start+"#"+slut;
                			if(!retur.containsValue(v)){
                				retur.put(dag+count, v);
                				count++;
                			}
                		}
                	}
                	else if(tdcount == 9) tdcount = 0;
                	else if(dag.toLowerCase().equals("fredag") && linje.startsWith("</table")) break;
                }
                skema = true;
        		//Og hent skemaet...
        		break;
        	}
        }
        System.out.println("Retur: "+retur);
        isr.close();
        br.close();
        if(!skema) throw new Exception("Intet skema fundet for faget.");
        retur.put("institut", institut);
    	return retur;
    }
	
	public void hentSemestre(Node fakultet) throws Exception,IOException{
		if(fakultet.children.size() > 0){
			return;
		}
		//Hent oversigt over perioder
        URLConnection yc = connect(root+fakultet.value, false);
        InputStreamReader isr = new InputStreamReader(yc.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        String linje;
        try{
            while((linje = br.readLine()) != null){
            	if(linje.contains("Vælg publikation")){
            		while(linje.contains("lptree")){
            			linje = linje.substring(linje.indexOf("lptree"));
            			String semesterlink = linje.substring(linje.indexOf("lptree"));
            			semesterlink = semesterlink.substring(0, semesterlink.indexOf("\""));
            			String semesternavn = linje.substring(linje.indexOf(">")+1);
            			semesternavn = semesternavn.substring(0, semesternavn.indexOf("<"));
            			this.tree.add(semesternavn, semesterlink, fakultet);
            			linje = linje.substring(linje.indexOf("class=\"menuLink\""));
            		}
            		break;
            	}
            }
        }
        catch(Exception e){
        	throw new Exception(e);
        }
        if(fakultet.children.size() == 0) throw new Exception("Ingen semestre fundet. Er sis oppe?");
	}
	
	public boolean isTop(){
		return this.current.equals(this.tree.getTop());
	}
	
    private static String tdremove(String in){
    	return in.substring(in.indexOf("<td>")+4, in.indexOf("</td>"));
    }
	
	public ArrayList<Node> tilbage() throws Exception{
		if(this.current.equals(this.tree.getTop())) return update(this.tree.getTop());
		return update(this.current.parent);
	}
	
	private ArrayList<Node> update(Node n) throws Exception{
		if(n == null){
			throw new Exception("Fejl i update");
		}
		this.current = n;
		this.notifyObservers();
		return n.children;
	}
	
	//Static funkioner
	/**
	 * 
	 * @param compare
	 * @param compareTo Den der skal beregnes lighed i forhold til
	 * @return
	 */
	private static double equality(String compare, String compareTo){
		String lcs = lcs(compare.toLowerCase(), compareTo.toLowerCase());
		return ((double)lcs.length())/((double)compareTo.length());
	}
	
	private static String lcs(String a, String b, HashMap<String,String> known, int ia, int ib){
		String cord = ia+":"+ib;
		if(known.containsKey(cord)) return known.get(cord);
		if(ia >= a.length() || ib >= b.length()) return "";
		if(a.charAt(ia) == b.charAt(ib)){
			String s = a.charAt(ia)+lcs(a, b, known, ia+1, ib+1);
			known.put(cord, s);
			return s;
		}
		String s = longest(lcs(a, b, known, ia+1, ib), lcs(a,b,known,ia,ib+1));
		known.put(cord, s);
		return s;
	}
	
	private static String lcs(String a, String b){
		return lcs(a, b, new HashMap<String,String>(), 0, 0);
	}
	
	private static String longest(String a, String b){
		if(a.length() > b.length()) return a;
		return b;
	}
	
	private static String findtype(String s){
		double øvelse = equality(s.toLowerCase(), "øvelse");
		double forelæsning = equality(s.toLowerCase(), "forelæsning");
		if(øvelse < 0.5 && forelæsning < 0.5) return "Ukendt";
		if(øvelse > forelæsning) return "Øvelse";
		else return "Forelæsning";
	}
}
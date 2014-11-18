package nu.thiele.kuskemaapp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nu.thiele.kuskemaapp.data.Course;
import nu.thiele.kuskemaapp.data.HtmlOption;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.SparseArray;

public class CourseLoader {
	private List<HtmlOption> blocks,faculties;
	
	public CourseLoader(){
		this.blocks = new LinkedList<HtmlOption>();
		this.faculties = new LinkedList<HtmlOption>();
	}
	
	public List<HtmlOption> getPeriods(){
		return this.blocks;
	}
	
	public List<HtmlOption> getFaculties(){
		return this.faculties;
	}
	
	public List<ScheduleClassLink> getCourseClasses(Course c) throws IOException{
		Document doc = getDocument(Settings.COURSES_URL, c.getCourseLink());
		Elements elems = doc.select("#dropCourseHoldingsHere tr:not(:first-child) div a");
		List<ScheduleClassLink> classes = new LinkedList<ScheduleClassLink>();
		for(Element elem : elems){
			String href = elem.attr("href");
			String text = elem.text();
			ScheduleClassLink classLink = new ScheduleClassLink(c, href, text);
			classes.add(classLink);
		}
		return classes;
	}
	
	public SparseArray<List<ClassHour>> getCourseSchedule(ScheduleClassLink c) throws IOException{
		SparseArray<List<ClassHour>> retval = new SparseArray<List<ClassHour>>();
		URL url = new URL(c.getValue());
		Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
		Pattern dayPattern = Pattern.compile("<p><span class='labelone'>(.*)</span></p>");
		URLConnection con = url.openConnection();
		Matcher m = p.matcher(con.getContentType());
		String charset = m.matches() ? m.group(1) : "ISO-8859-1"; //ISO is default at page. Keep it just in case nothing is set
		InputStreamReader r = new InputStreamReader(con.getInputStream(), charset);
		BufferedReader in = new BufferedReader(r);
		String line = null;
		int dayCount = 0;
		while ((line = in.readLine()) != null) {
			Matcher match = dayPattern.matcher(line);
			if(match.find()){
				StringBuilder html = new StringBuilder();
				//Go to next
				line = in.readLine();
				for(;line != null && !line.contains("</table>"); line = in.readLine()){
					html.append(line+"\r\n");
				}
				html.append("</table>");
				Document table = getDocumentFromString(html.toString());
				Elements rows = table.select("table.spreadsheet tr:not(:first-child)");
				List<ClassHour> hours = new LinkedList<ClassHour>();
				if(rows.size() > 0){
					for(Element row : rows){
						Elements cols = row.select("td");
						String type = cols.get(2).text().trim();
						String start = cols.get(3).text().trim();
						String end = cols.get(4).text().trim();
						String room = cols.get(7).text().trim();
						String teacher = cols.get(8).text().trim();

						//Make a guess at the type
						ClassHourType guessType = ClassHourType.UNKNOWN;						
						Locale l = new Locale("da","DK"); //Page is in Danish...
						String oev = "øv";
						String aud = "aud";
						System.out.println(type);
						if(type.toLowerCase(l).contains(aud) || type.toLowerCase(l).contains("forelæsning") || room.toLowerCase(l).startsWith(aud)) guessType = ClassHourType.LECTURE;
						else if(type.toLowerCase(l).contains(oev) || room.toLowerCase(l).startsWith(oev)) guessType = ClassHourType.CLASS;

						//If indeed øvelse, cut off start
						if(room.toLowerCase(l).startsWith(oev+" -")){
							room = room.substring((oev+" -").length());
						}
						else if(room.toLowerCase(l).startsWith(aud+" - ")){
							room = room.substring((aud+" -").length());
						}
						
						//And add then new
						ClassHour hour = new ClassHour(c.getParent(), dayCount, start, end, room, teacher, guessType);
						hours.add(hour);
					}
				}
				retval.put(dayCount, hours);
				dayCount++;
			}
		}
		//Cleanup
		in.close();
		r.close();
		
		return retval;
	}
	
	public synchronized void initialise() throws IOException{
		//Remove old
		this.faculties.clear();
		this.blocks.clear();
		
		//Add blocks
		Document doc = getDocument(Settings.COURSES_URL, "/");
		Elements elems = doc.select("select#faculty option");
		for(Element elem : elems){
			HtmlOption fac = new HtmlOption(elem.attr("value"),elem.text());
			this.faculties.add(fac);
		}
		
		//Add faculties
		elems = doc.select("select#period option");
		for(Element elem : elems){
			HtmlOption period = new HtmlOption(elem.attr("value"),elem.text());
			this.blocks.add(period);
		}
		Collections.sort(this.faculties, new HtmlOption.TextComparator());
	}
	
	public static String makeFullCoursePageUrl(String file){
		return Settings.COURSES_URL+file;
	}
	
	public List<Course> doSearch(String query) throws IOException{
		return this.doSearch(query, null, null);
	}
	
	public List<Course> doSearch(String query, String faculty) throws IOException{
		return this.doSearch(query, faculty, null);
	}
	
	public List<Course> doSearch(String query, String faculty, String period) throws IOException{
		List<Course> retval = new LinkedList<Course>();
		String u = "/search?studyBlockId=null&teachingLanguage=&schedules=&studyId=&openUniversity=-1&programme=&departments=&volume=";
		if(query != null) u += "&q="+query;
		if(period != null) u += "&period="+period;
		if(faculty != null) u += "&faculty="+faculty;
		Document d = getDocument(Settings.COURSES_URL,u);
		Elements elems = d.select("#searchResultDiv table tbody tr:not(:first-child)");
		for(Element elem : elems){
			try{
				Elements tds = elem.select("td");
				Element name = tds.get(0);
				Element link = name.select("a").first();
				String studieNaevn = name.select("span").first().text().trim();
				String courseLink = link.attr("href");
				String courseName = link.text().trim();
				String level = tds.get(1).text().trim();
				String ects = tds.get(2).text().trim();
				String language = tds.get(3).text().trim();
				String year = tds.get(5).text().trim();
				Course c = new Course(courseName, courseLink, studieNaevn, ects, level, language, year);
				retval.add(c);
			}
			catch(Exception e){} //Don't crash 'cause one fails
		}
		Collections.sort(retval, new Course.NameComparator());
		return retval;
	}
	
	private static Document getDocument(String u, String file) throws IOException{
		String url = u+file;
		Connection c = Jsoup.connect(url).userAgent(Settings.USER_AGENT).timeout(Settings.TIMEOUT_IN_MILLISECONDS); //15s load time before timeout
		Document d = Jsoup.parse(new String(c.execute().bodyAsBytes()));
		d.outputSettings().prettyPrint(false);
        return d;
	}
	
	private static Document getDocumentFromString(String s){
		return Jsoup.parse(s);
	}
	
	public static class ClassHour{
		private boolean highlight = false;
		private Course parent;
		private int day, id;
		private String start, end, room, teacher;
		private ClassHourType type;
		public ClassHour(Course parent, int day, String start, String end, String room, String teacher, ClassHourType type){
			this.parent = parent;
			this.day = day;
			this.start = start;
			this.end = end;
			this.room = room;
			this.teacher = teacher;
			this.type = type;
		}
		public int getDay() {
			return this.day;
		}
		public String getStart() {
			return this.start;
		}
		public String getEnd() {
			return this.end;
		}
		public int getId(){
			return this.id;
		}
		public Course getParent(){
			return this.parent;
		}
		public String getRoom() {
			return this.room;
		}
		public String getTeacher() {
			return this.teacher;
		}
		public ClassHourType getHourType() {
			return this.type;
		}
		
		public boolean isHighlighted(){
			return this.highlight;
		}
		
		public void setHighlight(boolean val){
			this.highlight = val;
		}
		
		public void setId(int id){
			this.id = id;
		}
		
		public String toString(){
			return this.type.toString();
		}
		
		public static class StartEndComparator implements Comparator<ClassHour>{
			@Override
			public int compare(ClassHour lhs, ClassHour rhs) {
				String missingZeroRegex = "^[1-9]:\\d{2}$";
				//Make sure it's same format. Possibly format into ints, but this will be fine as well
				String s1start = lhs.getStart();
				String s2start = rhs.getStart();
				String s1end = lhs.getEnd();
				String s2end = rhs.getEnd();
				if(s1start.matches(missingZeroRegex)) s1start = "0"+s1start;
				if(s2start.matches(missingZeroRegex)) s2start = "0"+s2start;
				if(s1end.matches(missingZeroRegex)) s1end = "0"+s1end;
				if(s2end.matches(missingZeroRegex)) s2end = "0"+s2end;

				if(s1start.equals(s2start)) return s1end.compareTo(s2end);
				return s1start.compareTo(s2start);
			}
		}
	}
	
	public static enum ClassHourType{
		CLASS, LECTURE, UNKNOWN
	}
	
	public static class ScheduleClassLink{
		private Course parent;
		private String text, value;
		public ScheduleClassLink(Course parent, String value, String text){
			this.parent = parent;
			this.text = text;
			this.value = value;
		}
		
		public Course getParent(){
			return this.parent;
		}
		
		public String getText(){
			return this.text;
		}
		
		public String getValue(){
			return this.value;
		}
		
		public String toString(){
			return this.getText();
		}
		
		public static class TextComparator implements Comparator<HtmlOption>{
			@Override
			public int compare(HtmlOption arg0, HtmlOption arg1) {
				return arg0.getText().compareTo(arg1.getText());
			}
		}
	}
}

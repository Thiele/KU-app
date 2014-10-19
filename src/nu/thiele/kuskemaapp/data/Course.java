package nu.thiele.kuskemaapp.data;

import java.util.Comparator;
import java.util.Locale;

public class Course{
	private int id;
	private String name, courseLink, studieNaevn, ects, level, language, year;
	public Course(String name, String courseLink, String studieNaevn, String ects, String level, String language, String year){
		this.name = name;
		this.courseLink = courseLink;
		this.studieNaevn = studieNaevn;
		this.ects = ects;
		this.level = level;
		this.language = language;
		this.year = year;
	}
	
	public String getCourseLink(){
		return this.courseLink;
	}
	
	public String getECTS(){
		return this.ects;
	}
	
	public int getId(){
		return this.id;
	}
	
	public String getLanguage(){
		return this.language;
	}
	
	public String getLevel(){
		return this.level;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getStudieNaevn(){
		return this.studieNaevn;
	}
	
	public String getYear(){
		return this.year;
	}
	
	@Override
	public boolean equals(Object c){
		if(!(c instanceof Course)) return false;
		Course course = (Course) c;
		if(this.id != 0 && course.getId() != 0) return this.id == course.getId();
		//TODO: Check periods as well?
		return (this.courseLink.equals(course.getCourseLink())
				&& this.ects.equals(course.getECTS())
				&& this.language.equals(course.language)
				&& this.level.equals(course.level)
				&& this.name.equals(course.getName())
				&& this.studieNaevn.equals(course.getStudieNaevn())
				&& this.year.equals(course.getYear()));
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public String toString(){
		return this.name;
	}
	
	public static class NameComparator implements Comparator<Course>{
		@Override
		public int compare(Course o1, Course o2) {
			return o1.getName().toLowerCase(Locale.getDefault()).compareTo(o2.getName().toLowerCase(Locale.getDefault()));
		}
	}
}

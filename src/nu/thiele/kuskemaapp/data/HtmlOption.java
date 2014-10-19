package nu.thiele.kuskemaapp.data;

import java.util.Comparator;

public class HtmlOption{
	private String text, value;
	public HtmlOption(String value, String text){
		this.text = text;
		this.value = value;
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
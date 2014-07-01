package nu.thiele.kuskema.ui;

import java.util.ArrayList;

public class Tree {
	private static final String topname = "TOP OF THE TREE";
	private Node top;
	public Tree(){
		this.top = new Node(topname,topname);
	}
		
	public void add(String id, String value, Node node){
		node.addChild(new Node(id, value));
	}
	
	public void addToTop(String id, String value){
		add(id,value,top);
	}
	
	public void addToTop(Node n){
		this.top.addChild(n);
	}
	
	public Node getTop(){
		return this.top;
	}
	
	public static class Node implements Comparable<Node>{
		public static final String link = "link", kursus = "kursus"; 
		public String id;
		public String type = link;
		public String value;
		public Node parent;
		public ArrayList<Node> children;
		
		public Node(String id, String value){
			this.id = id;
			this.value = value;	
			this.children = new ArrayList<Node>();
		}
		
		public void addChild(Node t){
			t.parent = this;
			this.children.add(t);
		}
		
		public boolean equals(Node n){
			return this.id.equals(n.id) && this.value.equals(n.value);
		}
		
		public String toString(){
			return this.id+"="+this.value;
		}

		@Override
		public int compareTo(Node another) {
			return this.id.compareTo(another.id);
		}
	}
}
package br.com.thinkti.android.filechooser;

public class Option implements Comparable<Option>{
	private String name;
	private String data;
	private String path;
	private boolean folder;
	private boolean parent;
	
	public Option(String n,String d,String p, boolean folder, boolean parent)
	{
		name = n;
		data = d;
		path = p;
		this.folder = folder;
		this.parent = parent;
	}
	public String getName()
	{
		return name;
	}
	public String getData()
	{
		return data;
	}
	public String getPath()
	{
		return path;
	}
	@Override
	public int compareTo(Option o) {
		if(this.name != null)
			return this.name.toLowerCase().compareTo(o.getName().toLowerCase()); 
		else 
			throw new IllegalArgumentException();
	}
	public boolean isFolder() {
		return folder;
	}
	public boolean isParent() {
		return parent;
	}
}

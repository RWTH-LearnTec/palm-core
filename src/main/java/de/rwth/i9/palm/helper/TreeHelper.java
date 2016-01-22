package de.rwth.i9.palm.helper;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

public class TreeHelper
{

	/*
	 * Constructor is needed because jackson JSON library uses constructor
	 * instead of setter
	 */
	public TreeHelper()
	{
	}

	public TreeHelper( final String title, final String key, final List<TreeHelper> children )
	{
		this.title = title;
		this.key = key;
		this.children = children;
	}

	public TreeHelper( final String title, final String key, final List<TreeHelper> children, int position )
	{
		this.title = title;
		this.key = key;
		this.children = children;
		this.position = position;
	}

	/* attributes */
	private String title;
	private String key;
	private String type;
	private String link;
	private boolean expand;
	private List<TreeHelper> children = null;
	@JsonIgnore
	private int position;

	/* getters & setters */
	public String getTitle()
	{
		return title;
	}

	public void setTitle( final String title )
	{
		this.title = title;
	}

	public String getKey()
	{
		return key;
	}

	public void setKey( final String key )
	{
		this.key = key;
	}

	public List<TreeHelper> getChildren()
	{
		if ( this.children == null )
		{
			return new ArrayList<TreeHelper>();
		}
		return this.children;
	}

	public void setChildren( final List<TreeHelper> children )
	{
		this.children = children;
	}

	public TreeHelper addChild( final TreeHelper child )
	{

		if ( this.children == null )
			this.children = new ArrayList<TreeHelper>();

		this.children.add( child );

		return this;
	}

	public static TreeHelper findNodeByKey( TreeHelper theTree, String targetTreeNodeKey )
	{
		if ( theTree.children != null && !theTree.children.isEmpty() )
			for ( TreeHelper treeNode : theTree.children )
			{
				if ( treeNode.getKey().equalsIgnoreCase( targetTreeNodeKey ) )
					return treeNode;
				else
					findNodeByKey( treeNode, targetTreeNodeKey );
			}

		return null;
	}

	public int getPosition()
	{
		return position;
	}

	public void setPosition( int position )
	{
		this.position = position;
	}

	public boolean isExpand()
	{
		return expand;
	}

	public void setExpand( boolean expand )
	{
		this.expand = expand;
	}

	public String getType()
	{
		return type;
	}

	public void setType( String type )
	{
		this.type = type;
	}

	public String getLink()
	{
		return link;
	}

	public void setLink( String link )
	{
		this.link = link;
	}

}

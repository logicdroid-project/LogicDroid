package android.pem;

import java.util.ArrayList;

public class Event
{
    public String rel;
    public ArrayList<Integer> vars;
    public int varCount;

    public Event(String ev)
    {
    	this.vars = new ArrayList<Integer>();
      	varCount = 0;

      	String[] temp = ev.split(" ");
      	this.rel = temp[0];
      	for (int i = 1; i < temp.length; i++)
      	{
      		this.vars.add(Integer.parseInt(temp[i]));
        	varCount++;
      	}
    }

    public Event(String rel, int ... UID)
    {
        this.rel = rel;
        this.varCount = UID.length;
        vars = new ArrayList<Integer>();
        for (int i = 0; i < UID.length; i++)
        {
            vars.add(UID[i]);
        }
    }

    public Event(String rel, ArrayList<Integer> vars, int varCount)
    {
    	this.rel = rel;
    	this.vars = vars;
    	this.varCount = varCount;
    }
    
    public int get_index(int app_num)
    {
    	int idx = 0, mul = 1;
      	for (int i = varCount - 1; i >= 0; mul*=app_num, i--)
      	{
      		idx += vars.get(i) * mul;
      	}
      	return idx;
    }
    
    public String toString()
	{
		StringBuilder sb = new StringBuilder(rel + "(");
		if (varCount > 0)
		{
			sb.append(vars.get(0));
		}
		for (int i = 1; i < varCount; i++)
		{
			sb.append("," + vars.get(i));
		}
		sb.append(")");
		return sb.toString();
	}
}

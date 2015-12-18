/*
    PolicyMonitoring is a program to generate monitor module in LogicDroid specified in XML
    Copyright (C) 2012-2013  Hendra Gunadi

    This file is part of PolicyMonitoring

    PolicyMonitoring is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.*;

public class MetaFormula {
	public int varCount;
	public ArrayList<String> vars;
	int objectCount;
	public ArrayList<String> objects;
	public int relCount;
	public ArrayList<String> rels;
	public ArrayList<String> rel_type;
	public ArrayList<Integer> relsCard; // Denote the cardinality of a relation
	public int tempCount; // number of temporal operators
	public ArrayList<Integer> metric;
	// More information can be added if needed
	
	public MetaFormula()
	{
		varCount = 0;
		relCount = 0;
		objectCount = 0;
		vars = new ArrayList<String>();
		objects = new ArrayList<String>();
		rels = new ArrayList<String>();
		rel_type = new ArrayList<String>();				
		relsCard = new ArrayList<Integer>();
		metric = new ArrayList<Integer>();
	}
	
	public void insert_rel(String rel, Integer cardinality, String rel_type)
	{
		if (!rels.contains(rel))
		{
			rels.add(rel);
			relsCard.add(cardinality);
			this.rel_type.add(rel_type);
			relCount++;
		}
	}
	
	public void insert_var(String var)
	{
		if (!vars.contains(var))
		{
			vars.add(var);
			varCount++;
		}
	}
	
	public void insert_object(String object)
	{
		if (!objects.contains(object))
		{
			objects.add(object);
			objectCount++;
		}
	}
	
	public void insert_metric(Integer metric)
	{
		this.metric.add(metric);
		tempCount++;
	}
}

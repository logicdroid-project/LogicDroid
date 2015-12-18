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

public class Policy {
	public int ID;
	public ArrayList<Formula> formulas;
	public ArrayList<String> formula_type;
	public ArrayList<String> target_recursive;
	public ArrayList<String> objects;
	public ArrayList<String> relations;
	public ArrayList<String> relation_type;
	public ArrayList<Integer> relationsCard;
	public ArrayList<Integer> metrics;
	public int tempCount;
	public int formulaCount;
	public int relationCount;
	public int objectCount;
	
	public Policy()
	{
		formulas = new ArrayList<Formula>();
		formula_type = new ArrayList<String>();
		target_recursive = new ArrayList<String>();
		relations = new ArrayList<String>();
		relation_type = new ArrayList<String>();
		relationsCard = new ArrayList<Integer>();
		metrics = new ArrayList<Integer>();
		objects = new ArrayList<String>();
		formulaCount = 0;
		relationCount = 0;
		tempCount = 0;
		objectCount = 0;
	}
	
	public void add_formula(Formula formula, String type, String target)
	{
		formulas.add(formula);
		formula_type.add(type);
		target_recursive.add(target);
		formulaCount++;
	}
	
	public void add_relation(String relation, Integer cardinality, String relation_type)
	{
		// only needs to maintain overall unique relations
		if (!relations.contains(relation))
		{
			relations.add(relation);
			this.relation_type.add(relation_type);
			relationsCard.add(cardinality);
			relationCount++;
		}
	}
	
	public void add_object(String object)
	{
		// only needs to maintain overall unique relations
		if (!objects.contains(object))
		{
			objects.add(object);
			objectCount++;
		}
	}
	
	public void add_metric(ArrayList<Integer> metric)
	{
		for (int i = metric.size() - 1; i >= 0 ; i-- )
		{
			metrics.add(0, metric.get(i));
			tempCount++;
		}
	}
}

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

import org.w3c.dom.*;

import java.util.*;


public class Formula {
	public String type;
	public ArrayList<Formula> sub;
	public int count; // sub formula count
	
	// For the purpose of statistics used to generate the monitor
	public ArrayList<String> vars;
	public int varCount;
	
	public static String formula_type(String subformula)
	{
		int len = subformula.length();
		int stackCount = 0;
		if (subformula.contains("F_") && subformula.charAt(0) == 'F' && subformula.charAt(1) == '_') return GlobalVariable.DIAMONDDOT;
		if (subformula.contains("exist_") && subformula.substring(0, 5).compareTo("exist") == 0 && subformula.charAt(5) == '_') return GlobalVariable.EXIST;
		if (subformula.contains("forall_") && subformula.substring(0, 5).compareTo("forall") == 0 && subformula.charAt(6) == '_') return GlobalVariable.FORALL;
		for (int i = 0; i < len; i++)
		{
			if (subformula.charAt(i) == '(') stackCount++;
			else if (subformula.charAt(i) == ')') stackCount--;
			
			if (subformula.charAt(i) == '&' && stackCount == 0) return GlobalVariable.AND;
			else if (subformula.charAt(i) == '|' && stackCount == 0) return GlobalVariable.OR;
		}
		return GlobalVariable.ATOM;
	}
	
	private int count_distinct_vars(ArrayList<String> addition, ArrayList<String> source)
	{
		int count = 0;
		HashSet<String> counter = new HashSet<String>();
		
		int len = source.size();
		for (int i = 0; i < len; i++)
		{
			if (!counter.contains(source.get(i)))
			{
				counter.add(source.get(i));
				count++;
			}
		}
		len = addition.size();
		for (int i = 0; i < len; i++)
		{
			if (!counter.contains(addition.get(i)))
			{
				counter.add(addition.get(i));
				source.add(addition.get(i));
				count++;
			}
		}
		
		return count;
	}
	
	private int count_distinct_vars_for_atom(ArrayList<String> addition, ArrayList<String> var_type, ArrayList<String> source)
	{
		int count = 0;
		HashSet<String> counter = new HashSet<String>();
		
		int len = source.size();
		for (int i = 0; i < len; i++)
		{
			if (!counter.contains(source.get(i)))
			{
				counter.add(source.get(i));
				count++;
			}
		}
		len = addition.size();
		for (int i = 0; i < len; i++)
		{
			if (var_type.get(i).equals(GlobalVariable.FREE) && !counter.contains(addition.get(i)))
			{
				counter.add(addition.get(i));
				source.add(addition.get(i));
				count++;
			}
		}
		
		return count;
	}
	
	public Formula(String subformula, String type, MetaFormula info)
	{
		if (subformula.charAt(0) == '#') return; // Indicate the end of string
		
		sub = new ArrayList<Formula>();
		vars = new ArrayList<String>();
		String checkStr = subformula.substring(1, subformula.length() - 1); // Assume well formed, that is subformula always in the form of (...)
		//String checkType = formula_type(checkStr);
		int len = checkStr.length();
		this.type = type;
		// Either Atomic formula or contain logic operator (uniform)
		if (type == GlobalVariable.AND || type == GlobalVariable.OR || type == GlobalVariable.SINCE) // Either And or Or
		{
			int stackCount = 0;
			int startIdx;
			for(int i = 0; i < len; i++)
			{
				if (checkStr.charAt(i) == '(')
				{
					startIdx = i;
					i++;
					stackCount = 1;
					while(stackCount > 0) // assume well formed
					{
						if (checkStr.charAt(i) == ')') stackCount--;
						else if (checkStr.charAt(i) == '(') stackCount++;
						i++;
					}
					String newSub = checkStr.substring(startIdx, i);
					String checkType = formula_type(newSub.substring(1, newSub.length() - 1));
					if (checkType == GlobalVariable.AND || checkType == GlobalVariable.OR)
					{
						sub.add(new Formula(newSub, checkType, info));
						varCount = count_distinct_vars(sub.get(count).vars, vars);
						count++;
					}
					else if (checkType == GlobalVariable.DIAMONDDOT)
					{
						sub.add(new DiamondDotFormula(newSub, GlobalVariable.DIAMONDDOT, info));
						varCount = count_distinct_vars(sub.get(count).vars, vars);
						count++;
					}
					else if (checkType == GlobalVariable.DIAMOND)
					{
						sub.add(new DiamondFormula(newSub, GlobalVariable.DIAMOND, info));
						varCount = count_distinct_vars(sub.get(count).vars, vars);
						count++;
					}
					else if (checkType == GlobalVariable.SINCE)
					{
						sub.add(new SinceFormula(newSub, GlobalVariable.SINCE, info));
						varCount = count_distinct_vars(sub.get(count).vars, vars);
						count++;
					}
					else if (checkType == GlobalVariable.EXIST)
					{
						sub.add(new ExistFormula(newSub, GlobalVariable.EXIST, info));
						varCount = count_distinct_vars(sub.get(count).vars, vars);
						count++;
					}
					else if (checkType == GlobalVariable.FORALL)
					{
						sub.add(new UniversalFormula(newSub, GlobalVariable.FORALL, info));
						varCount = count_distinct_vars(sub.get(count).vars, vars);
						count++;
					}
					else if (checkType == GlobalVariable.ATOM)
					{
						sub.add(new Atom(newSub, GlobalVariable.ATOM, info));
						varCount = count_distinct_vars(sub.get(count).vars, vars);
						count++;
					}
				}
			}			
		}
		else
		{
			// Otherwise only unary operator, or atomic operator
			if (type == GlobalVariable.DIAMONDDOT || type == GlobalVariable.DIAMOND ||
					type == GlobalVariable.EXIST || type == GlobalVariable.FORALL) 
			{
				// Assume for now it's well defined, then the metric number always start from third char to first '('
				String newSub = checkStr.substring(checkStr.indexOf('('), checkStr.length());
				String checkType = formula_type(newSub.substring(1, newSub.length() - 1));
				if (checkType == GlobalVariable.AND || checkType == GlobalVariable.OR)
				{
					sub.add(new Formula(newSub, checkType, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (checkType == GlobalVariable.DIAMONDDOT)
				{
					sub.add(new DiamondDotFormula(newSub, GlobalVariable.DIAMONDDOT, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (checkType == GlobalVariable.DIAMOND)
				{
					sub.add(new DiamondFormula(newSub, GlobalVariable.DIAMOND, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (checkType == GlobalVariable.SINCE)
				{
					sub.add(new SinceFormula(newSub, GlobalVariable.SINCE, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (checkType == GlobalVariable.EXIST)
				{
					sub.add(new ExistFormula(newSub, GlobalVariable.EXIST, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (checkType == GlobalVariable.FORALL)
				{
					sub.add(new UniversalFormula(newSub, GlobalVariable.FORALL, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (checkType == GlobalVariable.ATOM)
				{
					sub.add(new Atom(newSub, GlobalVariable.ATOM, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
			}
			// Atomic operator (TODO : Not operator)
		}
	}
	
	public Formula(Element subformula, String type, MetaFormula info)
	{
		sub = new ArrayList<Formula>();
		vars = new ArrayList<String>();
		this.type = type;
		NodeList childs = subformula.getChildNodes();
		int len = childs.getLength();
		for (int i = 0; i < len; i++)
		{
			Node child = childs.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE)
			{
				if (child.getNodeName() == GlobalVariable.ATOM)
				{
					sub.add(new Atom((Element)child, GlobalVariable.ATOM, info));
					varCount = count_distinct_vars_for_atom(sub.get(count).vars, ((Atom)sub.get(count)).var_type, vars);
					count++;
				}
				else if (child.getNodeName() == GlobalVariable.NOT)
				{
					sub.add(new Negation((Element)child, GlobalVariable.NOT, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (child.getNodeName() == GlobalVariable.DIAMONDDOT)
				{
					sub.add(new DiamondDotFormula((Element)child, GlobalVariable.DIAMONDDOT, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (child.getNodeName() == GlobalVariable.DIAMOND)
				{
					sub.add(new DiamondFormula((Element)child, GlobalVariable.DIAMOND, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (child.getNodeName() == GlobalVariable.SINCE)
				{
					sub.add(new SinceFormula((Element)child, GlobalVariable.SINCE, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (child.getNodeName() == GlobalVariable.EXIST)
				{
					sub.add(new ExistFormula((Element)child, GlobalVariable.EXIST, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				else if (child.getNodeName() == GlobalVariable.FORALL)
				{
					sub.add(new UniversalFormula((Element)child, GlobalVariable.FORALL, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				if (child.getNodeName() == GlobalVariable.AND)
				{
					sub.add(new Formula((Element)child, GlobalVariable.AND, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
				if (child.getNodeName() == GlobalVariable.OR)
				{
					sub.add(new Formula((Element)child, GlobalVariable.OR, info));
					varCount = count_distinct_vars(sub.get(count).vars, vars);
					count++;
				}
			}
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");
		if (count > 0)
		{
			sb.append(sub.get(0).toString());
		}
		for (int i = 1; i < count; i++)
		{
			sb.append(" " + type + " ");
			sb.append(sub.get(i).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public void append_formula(Document dom, Element Ele)
	{
		Element subEle = dom.createElement(type);
		Ele.appendChild(subEle);
		
		for(int i = 0; i < count; i++)
		{
			sub.get(i).append_formula(dom, subEle);
		}
	}
}

class DiamondDotFormula extends Formula
{
	public int metric;
	public DiamondDotFormula(String subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		// Assume well defined, the metric always start from the third character to the first '('
		String checkStr = subformula.substring(1, subformula.length() - 1);
		metric = Integer.parseInt(checkStr.substring(2, checkStr.indexOf('(')));
		if (metric >= 0) info.insert_metric(metric);
	}
	
	public DiamondDotFormula(Element subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		NodeList nl = subformula.getElementsByTagName(GlobalVariable.METRIC);
		if (nl != null && nl.getLength() > 0)
		{
			metric = Integer.parseInt(((Element)nl.item(0)).getTextContent().trim());
			if (metric >= 0) info.insert_metric(metric);
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("DiamondDot");
		if (metric >= 0) sb.append("_" + metric);
		sb.append("(");
		if (count > 0)
		{
			sb.append(sub.get(0).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public void append_formula(Document dom, Element Ele)
	{
		Element pastEle = dom.createElement(type);
		Ele.appendChild(pastEle);
		
		Element metricEle = dom.createElement(GlobalVariable.METRIC);
		metricEle.setTextContent(String.valueOf(metric));
		pastEle.appendChild(metricEle);
		
		// Assume well formed, every past formula has exactly 1 sub formula
		sub.get(0).append_formula(dom, pastEle);
	}
}

class DiamondFormula extends Formula
{
	public int metric;
	public DiamondFormula(String subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		// Assume well defined, the metric always start from the third character to the first '('
		String checkStr = subformula.substring(1, subformula.length() - 1);
		metric = Integer.parseInt(checkStr.substring(2, checkStr.indexOf('(')));
		if (metric >= 0) info.insert_metric(metric);
	}
	
	public DiamondFormula(Element subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		NodeList nl = subformula.getElementsByTagName(GlobalVariable.METRIC);
		if (nl != null && nl.getLength() > 0)
		{
			metric = Integer.parseInt(((Element)nl.item(0)).getTextContent().trim());
			if (metric >= 0) info.insert_metric(metric);
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("Diamond");
		if (metric >= 0) sb.append("_" + metric);
		sb.append("(");
		if (count > 0)
		{
			sb.append(sub.get(0).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public void append_formula(Document dom, Element Ele)
	{
		Element pastEle = dom.createElement(type);
		Ele.appendChild(pastEle);
		
		Element metricEle = dom.createElement(GlobalVariable.METRIC);
		metricEle.setTextContent(String.valueOf(metric));
		pastEle.appendChild(metricEle);
		
		// Assume well formed, every past formula has exactly 1 sub formula
		sub.get(0).append_formula(dom, pastEle);
	}
}

class SinceFormula extends Formula
{
	public int metric;
	public SinceFormula(String subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		// Assume well defined, the metric always start from the third character to the first '('
		String checkStr = subformula.substring(1, subformula.length() - 1);
		metric = Integer.parseInt(checkStr.substring(2, checkStr.indexOf('(')));
		if (metric >= 0) info.insert_metric(metric);
	}
	
	public SinceFormula(Element subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		NodeList nl = subformula.getElementsByTagName(GlobalVariable.METRIC);
		if (nl != null && nl.getLength() > 0)
		{
			metric = Integer.parseInt(((Element)nl.item(0)).getTextContent().trim());
			if (metric >= 0) info.insert_metric(metric);
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if (count > 0)
		{
			sb.append(sub.get(0).toString());
		}
		sb.append(")");
		sb.append("Since");
		if (metric >= 0) sb.append("_" + metric);
		sb.append("(");
		if (count > 1)
		{
			sb.append(sub.get(1).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public void append_formula(Document dom, Element Ele)
	{
		Element pastEle = dom.createElement(type);
		Ele.appendChild(pastEle);
		
		Element metricEle = dom.createElement(GlobalVariable.METRIC);
		metricEle.setTextContent(String.valueOf(metric));
		pastEle.appendChild(metricEle);
		
		// Assume well formed, every past formula has exactly 1 sub formula
		sub.get(0).append_formula(dom, pastEle);
	}
}

class UniversalFormula extends Formula
{
	public ArrayList<String> varIndex;
	public int varIndexCount;
	public UniversalFormula(String subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		// Assume well defined, the vars always start from the 8th character to the first '('
		String checkStr = subformula.substring(1, subformula.length() - 1);
		String varList = checkStr.substring(7, checkStr.indexOf('('));
		// Separated by ','
		String[] temp = varList.split(",");
		varIndex = new ArrayList<String>();
		varIndexCount = 0;
		for (int i = 0; i < temp.length; i++)
		{
			varIndex.add(temp[i].trim());
			varIndexCount++;
		}
		remove_index();
	}
	
	public UniversalFormula(Element subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		// Get the var list
		NodeList nl = subformula.getChildNodes();
		varIndex = new ArrayList<String>();
		varIndexCount = 0;
		for (int i = 0; i < nl.getLength(); i++)
		{
			Node child = nl.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName() == GlobalVariable.VAR)
			{
				varIndex.add(child.getTextContent().trim());
				varIndexCount++;
			}
		}
		remove_index();
	}
	
	private void remove_index()
	{
		HashSet<String> counter = new HashSet<String>();
		for (int i = 0; i < varIndexCount; i++)
		{
			counter.add(varIndex.get(i));
		}
		for (int i = 0; i < varCount; i++)
		{
			if (!counter.contains(vars.get(i)))
			{
				counter.add(vars.get(i));
			}
			else
			{
				vars.remove(i);
				i--;
				varCount--;
			}
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		// assume well formed, there's are at least 1 variable
		sb.append("forall_" + varIndex.get(0));
		for (int i = 1; i < varIndexCount; i++)
		{
			sb.append("," + varIndex.get(i));
		}
		sb.append("(");
		if (count > 0)
		{
			sb.append(sub.get(0).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public void append_formula(Document dom, Element Ele)
	{
		Element existEle = dom.createElement(type);
		Ele.appendChild(existEle);
		
		for (int i = 0; i < varIndexCount; i++)
		{
			Element varEle = dom.createElement(GlobalVariable.VAR);
			varEle.setTextContent(varIndex.get(i));
			existEle.appendChild(varEle);
		}
		
		// Assume well formed, every exist formula has exactly 1 sub formula
		sub.get(0).append_formula(dom, existEle);
	}
}

class ExistFormula extends Formula
{
	public ArrayList<String> varIndex;
	public int varIndexCount;
	public ExistFormula(String subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		// Assume well defined, the vars always start from the 7th character to the first '('
		String checkStr = subformula.substring(1, subformula.length() - 1);
		String varList = checkStr.substring(6, checkStr.indexOf('('));
		// Separated by ','
		String[] temp = varList.split(",");
		varIndex = new ArrayList<String>();
		varIndexCount = 0;
		for (int i = 0; i < temp.length; i++)
		{
			varIndex.add(temp[i].trim());
			varIndexCount++;
		}
		remove_index();
	}
	
	public ExistFormula(Element subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		// Get the var list
		NodeList nl = subformula.getChildNodes();
		varIndex = new ArrayList<String>();
		varIndexCount = 0;
		for (int i = 0; i < nl.getLength(); i++)
		{
			Node child = nl.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName() == GlobalVariable.VAR)
			{
				varIndex.add(child.getTextContent().trim());
				varIndexCount++;
			}
		}
		remove_index();
	}
	
	private void remove_index()
	{
		HashSet<String> counter = new HashSet<String>();
		for (int i = 0; i < varIndexCount; i++)
		{
			counter.add(varIndex.get(i));
		}
		for (int i = 0; i < varCount; i++)
		{
			if (!counter.contains(vars.get(i)))
			{
				counter.add(vars.get(i));
			}
			else
			{
				vars.remove(i);
				i--;
				varCount--;
			}
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		// assume well formed, there's are at least 1 variable
		sb.append("exist_" + varIndex.get(0));
		for (int i = 1; i < varIndexCount; i++)
		{
			sb.append("," + varIndex.get(i));
		}
		sb.append("(");
		if (count > 0)
		{
			sb.append(sub.get(0).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public void append_formula(Document dom, Element Ele)
	{
		Element existEle = dom.createElement(type);
		Ele.appendChild(existEle);
		
		for (int i = 0; i < varIndexCount; i++)
		{
			Element varEle = dom.createElement(GlobalVariable.VAR);
			varEle.setTextContent(varIndex.get(i));
			existEle.appendChild(varEle);
		}
		
		// Assume well formed, every exist formula has exactly 1 sub formula
		sub.get(0).append_formula(dom, existEle);
	}
}

class Negation extends Formula
{
	public Negation(Element subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("!(");
		if (count > 0)
		{
			sb.append(sub.get(0).toString());
		}
		sb.append(")");
		return sb.toString();
	}
}

class Atom extends Formula
{
	public String rel;
	// Assume no duplicate of variables
	public String atom_type; // Indicate whether this atom persists among all the history
	public ArrayList<String> var_type; // Indicate whether a variable is an explicit object or free variable
	
	public Atom(String subformula, String type, MetaFormula info)
	{
		super("#", type, info);
		
		vars = new ArrayList<String>();
		var_type = new ArrayList<String>();
		
		this.type = GlobalVariable.ATOM;
		this.rel = "";
		this.varCount = 0;
		
		String checkStr = subformula.substring(1, subformula.length() - 1);
		
		if (checkStr.contains("("))
		{
			String temp[] = checkStr.split("\\(");
			this.rel = temp[0].trim();
			String temp2[] = temp[1].substring(0, temp[1].length() - 1).split(",");
			String atom_type = ""; //TODO
			info.insert_rel(rel, temp2.length, atom_type);
			for (int i = 0; i < temp2.length; i++)
			{
				vars.add(temp2[i].trim());
				this.varCount++;
				info.insert_var(temp2[i].trim());
			}
		}
		else
		{
			rel = subformula;
			String atom_type = ""; //TODO
			info.insert_rel(rel, 0, atom_type);
		}
	}
	
	public Atom(Element subformula, String type, MetaFormula info)
	{
		super(subformula, type, info);
		
		//vars = new ArrayList<String>();
		var_type = new ArrayList<String>();
		
		// Get the relation name : Assuming atom is always at the leaf
		NodeList nl = subformula.getElementsByTagName(GlobalVariable.REL);
		if (nl != null && nl.getLength() > 0)
		{
			rel = ((Element)nl.item(0)).getTextContent().trim();
		}
		
		// Get the atom type
		NodeList tl = subformula.getElementsByTagName(GlobalVariable.ATOM_TYPE);
		if (tl != null && tl.getLength() > 0)
		{
			atom_type = ((Element)tl.item(0)).getTextContent().trim();
		}
		
		// Get the list of variables
		NodeList vl = subformula.getElementsByTagName(GlobalVariable.VAR);
		if (vl != null)
		{
			info.insert_rel(rel, vl.getLength(), atom_type);
			for (int i = 0; i < vl.getLength(); i++)
			{
				String item = ((Element)vl.item(i)).getTextContent().trim();
				if (item.startsWith("object:"))
				{
					vars.add(item.substring(7));
					varCount++;
					var_type.add(GlobalVariable.OBJECT);
					info.insert_object(item.substring(7));
				}
				else
				{
					vars.add(item);
					var_type.add(GlobalVariable.FREE);
					varCount++;
					info.insert_var(item);
				}
			}
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(rel + "(");
		if (varCount > 0)
		{
			sb.append(vars.get(0));
		}
		for (int i = 1; i < varCount; i++)
		{
			sb.append(", " + vars.get(i));
		}
		sb.append(")");
		return sb.toString();
	}
	
	public void append_formula(Document dom, Element Ele)
	{
		// atom is the leaf node
		Element atomEle = dom.createElement(type);
		Ele.appendChild(atomEle);
		
		Element relEle = dom.createElement(GlobalVariable.REL);
		relEle.setTextContent(rel);
		atomEle.appendChild(relEle);
		
		for (int i = 0; i < varCount; i++)
		{
			Element varEle = dom.createElement(GlobalVariable.VAR);
			varEle.setTextContent(vars.get(i));
			atomEle.appendChild(varEle);
		}
	}
}

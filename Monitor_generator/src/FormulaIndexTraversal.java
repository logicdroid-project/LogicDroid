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

public class FormulaIndexTraversal {
	private int idx;
	private int timetag_idx;
	private String indent[];
	private String sizeArr[];
	
	public FormulaIndexTraversal(String indent[], String sizeArr[])
	{
		this.indent = indent;
		this.sizeArr = sizeArr;
		idx = 0;
		timetag_idx = 0;
	}
	
	void reset() {idx = 0; timetag_idx = 0;}
	
	private String determine_var(Formula sub, int index)
	{
		String var = sub.vars.get(index);
		// If the sub formula is an atom, there's a possibility of being an explicit object
		if (sub.type.equals(GlobalVariable.ATOM))
		{
			String var_type = ((Atom)sub).var_type.get(index);
			if (var_type.equals(GlobalVariable.OBJECT))
			{
				//var = Integer.toString(ObjectMapping.get(var).localUID); // Get the app ID from the mapping (integer)

				// Add object:localUID
				try {
					var = "mapping[" + Integer.parseInt(var) +"]";
				}
				catch (NumberFormatException e) {
					var = Integer.toString(Monitoring.getCUID_localUID(var)); // Get the app ID from the mapping (integer)
				}
			}
			// If it's a free variable then it's OK
		}
		// Else just return variable name as it is
		return var;
	}
	
	private static class FirstIndexingReturn
	{
		public int curr_indent;
		public int curr_idx;
		public String firstIdx;
		public FirstIndexingReturn(int curr_indent, int curr_idx, String firstIdx)
		{
			this.curr_idx = curr_idx;
			this.curr_indent = curr_indent;
			this.firstIdx = firstIdx;
		}
	}
	
	private FirstIndexingReturn FirstIndexing(StringBuilder formulaStr, Formula policy, int indent_index)
	{
		formulaStr.append(indent[indent_index] + "// [" + idx + "] : "+ policy.toString() + "\n");
		int curr_indent = indent_index;
		for (int i = 0; i < policy.varCount; i++)
		{
			String var = policy.vars.get(i);
			formulaStr.append(indent[curr_indent] + "for (" + var + " = 0; " + var + " < app_num; " + var + "++) {\n");
			curr_indent++;
		}
		StringBuilder firstIndex = new StringBuilder();
		if (policy.varCount > 0)
		{
			if (policy.varCount == 1)
			{
				firstIndex.append(policy.vars.get(0));
			}
			else
			{
				firstIndex.append(policy.vars.get(0) + " * " + sizeArr[policy.varCount - 1]);
			}
		}
		int len = policy.varCount;
		for (int j = 1; j < len; j ++)
		{
			if (j == len - 1)
			{
				firstIndex.append(" + " + policy.vars.get(j));
			}
			else
			{
				firstIndex.append(" + " + policy.vars.get(j) + " * " + sizeArr[len - j - 1]);
			}
		}
		if (len == 0)
		{
			firstIndex.append("0");
		}
		idx = idx + 1;
		
		return new FirstIndexingReturn(curr_indent, idx - 1, firstIndex.toString());
	}
	
	private void FirstIndexingClose(int curr_indent, StringBuilder formulaStr, int policyVarCount)
	{
		curr_indent--;
		formulaStr.append("\n");
		for (int i = 0; i < policyVarCount; i++)
		{
			formulaStr.append(indent[curr_indent] + "}\n");
			curr_indent--;
		}
	}
	
	private String getSecondIndex(Formula sub)
	{
		int len2 = sub.varCount;
		StringBuilder secondIndex = new StringBuilder();

		// Fix missing index bug
		if (len2 == 0) {
			String var2;
			try {
				var2 = determine_var(sub, 0);
			} catch (IndexOutOfBoundsException e) {
				var2 = "0";
			}
			secondIndex.append(var2);
			return secondIndex.toString();
		}	

		for (int j = 0; j < len2; j++)
		{
			String var = determine_var(sub, j);
			if (j == 0)
			{
				if (len2 > 1)
				{
					secondIndex.append(var + " * " + sizeArr[len2 - 1]);
				}
				else
				{
					secondIndex.append(var);
				}
			}
			else
			{
				if (j == len2 - 1)
				{
					secondIndex.append(" + " + var);
				}
				else
				{
					secondIndex.append(" + " + var + " * " + sizeArr[len2 - j - 1]);
				}
			}
		}
		return secondIndex.toString();
	}
	
	private void traverse_DiamondDot(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for diamond dot formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		String tmpIdx = getSecondIndex(policy.sub.get(0));
		
		if (((DiamondDotFormula)policy).metric >= 0)
		{
			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = 0;\n");
			formulaStr.append(indent[curr_indent] + "next->time_tag[" + timetag_idx + "][" + firstIndex + "] = 0;\n");
			formulaStr.append(indent[curr_indent] + "delta = (next->timestamp - prev->timestamp);\n");
			formulaStr.append(indent[curr_indent] + "if (delta <= metric[" + timetag_idx + "])\n");
			formulaStr.append(indent[curr_indent] + "{\n");
			formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
			formulaStr.append(indent[curr_indent + 1] + "next->time_tag[" + timetag_idx + "][" + firstIndex + "] = (int) delta;\n");
			formulaStr.append(indent[curr_indent + 1] + "if (!next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] && prev->time_tag[" + 
								timetag_idx + "][" + firstIndex + "] + delta <= metric[" + timetag_idx + "])\n");
			formulaStr.append(indent[curr_indent + 1] + "{\n");
			formulaStr.append(indent[curr_indent + 2] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex +"];\n");
			formulaStr.append(indent[curr_indent + 2] + "if (next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "]) next->time_tag[" + timetag_idx + "][" + 
					firstIndex.toString() + "] += prev->time_tag[" + timetag_idx + "][" + firstIndex + "];\n");
			formulaStr.append(indent[curr_indent + 1] + "}\n");
			formulaStr.append(indent[curr_indent] + "}\n");
			timetag_idx++;
		}
		else // just original LTL formula
		{
			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "] || " +
					"prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "];\n");
		}
	}
	
	private void traverse_Diamond(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for diamond formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		String tmpIdx = getSecondIndex(policy.sub.get(0));
		
		if (((DiamondFormula)policy).metric >= 0)
		{
			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
			formulaStr.append(indent[curr_indent] + "next->time_tag[" + timetag_idx + "][" + firstIndex + "] = 0;\n");
			formulaStr.append(indent[curr_indent] + "delta = (next->timestamp - prev->timestamp);\n");
			formulaStr.append(indent[curr_indent] + "if (!next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] && prev->time_tag[" + 
								timetag_idx + "][" + firstIndex + "] + delta <= metric[" + timetag_idx + "])\n");
			formulaStr.append(indent[curr_indent] + "{\n");
			formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex +"];\n");
			formulaStr.append(indent[curr_indent + 1] + "next->time_tag[" + timetag_idx + "][" + firstIndex + 
								"] = delta + prev->time_tag[" + timetag_idx + "][" + firstIndex + "];\n");
			formulaStr.append(indent[curr_indent] + "}\n");
			timetag_idx++;
		}
		else // just original LTL formula
		{
			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "] || " +
					"prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "];\n");
		}
	}
	
	private void traverse_Since(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only two sub formulas for since formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		String tmpIdx = getSecondIndex(policy.sub.get(0));
		
		
		boolean isMetric = ((SinceFormula)policy).metric >= 0;
		if (isMetric)
		{
				formulaStr.append(indent[curr_indent] + "next->time_tag[" + timetag_idx + "][" + firstIndex.toString() + "] = 0;\n");
				formulaStr.append(indent[curr_indent] + "delta = (next->timestamp - prev->timestamp);\n");
				formulaStr.append(indent[curr_indent] + "if (delta <= metric[" + timetag_idx + "])\n");
				formulaStr.append(indent[curr_indent] + "{\n");
				formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + 
						"] = (next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx +"] || ");
		}
		else
		{
			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + "] = (next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "] && ");
		}
		
		subIndex = idx;
		traverse_index(policy.sub.get(1), indent_index, result, policy_number);
		tmpIdx = getSecondIndex(policy.sub.get(1));
		
		if (isMetric)
		{
			formulaStr.append("next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + 
					"]) && (next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx +"] || " + 
					"prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + "]);\n");
			formulaStr.append(indent[curr_indent + 1] + "if (!next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "]) next->time_tag[" + timetag_idx + "][" + firstIndex.toString() + 
					"] = delta + prev->time_tag[" + timetag_idx + "][" + firstIndex.toString() + "];\n");
			formulaStr.append(indent[curr_indent] + "}\n");
			formulaStr.append(indent[curr_indent] + "else\n");
			formulaStr.append(indent[curr_indent] + "{\n");
			formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + 
					"] = next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
			formulaStr.append(indent[curr_indent] + "}\n");
			timetag_idx++;
		}
		else // just original LTL formula
		{
			formulaStr.append("prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() +  "]) || " +
					"next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
		}
	}
	
	private void traverse_Exist(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for Exists formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		
		int add_idx = curr_indent;
		ArrayList<String> varIndex = ((ExistFormula)policy).varIndex;
		int varIndexCount = ((ExistFormula)policy).varIndexCount;
		for (int j = 0; j < varIndexCount; j++)
		{
			String var = varIndex.get(j);
			formulaStr.append(indent[add_idx] + "for (" + var + " = 0; " + var + " < app_num && !next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "]; " + var + "++) {\n");
			add_idx++;
		}
		
		String secondIndex = getSecondIndex(policy.sub.get(0));
		
		formulaStr.append(indent[add_idx] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] || next->propositions[" + 
				policy_number + "][" + subIndex + "][" + secondIndex + "];\n");

		add_idx--;
		for (int j = 0; j < varIndexCount; j++)
		{
			formulaStr.append(indent[add_idx] + "}\n");
			add_idx--;
		}
	}
	
	private void traverse_Forall(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for Forall formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		
		int add_idx = curr_indent;
		ArrayList<String> varIndex = ((UniversalFormula)policy).varIndex;
		int varIndexCount = ((UniversalFormula)policy).varIndexCount;
		for (int j = 0; j < varIndexCount; j++)
		{
			String var = varIndex.get(j);
			formulaStr.append(indent[add_idx] + "for (" + var + " = 0; " + var + " < app_num; " + var + "++) {\n");
			add_idx++;
		}
		
		String secondIndex = getSecondIndex(policy.sub.get(0));
		
		formulaStr.append(indent[add_idx] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] && next->propositions[" + 
				policy_number + "][" + subIndex + "][" + secondIndex + "];\n");

		add_idx--;
		for (int j = 0; j < varIndexCount; j++)
		{
			formulaStr.append(indent[add_idx] + "}\n");
			add_idx--;
		}
	}
	
	private void traverse_Not(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for Not formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		
		formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = !next->propositions[" + policy_number + "][" + subIndex + "][" + firstIndex + "];");
	}
	
	private void traverse_AndOr(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr, String operator)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		for (int i = 0; i < policy.count; i++)
		{
			int subIndex = idx;
			traverse_index(policy.sub.get(i), indent_index, result, policy_number);
			String secondIndex = getSecondIndex(policy.sub.get(i));
			
			if (i == 0)
			{
				formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + subIndex + "][" + secondIndex + "]");
			}
			else
			{
				formulaStr.append(" " + operator + " next->propositions[" + policy_number + "][" + subIndex + "][" + secondIndex + "]"); 
			}
			if (i == (policy.count - 1)) formulaStr.append(";");
		}
	}
	
	public void traverse_index(Formula policy, int indent_index, ArrayList<String> result, int policy_number)
	{
		if (policy.type.equals(GlobalVariable.ATOM)) {idx = idx + 1; return;}
		
		StringBuilder formulaStr = new StringBuilder();
		
		FirstIndexingReturn ret = FirstIndexing(formulaStr, policy, indent_index);
		int curr_indent = ret.curr_indent;
		
		if (policy.type.equals(GlobalVariable.DIAMONDDOT)) traverse_DiamondDot(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.DIAMOND)) traverse_Diamond(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.SINCE)) traverse_Since(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.EXIST)) traverse_Exist(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.FORALL)) traverse_Forall(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.NOT)) traverse_Not(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.AND)) traverse_AndOr(policy, indent_index, result, policy_number, ret, formulaStr, "&&");
		else if (policy.type.equals(GlobalVariable.OR)) traverse_AndOr(policy, indent_index, result, policy_number, ret, formulaStr, "||");
		
		FirstIndexingClose(curr_indent, formulaStr, policy.varCount);
		
		result.add(formulaStr.toString());
	}
}

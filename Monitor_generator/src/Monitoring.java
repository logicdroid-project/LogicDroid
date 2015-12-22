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

// The new monitoring targets to create a kernel space monitor

import java.io.*;
import java.util.*;

public class Monitoring {
	private static String[] sizeArr = new String[11];
	private static String[] indent = new String[10];
	private static int startVirtualUID = 1100;
	private static int idx;
	private static int timetag_idx;
	
	static class C_UID // this class is to address the problem of mapping using standard C which doesn't have class mapping
	{
		public int virtualUID;
		public int tempUID;
		public int localUID;
		
		public C_UID()
		{
			virtualUID = -1;
			tempUID = -1;
			localUID = -1;
		}
		
		public C_UID(int virtualUID, int tempUID, int localUID)
		{
			this.virtualUID = virtualUID;
			this.tempUID = tempUID;
			this.localUID = localUID;
		}
	}
	
	// for the purpose of initialising atoms
	private static HashMap<String, Integer> mapping;
	private static ArrayList<String> policyObjects;
	private static int policyObjectCount;
	private static HashMap<String, C_UID> ObjectMapping; // to map an explicit object to a temp UID
	
	public static int getCUID_localUID(String var)
	{
		return ObjectMapping.get(var).localUID;
	}
	
	public static void set_virtual_UID(Policy policy_list)
	{
		policyObjects = new ArrayList<String>();
		policyObjectCount = 0;//policy_list.objectCount;

		policyObjects.add("internet");
		policyObjectCount++;
		policyObjects.add("sms");
		policyObjectCount++;
		policyObjects.add("location");
		policyObjectCount++;
		policyObjects.add("contact");
		policyObjectCount++;
		policyObjects.add("hangup");
		policyObjectCount++;
		policyObjects.add("call_privileged");
		policyObjectCount++;
		policyObjects.add("imei");
		policyObjectCount++;
		for (int i = 0; i < policy_list.objectCount; i++) {
			// Object with fix localAppUID should not be added
			try{
				Integer.parseInt(policy_list.objects.get(i));
				continue;
			}
			catch (NumberFormatException e){
				// Not object:fixLocalAppUID
			}
			if (!policyObjects.contains(policy_list.objects.get(i).toLowerCase())) 
			{
				policyObjects.add(policy_list.objects.get(i));
				policyObjectCount++;
			}
		}
		
		ObjectMapping = new HashMap<String, C_UID>();
		for (int i = 0; i < policyObjectCount; i++)
		{
			ObjectMapping.put(policyObjects.get(i), new C_UID(i + startVirtualUID, i + 3, i + 1)); 
			// map objects to an application ID : virtual UID starting from 1100
		}
	}
	
	public static void generate_kernel_monitor(Policy policy_list, String fileName)
	{
		// Assume that we are given the root formula as the first policy in the list
		// the file name is going to be appended with .java later
		StringBuilder indentSb = new StringBuilder();
		
		/* 
		 * sizeArr is a pre-initialized size indices for the arrays, so for example :
		 *   - sizeArr[0] = "app_num";
		 *   - sizeArr[1] = "app_num * app_num";
		 *   - sizeArr[2] = "app_num * app_num * app_num";
		 *   
		 * This is due to the observation that all the variables depends on the domain which in this context is the number of applications
		 */
		StringBuilder sizeSb = new StringBuilder("app_num");
		sizeArr[0] = "";
		for (int i = 0; i < indent.length; i++)
		{
			indent[i] = indentSb.toString();
			sizeArr[i + 1] = sizeSb.toString();
			indentSb.append("  ");
			sizeSb.append(" * app_num");
		}
		
		try
		{
			FileOutputStream stream = new FileOutputStream(fileName + "_module.c");
			PrintStream ps = new PrintStream(stream);
			
			int num_temp = policy_list.tempCount;
			ArrayList<Integer> metric = policy_list.metrics;
			
			//String permissionString = ""; // TODO : have to know how to get the permission list from the android
			
			mapping = new HashMap<String, Integer>();
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				mapping.put(policy_list.relations.get(i), i);
			}
			
			// This metricStr is the metric for each temporal operators
			StringBuilder metricStr = new StringBuilder();
			if (metric.size() > 0) metricStr.append(metric.get(0));
			for (int i = 1; i < metric.size(); i++)
			{
				metricStr.append(", " + metric.get(i));
			}
			
			ps.println(indent[0] + "#include<linux/kernel.h>");
			ps.println(indent[0] + "#include<linux/slab.h>");
			ps.println(indent[0] + "#include<linux/mutex.h>");
			ps.println();
			ps.println(indent[0] + "#define MAX_APPLICATION 100000");
			ps.println();
			ps.println(indent[0] + "typedef struct tHistory {");
			ps.println(indent[1] + "char ***propositions;");
			ps.println(indent[1] + "char **atoms;");
			ps.println(indent[1] + "int **time_tag;");
			ps.println(indent[1] + "long timestamp;");
			ps.println(indent[0] + "} History;");
			ps.println();
			ps.println(indent[0] + "#define ROOT_UID 0");
			// fixed mappings for static object from real UID to tempUID e.g. : case 1100 : return 3;
			for (int i = 0; i < policyObjectCount; i++)
			{
				C_UID temp = ObjectMapping.get(policyObjects.get(i));
				ps.println(indent[0] + "#define " + policyObjects.get(i).toUpperCase() + "_UID " + temp.virtualUID + "");
			}
			ps.println();
			ps.println(indent[0] + "static char notInitialized = 1;");
			ps.println(indent[0] + "static int mapping[MAX_APPLICATION];");
			
			// Handles static variables declaration
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[0] + "static char *" + policy_list.relations.get(i) + ";");
				}
			}
			ps.println(indent[0] + "static char** relations;");
			ps.println(indent[0] + "static int relationSize = 0;");
			ps.println(indent[0] + "static int app_num;");
			ps.println(indent[0] + "static int module_policyID = " + policy_list.ID + ";");

			ps.println();
			if (num_temp > 0)
			{
				ps.println(indent[0] + "static int num_temp = " + num_temp + ";");
				ps.println(indent[0] + "static long metric[" + num_temp + "] = {" + metricStr.toString() + "};");
				ps.println();
			}
			ps.println(indent[0] + "static int currentHist = 0;");
			ps.println();
			
			ps.println(indent[0] + "int LogicDroid_Module_renewMonitorVariable(int* UID, int varCount, char value, int rel);");
			ps.println(indent[0] + "int LogicDroid_Module_initializeMonitor(int *UID, int appCount);");
			ps.println(indent[0] + "int LogicDroid_Module_checkEvent(int rel, int *UID, int varCount, long timestamp);");
		
			ps.println();
			ps.println(indent[0] + "extern void LogicDroid_registerMonitor(int (*renewMonitorVariable)(int*, int, char, int),");
			ps.println(indent[4] + "int (*initializeMonitor) (int*, int),");
			ps.println(indent[4] + "int (*checkEvent)(int, int*, int, long),");
			ps.println(indent[4] + "char **module_relations, int module_relationSize, int module_policyID);");
			ps.println(indent[0] + "extern void LogicDroid_unregisterMonitor(void);");
			ps.println(indent[0] + "extern void LogicDroid_setIDs(int, int);");
			// ##########################################################
			ps.println();
			ps.println(indent[0] + "History* History_Constructor(long timestamp);");
			ps.println(indent[0] + "void History_Reset(History *h);");
			ps.println(indent[0] + "void History_insertEvent(History *h, int rel, int idx);");
			ps.println(indent[0] + "char History_Process(History *next, History *prev);");
			ps.println(indent[0] + "void History_Dispose(History *h);");
			ps.println();
			
			ps.println(indent[0] + "DEFINE_MUTEX(lock);");
			ps.println();
			
			ps.println(indent[0] + "History **hist = NULL;");
			ps.println();
			
			ps.println(indent[0] + "int LogicDroid_Module_renewMonitorVariable(int *UID, int varCount, char value, int rel) {");
			ps.println(indent[1] + "int varIdx = 0;");
			ps.println(indent[1] + "int mul = 1;");
			ps.println(indent[1] + "int i = 0;");
			ps.println();
			ps.println();
			ps.println(indent[1] + "if (notInitialized)");
			ps.println(indent[1] + "{");
			ps.println(indent[2] + "return 0;");
			ps.println(indent[1] + "}");
			ps.println();
			ps.println(indent[1] + "mutex_lock(&lock);"); 
			ps.println(indent[1] + "for (i = varCount - 1; i >= 0; mul *= app_num, i--)");
			ps.println(indent[1] + "{");
			ps.println(indent[2] + "varIdx += mapping[UID[i]] * mul;");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "hist[currentHist]->atoms[rel][varIdx] = value;");
			ps.println(indent[1] + "mutex_unlock(&lock);"); 
			ps.println(indent[1] + "return 0;");
			ps.println(indent[0] + "}");
			
			ps.println();
			
			ps.println(indent[0] + "int LogicDroid_Module_initializeMonitor(int *UID, int appCount) {");
			ps.println(indent[1] + "int appIdx = " + (policyObjectCount + 1) + ";");
			ps.println(indent[1] + "int i;");
			ps.println(indent[1] + "mutex_lock(&lock);"); 
			ps.println();
			ps.println(indent[1] + "printk(\"initializing Monitor for %d applications\\n\", appCount);");
			ps.println();
			ps.println(indent[1] + "mapping[0] = 0;");
			
			for (int i = 0; i < policyObjectCount; i++)
			{
				C_UID temp = ObjectMapping.get(policyObjects.get(i));
				ps.println(indent[1] + "mapping[" + policyObjects.get(i).toUpperCase() + "_UID] = " + temp.localUID + ";");
			}
			
			ps.println(indent[1] + "app_num = appCount + " + (policyObjectCount + 1) + ";");
			ps.println();
			// Free static variable
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[1] + "kfree(" + policy_list.relations.get(i) + ");");
				}
			}
			ps.println();
			// Allocate space for all static variable
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[1] + policy_list.relations.get(i) + " = (char*) kmalloc(sizeof(char) * " + 
							sizeArr[policy_list.relationsCard.get(i)] + ", GFP_KERNEL);");
					ps.println(indent[1] + "memset(" + policy_list.relations.get(i) + ", 0, sizeof(char) * app_num);");
				}
			}
			ps.println();
			ps.println(indent[1] + "for (i = 0; i < appCount; i++)");
			ps.println(indent[1] + "{");

			ps.println(indent[2] + "mapping[UID[i]] = appIdx++;");
			ps.println(indent[1] + "}");
			ps.println();
			{
				ps.println(indent[1] + "if (hist == NULL)");
				ps.println(indent[1] + "{");
				ps.println(indent[2] + "hist = (History**) kmalloc(sizeof(History*) * 2, GFP_KERNEL);");
				ps.println(indent[2] + "hist[0] = NULL;");
				ps.println(indent[2] + "hist[1] = NULL;");
				ps.println(indent[1] + "}");
			}
			ps.println();
			ps.println(indent[1] + "History_Dispose(hist[0]);");
			ps.println(indent[1] + "History_Dispose(hist[1]);");
			ps.println(indent[1] + "hist[0] = History_Constructor(0);");
			ps.println(indent[1] + "hist[1] = History_Constructor(0);");
			ps.println(indent[1] + "History_Reset(hist[0]);");
			ps.println(indent[1] + "History_Reset(hist[1]);");
			ps.println();

			int callRelationID = -1;
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relations.get(i).equalsIgnoreCase("call") || policy_list.relations.get(i).equalsIgnoreCase("calls"))
				{
					callRelationID = i;
					break;
				}
			}
			ps.println(indent[1] + "LogicDroid_setIDs(" + callRelationID + ", INTERNET_UID);");

			ps.println(indent[1] + "currentHist = 0;");
			ps.println(indent[1] + "notInitialized = 0;");
			ps.println(indent[1] + "mutex_unlock(&lock);"); 
			ps.println(indent[1] + "return module_policyID;");
			ps.println(indent[0] + "}");
			
			ps.println();
			ps.println(indent[0] + "int LogicDroid_Module_checkEvent(int rel, int *UID, int varCount, long timestamp) {");
			ps.println(indent[1] + "int varIdx = 0;");
			ps.println(indent[1] + "int mul = 1;");
			ps.println(indent[1] + "int i = 0;");
			ps.println(indent[1] + "char result;");
			ps.println(indent[1] + "if (notInitialized)");
			ps.println(indent[1] + "{");
			ps.println(indent[2] + "return 0;");
			ps.println(indent[1] + "}");
			ps.println();

			ps.println(indent[1] + "mutex_lock(&lock);"); 
			ps.println(indent[1] + "History_Reset(hist[!currentHist]);");
			ps.println(indent[1] + "currentHist = !currentHist;");
			ps.println(indent[1] + "hist[currentHist]->timestamp = timestamp;");
			ps.println();
			ps.println(indent[1] + "for (i = varCount - 1; i >= 0; mul *= app_num, i--)");
			ps.println(indent[1] + "{");
			
			ps.println(indent[2] + "varIdx += mapping[UID[i]] * mul;");
			
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "History_insertEvent(hist[currentHist], rel, varIdx);");
			ps.println(indent[1] + "result = History_Process(hist[currentHist], hist[!currentHist]);");
			ps.println(indent[1] + "if (result)");
			ps.println(indent[1] + "{");

			ps.println(indent[2] + "currentHist = !currentHist;");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "mutex_unlock(&lock);"); 
			ps.println();
			ps.println(indent[1] + "return result;");
			ps.println(indent[0] + "}");
			ps.println();
			
			ps.println(indent[0] + "History* History_Constructor(long timestamp) {");
			ps.println(indent[1] + "History *retVal = (History*) kmalloc(sizeof(History), GFP_KERNEL);");
			ps.println();
			ps.println(indent[1] + "retVal->atoms = (char**) kmalloc(sizeof(char*) * " + policy_list.relationCount + ", GFP_KERNEL);");
			StringBuilder memsetSb = new StringBuilder();
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.DYNAMIC))
				{
					ps.println(indent[1] + "retVal->atoms[" + i + "] = (char*) kmalloc(sizeof(char) * " + 
							sizeArr[policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)))] + ", GFP_KERNEL); // " + policy_list.relations.get(i));
					memsetSb.append(indent[1] + "memset(h->atoms[" + i + "], 0, sizeof(char) * " + 
							sizeArr[policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)))] + ");\n");
				}
				else if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[1] + "retVal->atoms[" + i + "] = " + policy_list.relations.get(i) + "; // " + policy_list.relations.get(i));
				}
			}
			memsetSb.append("\n");
			ps.println(indent[1] + "retVal->propositions = (char***) kmalloc(sizeof(char**) * " + policy_list.formulaCount + ", GFP_KERNEL);");
			// Traverse all the formula for initialising the space
			StringBuilder spaceSb = new StringBuilder();
			idx = 0;
			ArrayList<ArrayList<Integer>> atomsIdx = new ArrayList<ArrayList<Integer>>(); // This is used for history disposal, as we need to know which propositions referring to the static data
			atomsIdx.add(new ArrayList<Integer>());
			traverse_space(policy_list.formulas.get(0), spaceSb, memsetSb, indent[1], 0, atomsIdx.get(0));
			// idx (a "global" variable) resulting from traverse_space will tell us how many sub formulas are there
			ps.println(indent[1] + "retVal->propositions[0] = (char**) kmalloc(sizeof(char*) * " + idx + ", GFP_KERNEL);");
			ArrayList<Integer> propositionIdx = new ArrayList<Integer>();
			propositionIdx.add(idx);
			for (int i = 1; i < policy_list.formulaCount; i++)
			{
				idx = 0;
				atomsIdx.add(new ArrayList<Integer>());
				traverse_space(policy_list.formulas.get(i), spaceSb, memsetSb, indent[1], i, atomsIdx.get(i));
				ps.println(indent[1] + "retVal->propositions[" + i + "] = (char**) kmalloc(sizeof(char*) * " + idx + ", GFP_KERNEL);");
				ps.println(indent[1] + "retVal->propositions[" + i + "][0] = retVal->atoms[" + mapping.get(policy_list.target_recursive.get(i)) + "];");
				propositionIdx.add(idx);
				atomsIdx.get(i).add(mapping.get(policy_list.target_recursive.get(i)));
			}
			ps.println();
			ps.println(spaceSb.toString());
			// End Traverse
			
			if (num_temp > 0)
			{
				ps.println(indent[1] + "retVal->time_tag = (int**) kmalloc(sizeof(int*) * num_temp, GFP_KERNEL);");
				// Traverse all the formula for temporal operator
				idx = 0;
				spaceSb = new StringBuilder();
				memsetSb.append("\n");
				for (int i = policy_list.formulaCount - 1; i >= 0; i--)
				{
					traverse_timetag(policy_list.formulas.get(i), spaceSb, memsetSb, indent[1]);
				}
				ps.println(spaceSb.toString());
				// End Traverse
			}
			ps.println(indent[1] + "retVal->timestamp = timestamp;");
			ps.println();
			ps.println(indent[1] + "return retVal;");
			ps.println(indent[0] + "}");
			
			ps.println(indent[0] + "void History_Reset(History *h)");
			ps.println(indent[0] + "{");
			ps.print(memsetSb.toString());
			ps.println(indent[0] + "}");
			
			ps.println();
			ps.println(indent[0] + "void History_insertEvent(History *h, int rel, int idx) {");
			ps.println(indent[1] + "h->atoms[rel][idx] = 1;");
			ps.println(indent[0] + "}");
			
			ps.println();
			
			ps.println(indent[0] + "char History_Process(History *next, History *prev) {");
			
			StringBuilder vars = new StringBuilder();
			HashSet<String> usedVars = new HashSet<String>();
			for (int i = policy_list.formulaCount - 1; i >= 0; i--)
			{
				traverse_vars(policy_list.formulas.get(i), vars, usedVars, policyObjects);
			}
			// Fix missing index bug
			if (vars.length() > 0) {
				vars.delete(vars.length() - 1, vars.length()); // removing the last ','
				ps.println(indent[1] + "int" + vars.toString() + ";");
			}
			if (num_temp > 0) ps.println(indent[1] + "long delta;");
			ps.println();
			
			// Establishing the relations between the sub formulas
			ArrayList<String> result = new ArrayList<String>();
			FormulaIndexTraversal indexTraverser = new FormulaIndexTraversal(indent, sizeArr);
			for (int i = policy_list.formulaCount - 1; i >= 0; i--)
			{

				indexTraverser.reset();
				indexTraverser.traverse_index(policy_list.formulas.get(i), 1, result, i);
			}
			for (int i = 0; i < result.size(); i++)
			{
				ps.println(result.get(i));
			}
			// Finish Establishing the relations between sub formulas
		
			ps.println(indent[1] + "return next->propositions[0][0][0];"); // The result of the monitoring always stored in the root formula
			ps.println(indent[0] + "}");
			
			ps.println();
			
			ps.println(indent[0] + "// Additional function to clean up garbage");
			ps.println(indent[0] + "void History_Dispose(History *h) {");
			ps.println(indent[1] + "int i;");
			ps.println();
			ps.println(indent[1] + "if (h == NULL) return;");
			ps.println();
			ps.println(indent[1] + "// Don't remove the actual data aliased by the variables");
			for (int i = 0; i < atomsIdx.size(); i++)
			{
				ArrayList<Integer> temp = atomsIdx.get(i);
				for (int j = 0; j < temp.size(); j++)
				{
					ps.println(indent[1] + "h->propositions[" + i + "][" + temp.get(j) + "] = NULL;");
				}
			}
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[1] + "h->atoms[" + i + "] = NULL;");
				}
			}
			ps.println();
			ps.println(indent[1] + "// clean propositions");
			for (int i = 0; i < policy_list.formulaCount; i++)
			{
				ps.println(indent[1] + "for (i = 0; i < " +  propositionIdx.get(i) + "; i++)");
				ps.println(indent[1] + "{");
				ps.println(indent[2] + "kfree(h->propositions[" + i + "][i]);");
				ps.println(indent[1] + "}");
				ps.println(indent[1] + "kfree(h->propositions[" + i + "]);");
			}
			ps.println(indent[1] + "kfree(h->propositions);");
			ps.println();
			ps.println(indent[1] + "// clean atoms");
			ps.println(indent[1] + "for (i = 0; i < " + policy_list.relationCount + "; i++)");
			ps.println(indent[1] + "{");
			ps.println(indent[2] + "kfree(h->atoms[i]);");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "kfree(h->atoms);");
			ps.println();
			if (num_temp > 0)
			{
				ps.println(indent[1] + "// clean temporal metric");
				ps.println(indent[1] + "for (i = 0; i < num_temp; i++)");
				ps.println(indent[1] + "{");
				ps.println(indent[2] + "kfree(h->time_tag[i]);");
				ps.println(indent[1] + "}");
				ps.println(indent[1] + "kfree(h->time_tag);");
				ps.println();
			}
			ps.println(indent[1] + "// finally free the history reference");
			ps.println(indent[1] + "kfree(h);");
			ps.println(indent[0] + "}");
			
			ps.println();

			ps.println(indent[0] + "int __init init_monitor_module(void)");
			ps.println(indent[0] + "{");
			ps.println(indent[1] + "printk(KERN_INFO \"Attaching the policy " + policy_list.formulas.get(0) + "\\n\");");
			ps.println(indent[1] + "relations = (char**) kmalloc(sizeof(char*) * " + policy_list.relationCount + ", GFP_KERNEL);");
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				ps.println(indent[1] + "relations[" + i + "] = \"" + policy_list.relations.get(i) + "\";");
			}
			ps.println(indent[1] + "relationSize = " + policy_list.relations.size() + ";");
			ps.println(indent[1] + "LogicDroid_registerMonitor(&LogicDroid_Module_renewMonitorVariable,"); 
			ps.println(indent[2] + "&LogicDroid_Module_initializeMonitor,");
			ps.println(indent[2] + "&LogicDroid_Module_checkEvent,");
			ps.println(indent[2] + "relations, relationSize, module_policyID);");
			ps.println(indent[1] + "return 0;");
			ps.println(indent[0] + "}");
			ps.println();
			ps.println(indent[0] + "void __exit cleanup_monitor_module(void)");
			ps.println(indent[0] + "{");
			ps.println(indent[1] + "printk(KERN_INFO \"Detaching the policy from the monitor stub in kernel\\n\");");
			ps.println(indent[1] + "kfree(relations);");
			ps.println(indent[1] + "History_Dispose(hist[0]);");
			ps.println(indent[1] + "History_Dispose(hist[1]);");
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[1] + "kfree(" + policy_list.relations.get(i) + ");");
				}
			}
			ps.println(indent[1] + "LogicDroid_unregisterMonitor();");
			ps.println(indent[0] + "}");
			ps.println();
			ps.println(indent[0] + "module_init(init_monitor_module);");
			ps.println(indent[0] + "module_exit(cleanup_monitor_module);");
			
			ps.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	private static void traverse_space(Formula policy, StringBuilder sb, StringBuilder memsetSb, String indent, int policy_number, ArrayList<Integer> atomsIdx)
	{
		if (policy.type.equals(GlobalVariable.ATOM))
		{
			int atomNumber = mapping.get(((Atom)policy).rel);
			sb.append(indent + "retVal->propositions[" + policy_number + "][" + idx + "] = retVal->atoms[" + atomNumber + "];\n");
			atomsIdx.add(idx);
		}
		else if (policy_number == 0 || idx > 0) // the head of the recursive formulas will be directed towards the atomic formula
		{
			if (policy.varCount > 0)
			{
				sb.append(indent + "retVal->propositions[" + policy_number + "][" + idx + "] = (char*) kmalloc(sizeof(char) * " + sizeArr[policy.varCount] + ", GFP_KERNEL);\n");
				memsetSb.append(indent + "memset(h->propositions[" + policy_number + "][" + idx + "], 0, " + sizeArr[policy.varCount] + ");\n");
			}
			else if (policy.varCount == 0)
			{
				sb.append(indent + "retVal->propositions[" + policy_number + "][" + idx + "] = (char*) kmalloc(sizeof(char), GFP_KERNEL);\n");
				memsetSb.append(indent + "memset(h->propositions[" + policy_number + "][" + idx + "], 0, 1);\n");
			}
		}
		idx = idx + 1;
		for (int i = 0; i < policy.count; i++)
		{
			traverse_space(policy.sub.get(i), sb, memsetSb, indent, policy_number, atomsIdx);
		}
	}
	
	private static void traverse_timetag(Formula policy, StringBuilder sb, StringBuilder memsetSb, String indent)
	{
		if (policy.type == GlobalVariable.DIAMONDDOT)
		{
			if (((DiamondDotFormula)policy).metric >= 0) // metric -1 means that it is original LTL formula
			{
				if (policy.varCount > 0)
				{
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) kmalloc(sizeof(int) * " + sizeArr[policy.varCount] + ", GFP_KERNEL) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int) * " + sizeArr[policy.varCount] + ");\n");
					idx++;
				}
				else if (policy.varCount == 0)
				{
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) kmalloc(sizeof(short), GFP_KERNEL) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int));\n");
					idx++;
				}
			}
		}
		else if (policy.type == GlobalVariable.DIAMOND)
		{
			if (((DiamondFormula)policy).metric >= 0) // metric -1 means that it is original LTL formula
			{
				if (policy.varCount > 0)
				{
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) kmalloc(sizeof(int) * " + sizeArr[policy.varCount] + ", GFP_KERNEL) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int) * " + sizeArr[policy.varCount] + ");\n");
					idx++;
				}
				else if (policy.varCount == 0)
				{
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) kmalloc(sizeof(short), GFP_KERNEL) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int));\n");
					idx++;
				}
			}
		}
		else if (policy.type == GlobalVariable.SINCE)
		{
			if (((SinceFormula)policy).metric >= 0) // metric -1 means that it is original LTL formula
			{
				if (policy.varCount > 0)
				{
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) kmalloc(sizeof(int) * " + sizeArr[policy.varCount] + ", GFP_KERNEL) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int) * " + sizeArr[policy.varCount] + ");\n");
					idx++;
				}
				else if (policy.varCount == 0)
				{
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) kmalloc(sizeof(short), GFP_KERNEL) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int));\n");
					idx++;
				}
			}
		}
		for (int i = 0; i < policy.count; i++)
		{
			traverse_timetag(policy.sub.get(i), sb, memsetSb,indent);
		}
	}
	
	private static void traverse_vars(Formula policy, StringBuilder sb, HashSet<String> usedVars, ArrayList<String> objects) {
		for (int i = policy.varCount - 1; i >= 0; i--) {
			try {
				Integer.parseInt(policy.vars.get(i));
			}
			catch (NumberFormatException e) {
				if (!objects.contains(policy.vars.get(i)))
					if (!usedVars.contains(policy.vars.get(i))) {
						usedVars.add(policy.vars.get(i));
						sb.append(" " + policy.vars.get(i) + ",");
					}
			}
		}
		
		for (int i = 0; i < policy.count; i++) {
			traverse_vars(policy.sub.get(i), sb, usedVars, objects);
		}
	}
}

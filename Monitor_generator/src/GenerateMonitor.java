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

import java.util.HashMap;

//import java.util.*;

public class GenerateMonitor {

	/**
	 * @param args
	 */
	private static void generateMonitor(String source, String target)
	{
			Policy fList = FormulaParser.parse_formula(source);
			for (int i = 0; i < fList.formulaCount; i++)
			{
				if (i == 0) System.out.println("Main Formula : " + fList.formulas.get(i));
				else System.out.println(" - " + fList.target_recursive.get(i) + " := " + fList.formulas.get(i));
			}
			
			Monitoring.set_virtual_UID(fList);
			Monitoring.generate_kernel_monitor(fList, target);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("########################################################################");
		System.out.println("#        PolicyMonitoring  Copyright (C) 2012-2013  Hendra Gunadi      #");
		System.out.println("#             This program comes with ABSOLUTELY NO WARRANTY;          #");
  	System.out.println("#     This is free software, and you are welcome to redistribute it    #");
   	System.out.println("# under certain conditions; See COPYING and COPYING.LESSER for details #");
   	System.out.println("########################################################################");
   	System.out.println();

		if (args.length > 0)
		{
			String source = args[0];
			String target = "Monitor";
			
			generateMonitor(source, target);
		}
		else
		{
			System.out.println("Input xml file required");
		}
	}

}

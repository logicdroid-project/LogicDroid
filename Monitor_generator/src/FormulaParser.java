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

//import java.io.*;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.util.*;

public class FormulaParser {
	public static boolean formula_to_xml(ArrayList<Formula> formulas, String fileName)
	{
		//get an instance of factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		try 
		{
			//get an instance of builder
			DocumentBuilder db = dbf.newDocumentBuilder();
	
			//create an instance of DOM
			dom = db.newDocument();

		}
		catch(ParserConfigurationException pce) {
			//dump it
			System.out.println("Error while trying to instantiate DocumentBuilder " + pce);
			return false;
		}

		Element rootEle = dom.createElement("formulas");
		dom.appendChild(rootEle);
		
		for(int i = 0; i < formulas.size(); i++)
		{
			Element formulaEle = dom.createElement("formula");
			rootEle.appendChild(formulaEle);
			
			formulas.get(i).append_formula(dom, formulaEle);
		}
		
		DOMSource source = new DOMSource(dom);
		
		// PrintStream is the one responsible for writing text data to file
		try
		{
			PrintStream ps = new PrintStream(fileName);
			StreamResult target = new StreamResult(ps);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			
			transformer.transform(source, target);
		}
		catch (FileNotFoundException e)
		{
			System.out.println("File not found : " + e.getStackTrace());
			return false;
		}
		catch (TransformerConfigurationException tce)
		{
			tce.printStackTrace();
			return false;
		}
		catch (TransformerException te)
		{
			te.printStackTrace();
			return false;
		}
		
		
		return true;
	}
	
	public static Formula parse_string(String sf)
	{
		// assume well formed, there are closing parentheses
		Formula f = new Formula(sf, Formula.formula_type(sf.substring(1, sf.length()-1)), new MetaFormula());
		return f;
	}
	
	public static Policy parse_formula(String fileName)
	{
		// get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom;
		try
		{
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			// parse using builder to get DOM representation of the XML file
			dom = db.parse(fileName);
			
			return do_parse(dom);
		}
		catch (ParserConfigurationException pce)
		{
			pce.printStackTrace();
		}
		catch(SAXException se)
		{
			se.printStackTrace();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		return null;
	}
	
	private static Policy do_parse(Document dom)
	{
		Policy retval = new Policy();
		
		// Get the root element (formulas)
		Element root = dom.getDocumentElement();
		
		NodeList policyidNL = root.getElementsByTagName("policyid");
		Node policyidEle = policyidNL.item(0);
		retval.ID = Integer.parseInt(policyidEle.getTextContent());
		
		NodeList formulasNL = root.getElementsByTagName("formulas");
		Element formulasEle = (Element)formulasNL.item(0);
		// Get the node list (list of formulas)
		NodeList rootl = formulasEle.getElementsByTagName("formula");
		if (rootl != null)
		{
			int len = rootl.getLength();
			for (int i = 0; i < len; i++)
			{
				Element main_formula = (Element)rootl.item(i);
				String type = "";
				NodeList childs = main_formula.getChildNodes();
				// Get the first node of the real formula (only 1 root formula and 1 information assuming well formed)
				for (int j = 0; j < childs.getLength(); j++)
				{
					Node child = childs.item(j);
					if (child.getNodeType() == Node.ELEMENT_NODE)
					{
						if (child.getNodeName().equals(GlobalVariable.NODE_TYPE))
						{
							type = child.getTextContent().trim();
						}
						else
						{
							MetaFormula meta = new MetaFormula();
							String[] temp = type.split(":");
							if (temp[0].equals(GlobalVariable.MAIN))
							{
								String name = child.getNodeName();
								if (name.equals(GlobalVariable.EXIST))
								{
									retval.add_formula(new ExistFormula((Element)child, child.getNodeName(), meta), type, "");
								}
								else if (name.equals(GlobalVariable.FORALL))
								{
									retval.add_formula(new UniversalFormula((Element)child, child.getNodeName(), meta), type, "");
								}
								else if (name.equals(GlobalVariable.ATOM))
								{
									retval.add_formula(new Atom((Element)child, child.getNodeName(), meta), type, "");
								}
								else if (name.equals(GlobalVariable.DIAMONDDOT))
								{
									retval.add_formula(new DiamondDotFormula((Element)child, child.getNodeName(), meta), type, "");
								}
								else if (name.equals(GlobalVariable.DIAMOND))
								{
									retval.add_formula(new DiamondFormula((Element)child, child.getNodeName(), meta), type, "");
								}
								else if (name.equals(GlobalVariable.SINCE))
								{
									retval.add_formula(new SinceFormula((Element)child, child.getNodeName(), meta), type, "");
								}
								else if (name.equals(GlobalVariable.NOT))
								{
									retval.add_formula(new Negation((Element)child, child.getNodeName(), meta), type, "");
								}
								else
								{
									retval.add_formula(new Formula((Element)child, child.getNodeName(), meta), type, "");
								}
							}
							else if (temp[0].equals(GlobalVariable.RECURSIVE))
							{
								retval.add_formula(new Formula((Element)child, child.getNodeName(), meta), temp[0], temp[1]);
							}
	
							retval.add_metric(meta.metric);
							for (int k = 0; k < meta.relCount; k++)
							{
								retval.add_relation(meta.rels.get(k), meta.relsCard.get(k), meta.rel_type.get(k));
							}
							for (int k = 0; k < meta.objectCount; k++)
							{
								retval.add_object(meta.objects.get(k));
							}
						}
					}
				}
				
			}
		}
		
		return retval;
	}
}

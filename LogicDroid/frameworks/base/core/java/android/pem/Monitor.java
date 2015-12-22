package android.pem;

import android.os.RemoteException;
import android.util.Log;
import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.content.pm.ParceledListSlice;

import java.util.*;
import java.io.*;
import java.security.LogicDroid;

public class Monitor{
  static class MonitorInfo
  {
    public int UID;
    public String name;

    public MonitorInfo(int UID, String name)
    {
      this.UID = UID;
      this.name = name;
    }

    public void appendName(String name)
    {
      this.name = this.name + ", " + name;
    }
  }

  public static final int ROOT_UID = 0;
  public static final String ROOT_APP = "ROOT";
  public static final int INTERNET_UID = 1100;
  public static final String INTERNET_APP = "INTERNET";
  public static final int SMS_UID = 1101;
  public static final String SMS_APP = "SMS";
  public static final int LOCATION_UID = 1102;
  public static final String LOCATION_APP = "LOCATION";
  public static final int CONTACT_UID = 1103;
  public static final String CONTACT_APP = "CONTACT";
  public static final int HANGUP_UID = 1104;
  public static final String HANGUP_APP = "HANGUP";
  public static final int CALLPRIVILEGED_UID = 1105;
  public static final String CALLPRIVILEGED_APP = "CALLPRIVILEGED";
  public static final int IMEI_UID = 1106;
  public static final String IMEI_APP = "IMEI";
  private static final int virtual_app_num = 8;

  private static final Monitor MonitorInstance = new Monitor();
  public static Monitor getInstance() {return MonitorInstance;}

  private boolean notInitialized = true;
  private HashMap<String, Integer> relMapping = null;
  private HashMap<Integer, Integer> mapping;
  private MonitorInfo[] componentInfo;
  private int app_num;
  private int Monitor_policyID = 0;

  public boolean isVirtualUID(int UID)
  {
    if (UID == ROOT_UID|| UID == INTERNET_UID|| UID == SMS_UID|| UID == LOCATION_UID|| UID == CONTACT_UID
        || UID == HANGUP_UID || UID == CALLPRIVILEGED_UID)
      return true;
    return false;
  }

  public String virtualAppName(int UID)
  {
    switch (UID)
    {
      case ROOT_UID : return ROOT_APP;
      case INTERNET_UID : return INTERNET_APP;
      case SMS_UID : return SMS_APP;
      case LOCATION_UID : return LOCATION_APP;
      case CONTACT_UID : return CONTACT_APP;
      case HANGUP_UID : return HANGUP_APP;
      case CALLPRIVILEGED_UID : return CALLPRIVILEGED_APP;
      case IMEI_UID : return IMEI_APP;
    }
    return null;
  }

  private void getRelationsFromKernel()
  {
    int j = 0;    
    while(true)
    {
      String rel = LogicDroid.getRelationName(j);
      if (rel.equals("OUTSIDE-BOUND")) break;
      relMapping.put(rel, j);
      Log.i("LogicDroid", "Added relation : " + rel + " to ID " + j);
      j++;
    }
  }

  public boolean initializeMonitor()
  {
    Log.i("track - Monitor", "Initializing Monitor...");
    ArrayList<ApplicationInfo> apps = null;
    try
    {
      ParceledListSlice parcels = AppGlobals.getPackageManager().getInstalledApplications(0, "", 0);
      apps = new ArrayList<ApplicationInfo>();
      parcels.populateList(apps, ApplicationInfo.CREATOR);
    }
    catch (RemoteException re)
    {
      Log.i("track - Monitor", "InitializeMonitor : failed to get app list");
      notInitialized = true;
      app_num = 0;
      return false;
    }
    catch (Exception ex)
    {
      Log.i("track - Monitor", "InitializeMonitor : " + ex);
      notInitialized = true;
      app_num = 0;
      return false;
    }
    //synchronized(this)
    //{
      Log.i("track - Monitor", "InitializeMonitor : " + apps.size());
      mapping = new HashMap<Integer, Integer>();
      // Doesn't matter if the list application is not sorted
      int appIdx = 0;
      ArrayList<MonitorInfo> tempArr = new ArrayList<MonitorInfo>();
      tempArr.add(new MonitorInfo(ROOT_UID, ROOT_APP));
      tempArr.add(new MonitorInfo(INTERNET_UID, INTERNET_APP));
      tempArr.add(new MonitorInfo(SMS_UID, SMS_APP));
      tempArr.add(new MonitorInfo(LOCATION_UID, LOCATION_APP));
      tempArr.add(new MonitorInfo(CONTACT_UID, CONTACT_APP));
      tempArr.add(new MonitorInfo(HANGUP_UID, HANGUP_APP));
      tempArr.add(new MonitorInfo(CALLPRIVILEGED_UID, CALLPRIVILEGED_APP));
      tempArr.add(new MonitorInfo(IMEI_UID, IMEI_APP));
      mapping.put(ROOT_UID, 0);
      mapping.put(INTERNET_UID, 1);
      mapping.put(SMS_UID, 2);
      mapping.put(LOCATION_UID, 3);
      mapping.put(CONTACT_UID, 4);
      mapping.put(HANGUP_UID, 5);
      mapping.put(CALLPRIVILEGED_UID, 6);
      mapping.put(IMEI_UID, 7);
      appIdx = virtual_app_num;
      for (ApplicationInfo ai : apps)
      {
        if (!mapping.containsKey(ai.uid))
        {
          mapping.put(ai.uid, appIdx);
          tempArr.add(new MonitorInfo(ai.uid, ai.processName));
          appIdx++;
        }
        else
        {
          tempArr.get(mapping.get(ai.uid)).appendName(ai.processName);
        }
      }
      app_num = tempArr.size();
      componentInfo = tempArr.toArray(new MonitorInfo[app_num]);

      appIdx = 0;
      // Additional array to communicate with kernel
      int UID[] = new int[app_num - virtual_app_num];
      for (int i = virtual_app_num; i < app_num; i++)
      {
        UID[i - virtual_app_num] = tempArr.get(i).UID;
      }
      Monitor_policyID = LogicDroid.initializeMonitor(UID);

      if (relMapping == null) relMapping = new HashMap<String, Integer>();
      if (LogicDroid.isMonitorPresent()) getRelationsFromKernel();

      for (int i = 0; i < app_num; i++)
      {
        MonitorInfo mi = tempArr.get(i);
        Log.i("track - Monitor", "   - item(" + appIdx + ") : " + mi.name + " (" + mi.UID + ")");
        appIdx++;
      }

      notInitialized = false;
    //}
    return true;
  }

  public boolean checkEvent(Event ev, long timestamp)
  {
    if (notInitialized)
    {
      if (!initializeMonitor()) return false; // If the monitor is failed to initialize just let the event pass through
    }
    if (ev.varCount != 2 || !ev.rel.equalsIgnoreCase("call"))
    {
      return false; // only handles call event
    }

    boolean result = false;
    
    int syscall_result = LogicDroid.checkEvent(Monitor_policyID, ev.vars.get(0), ev.vars.get(1));

    if (syscall_result == -1)
    {
      Log.i("LogicDroid", ev.vars.get(0) + "-" + ev.vars.get(1) + "-POLICY_MISMATCH");
      initializeMonitor();
      syscall_result = LogicDroid.checkEvent(Monitor_policyID, ev.vars.get(0), ev.vars.get(1));
    }
    result = (syscall_result == 1);
    Log.i("LogicDroid", ev.vars.get(0) + "-" + ev.vars.get(1) + "-" + ((result) ? 1 : 0));

    return result;
  }

  public void renewMonitorVariable(String rel, boolean value, int ... UID)
  {
    if (notInitialized)
    {
      if (!initializeMonitor()) return; // If the monitor is failed to initialize just ignore
    }

    if(!relMapping.containsKey(rel))
    {
      if (LogicDroid.isMonitorPresent())
      {
        getRelationsFromKernel();
      }
      else
      {
        return;
      }
    }
    if(relMapping.containsKey(rel))
    {
      int idx = relMapping.get(rel);
      if (LogicDroid.modifyStaticVariable(Monitor_policyID, UID[0], value, relMapping.get(rel)) == -1) 
      {
        initializeMonitor();
        LogicDroid.modifyStaticVariable(Monitor_policyID, UID[0], value, relMapping.get(rel)); // this time it's almost guaranteed to succeed
      }
      String nm = componentInfo[mapping.get(UID[0])].name;
      if(nm == null) nm = "";
      Log.i("LogicDroid", rel + ":" + nm + " to " + ((value)?1:0));
    }
    else // if there is still no matching relations even after we fetch from the kernel relations, there is something wrong
    {
      Log.e("LogicDroid", "Relation " + rel + " is not in the list of relations");
    }
  }
}

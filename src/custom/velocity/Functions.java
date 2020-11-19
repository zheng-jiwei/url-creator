package custom.velocity;

import com.sun.jdi.LongValue;

import java.util.*;

public class Functions {
  //コンストラクタ
  public Functions(){
  }

  public <T> Boolean contains(List<T> inList, T tSearch){
    return (Boolean)inList.contains(tSearch);
  }

  public List<String> map(List<String> inList, HashMap<String, String> inMap)  {

    List<String> result = new ArrayList();
    for (String strKey : inList){
      if(inMap.get(strKey) != null)
        result.add(inMap.get(strKey));
      else
        result.add(strKey);
    }

    return result;
  }

  public <T> T first(List<T> inList){
    return (T)inList.get(0);
  }

  public Map find(String strId, List<Map> inList, String fieldName){
    Map<String, Object> result = new HashMap();
    for(Map item : inList){
      if(item.get(fieldName) == strId){
        result = item;
      }
    }
    return result;
  }

  public List<String> split(String inString, String seperator){
    return Arrays.asList(inString.split(seperator));
  }
}

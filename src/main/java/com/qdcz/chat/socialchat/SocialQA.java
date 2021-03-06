package com.qdcz.chat.socialchat;

import com.qdcz.chat.entity.RequestParameter;
import com.qdcz.chat.interfaces.ChatQA;
import com.qdcz.chat.tools.MyComparetor;
import com.qdcz.common.CommonTool;
import com.qdcz.entity.Vertex;
import com.qdcz.graph.interfaces.IGraphBuzi;
import com.qdcz.graph.tools.ResultBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.v1.types.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by star on 17-8-11.
 */
@Service
public class SocialQA extends ChatQA {
    @Autowired
    @Qualifier("neo4jCypherService")
    private IGraphBuzi graphBuzi;

    @Override
    public Set<Path> MatchPath(List<Map<String, Object>> maps, RequestParameter requestParameter) {
        Set<Path> paths = null;
        MyComparetor mc = new MyComparetor("questSimilar");
        Collections.sort(maps, mc);
        Collections.reverse(maps);
        String str = null;
        //降序后，第一个node就是分数最大的点
        Map<String, Object> maxNode = null;
        for (Map<String, Object> node : maps) {
            if ("node".equals(node.get("typeOf"))) {
                maxNode = node;
                break;
            }
        }
        if (maxNode != null) {
            Vertex vertex=new Vertex();
            CommonTool.transMap2Bean(maxNode,vertex);
            try {
                List<Path> paths1 = graphBuzi.bfExtersion(vertex, 2);

                Set<Path> set=new HashSet<Path>();
                set.addAll(paths1);//给set填充
                paths1.clear();//清空list，不然下次把set元素加入此list的时候是在原来的基础上追加元素的
                paths1.addAll(set);//把set的
                paths=set;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return  paths;
    }

    @Override
    public StringBuffer parsePathToResult(Set<Path> paths, List<Map<String, Object>> maps, RequestParameter requestParameter) {
        if(paths==null){
            return new StringBuffer();
        }
        StringBuffer sb=new StringBuffer();
        ResultBuilder resultBuilder = new ResultBuilder();
        List list = new ArrayList(paths);
        JSONObject object = resultBuilder.graphResult( list);
        JSONArray nodes = object.getJSONArray("nodes");
        JSONArray edgesJarry = object.getJSONArray("edges");
        Map<String , JSONObject> nodesMaps=new HashMap<>();
        for(int i=0;i<nodes.length();i++){

            JSONObject jsonObject = nodes.getJSONObject(i);
            String id =jsonObject.getString("id");
            nodesMaps.put(id,jsonObject);
        }
        Map<String,Vector<String>> edgeMapping=new HashMap<>();
        Vector<String> value=new Vector<String>();
        value.add("同事");value.add("伙伴");value.add("哪些");
        edgeMapping.put("属于",value);
        value=new Vector<String>();
        value.add("合作");value.add("协作"); value.add("作者");value.add("发表");
        edgeMapping.put("发表",value);

        MyComparetor mc = new MyComparetor("questSimilar");
        Collections.sort(maps,mc);
        Collections.reverse(maps);
        String str = null;
        Map<String, Object> maxNode=null;
        for(Map<String, Object> node:maps){
            if("node".equals(node.get("typeOf"))){
                maxNode=node;
                break;
            }
        }
        if(maxNode!=null) {
            Map<String,Map<String,Set<String>>> reSultMaps=new HashMap<>();
            Set<Map.Entry<String, Vector<String>>> entries = edgeMapping.entrySet();
            for (Map.Entry<String, Vector<String>> entry : entries) {
                Vector<String> value1 = entry.getValue();
                for (String s : value1) {
                    if(requestParameter.question.contains(s)){
                        Map<String,Set<String>> hashmap=new HashMap<>();
                        for(int i=0;i<edgesJarry.length();i++){
                            JSONObject jsonObject = edgesJarry.getJSONObject(i);
                            String fromId = jsonObject.getString("from");
                            String toId = jsonObject.getString("to");
                            String relation= jsonObject.getString("name");
                            if(entry.getKey().equals(relation)){
                                JSONObject object1 = nodesMaps.get(fromId);
                                JSONObject object2 = nodesMaps.get(toId);
                                String name = object1.getString("name");
                                String name2=object2.getString("name");
                                if(hashmap.containsKey(name2)){
                                    Set<String> strings = hashmap.get(name2);
                                    strings.add(name);
                                }else{
                                    Set<String> setStrs=new HashSet<>();
                                    setStrs.add(name);
                                    hashmap.put(name2,setStrs);
                                }
                            }
                        }
                        reSultMaps.put(entry.getKey(),hashmap);

                    }
                }
            }
            System.out.println();
            for (String s : reSultMaps.keySet()) {
                switch (s){
                    case "属于":
                        if("author".equals(maxNode.get("type"))){
                            Map<String, Set<String>> stringSetMap = reSultMaps.get(s);
                            String r1="";
                            for (Map.Entry<String, Set<String>> entry  : stringSetMap.entrySet()) {
                                String key = entry.getKey();
                                Set<String> values = entry.getValue();
                                r1 += "在"+key+"的同事有";//为
                                for(String value2:values){
                                    if(value2.equals(maxNode.get("name"))){
                                        continue;
                                    }
                                    r1+=value2+"、";
                                }
                                r1= r1.substring(0, r1.length() - 1) + "。";
                            }
                            sb.append(r1);
                        }else if("com".equals(maxNode.get("type"))){
                            Map<String, Set<String>> stringSetMap = reSultMaps.get(s);
                            String r0="";
                            for (Map.Entry<String, Set<String>> entry  : stringSetMap.entrySet()) {
                                String key = entry.getKey();
                                Set<String> values = entry.getValue();
                                if(key.equals(maxNode.get("name"))){
                                    r0 += "在"+key+"的人员有";//为
                                    for(String value2:values){
                                        r0+=value2+"、";
                                    }
                                    r0= r0.substring(0, r0.length() - 1) + "。";
                                }
                            }
                            sb.append(r0);
                        }

                        break;
                    case "发表":
                        if("author".equals(maxNode.get("type"))){
                            Map<String, Set<String>> stringSetMap = reSultMaps.get(s);
                            String r2="";
                            for (Map.Entry<String, Set<String>> entry : stringSetMap.entrySet()) {
                                String key = entry.getKey();
                                Set<String> values = entry.getValue();
                                if(requestParameter.question.contains(s)){
                                    for(String value2:values){
                                        if(value2.equals(maxNode.get("name"))){
                                            r2+=value2+"发表过的《"+key+"》、";
                                            break;
                                        }
                                    }
                                }
                                else{
                                    r2 += "在《"+key+"》中合作过的成员有";//为
                                    for(String value2:values){
                                        if(value2.equals(maxNode.get("name"))){
                                            continue;
                                        }
                                        r2+=value2+"、";
                                    }
                                }
                                r2= r2.substring(0, r2.length() - 1) + "。";
                            }
                            sb.append(r2);
                        }else if("paper".equals(maxNode.get("type"))){
                            Map<String, Set<String>> stringSetMap = reSultMaps.get(s);
                            String r3="";
                            for (Map.Entry<String, Set<String>> entry : stringSetMap.entrySet()) {
                                String key = entry.getKey();
                                if(key.equals(maxNode.get("name").toString())){
                                    Set<String> values = entry.getValue();
                                    r3 += "在《"+key+"》中作者有";//为
                                    for(String value2:values){
                                        r3+=value2+"、";
                                    }
                                    r3= r3.substring(0, r3.length() - 1) + "。";
                                }
                            }
                            sb.append(r3);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return sb;
    }
}

import java.util.*;

public class Test {
    private Map<Integer,String> nodes=new TreeMap<Integer, String>(new Comparator<Integer>() {
        public int compare(Integer key1, Integer key2) {
            return key2.compareTo(key1);
        }
    });
    //SortedMap<Integer,String> sortedMapByKey = new TreeMap<Integer,String>();

    public static void main(String[] args){

        Test test=new Test();
        test.init();
        test.printMap();
    }

    private void init(){
        Map<String,String> map = new TreeMap<String,String>(new Comparator<String>() {
            public int compare(String key1, String key2) {
             return key2.compareTo(key1);
            }
        });
        String[] initArray={"Boss","hello","Fisher","Yeser"};
        for(int i=0;i<initArray.length;i++){
            nodes.put(initArray.length-1-i,initArray[initArray.length-1-i]);
        }
        /*List<Map.Entry<Integer,String>> list=new ArrayList<Map.Entry<Integer, String>>(nodes.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<Integer, String>>(){//按key值字符串比较从小到大
            public int compare(Map.Entry<Integer, String> o1,Map.Entry<Integer, String> o2) {
                return o2.getKey()-(o1.getKey());
            }});
        nodes.clear();
        for(int i=0;i<list.size();i++){
            nodes.put(list.get(i).getKey(),list.get(i).getValue());
        }*/
    }

    private void printMap(){
        for(Map.Entry<Integer,String> entry:nodes.entrySet()){
            System.out.println(entry.getKey()+" "+entry.getValue());
        }
    }

}

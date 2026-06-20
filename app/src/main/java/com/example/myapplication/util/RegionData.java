package com.example.myapplication.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中国行政区划数据（省份 → 城市二级联动）
 * 数据精简版，涵盖主要城市
 */
public class RegionData {

    private static final Map<String, String[]> PROVINCE_CITY_MAP = new LinkedHashMap<>();

    static {
        PROVINCE_CITY_MAP.put("北京市",    new String[]{"北京"});
        PROVINCE_CITY_MAP.put("天津市",    new String[]{"天津"});
        PROVINCE_CITY_MAP.put("上海市",    new String[]{"上海"});
        PROVINCE_CITY_MAP.put("重庆市",    new String[]{"重庆"});
        PROVINCE_CITY_MAP.put("河北省",    new String[]{"石家庄", "唐山", "秦皇岛", "邯郸", "邢台", "保定", "张家口", "承德", "沧州", "廊坊", "衡水"});
        PROVINCE_CITY_MAP.put("山西省",    new String[]{"太原", "大同", "阳泉", "长治", "晋城", "朔州", "晋中", "运城", "忻州", "临汾", "吕梁"});
        PROVINCE_CITY_MAP.put("内蒙古",    new String[]{"呼和浩特", "包头", "乌海", "赤峰", "通辽", "鄂尔多斯", "呼伦贝尔", "巴彦淖尔", "乌兰察布"});
        PROVINCE_CITY_MAP.put("辽宁省",    new String[]{"沈阳", "大连", "鞍山", "抚顺", "本溪", "丹东", "锦州", "营口", "阜新", "辽阳", "盘锦", "铁岭", "朝阳", "葫芦岛"});
        PROVINCE_CITY_MAP.put("吉林省",    new String[]{"长春", "吉林", "四平", "辽源", "通化", "白山", "松原", "白城", "延边"});
        PROVINCE_CITY_MAP.put("黑龙江省",  new String[]{"哈尔滨", "齐齐哈尔", "鸡西", "鹤岗", "双鸭山", "大庆", "伊春", "佳木斯", "七台河", "牡丹江", "黑河", "绥化"});
        PROVINCE_CITY_MAP.put("江苏省",    new String[]{"南京", "无锡", "徐州", "常州", "苏州", "南通", "连云港", "淮安", "盐城", "扬州", "镇江", "泰州", "宿迁"});
        PROVINCE_CITY_MAP.put("浙江省",    new String[]{"杭州", "宁波", "温州", "嘉兴", "湖州", "绍兴", "金华", "衢州", "舟山", "台州", "丽水"});
        PROVINCE_CITY_MAP.put("安徽省",    new String[]{"合肥", "芜湖", "蚌埠", "淮南", "马鞍山", "淮北", "铜陵", "安庆", "黄山", "滁州", "阜阳", "宿州", "六安", "亳州", "池州", "宣城"});
        PROVINCE_CITY_MAP.put("福建省",    new String[]{"福州", "厦门", "莆田", "三明", "泉州", "漳州", "南平", "龙岩", "宁德"});
        PROVINCE_CITY_MAP.put("江西省",    new String[]{"南昌", "景德镇", "萍乡", "九江", "新余", "鹰潭", "赣州", "吉安", "宜春", "抚州", "上饶"});
        PROVINCE_CITY_MAP.put("山东省",    new String[]{"济南", "青岛", "淄博", "枣庄", "东营", "烟台", "潍坊", "济宁", "泰安", "威海", "日照", "临沂", "德州", "聊城", "滨州", "菏泽"});
        PROVINCE_CITY_MAP.put("河南省",    new String[]{"郑州", "开封", "洛阳", "平顶山", "安阳", "鹤壁", "新乡", "焦作", "濮阳", "许昌", "漯河", "三门峡", "南阳", "商丘", "信阳", "周口", "驻马店"});
        PROVINCE_CITY_MAP.put("湖北省",    new String[]{"武汉", "黄石", "十堰", "宜昌", "襄阳", "鄂州", "荆门", "孝感", "荆州", "黄冈", "咸宁", "随州", "恩施"});
        PROVINCE_CITY_MAP.put("湖南省",    new String[]{"长沙", "株洲", "湘潭", "衡阳", "邵阳", "岳阳", "常德", "张家界", "益阳", "郴州", "永州", "怀化", "娄底", "湘西"});
        PROVINCE_CITY_MAP.put("广东省",    new String[]{"广州", "韶关", "深圳", "珠海", "汕头", "佛山", "江门", "湛江", "茂名", "肇庆", "惠州", "梅州", "汕尾", "河源", "阳江", "清远", "东莞", "中山", "潮州", "揭阳", "云浮"});
        PROVINCE_CITY_MAP.put("广西",      new String[]{"南宁", "柳州", "桂林", "梧州", "北海", "防城港", "钦州", "贵港", "玉林", "百色", "贺州", "河池", "来宾", "崇左"});
        PROVINCE_CITY_MAP.put("海南省",    new String[]{"海口", "三亚", "三沙", "儋州"});
        PROVINCE_CITY_MAP.put("四川省",    new String[]{"成都", "自贡", "攀枝花", "泸州", "德阳", "绵阳", "广元", "遂宁", "内江", "乐山", "南充", "眉山", "宜宾", "广安", "达州", "雅安", "巴中", "资阳", "凉山"});
        PROVINCE_CITY_MAP.put("贵州省",    new String[]{"贵阳", "六盘水", "遵义", "安顺", "毕节", "铜仁", "黔西南", "黔东南", "黔南"});
        PROVINCE_CITY_MAP.put("云南省",    new String[]{"昆明", "曲靖", "玉溪", "保山", "昭通", "丽江", "普洱", "临沧", "楚雄", "红河", "文山", "西双版纳", "大理", "德宏", "怒江", "迪庆"});
        PROVINCE_CITY_MAP.put("西藏",      new String[]{"拉萨", "日喀则", "昌都", "林芝", "山南", "那曲", "阿里"});
        PROVINCE_CITY_MAP.put("陕西省",    new String[]{"西安", "铜川", "宝鸡", "咸阳", "渭南", "延安", "汉中", "榆林", "安康", "商洛"});
        PROVINCE_CITY_MAP.put("甘肃省",    new String[]{"兰州", "嘉峪关", "金昌", "白银", "天水", "武威", "张掖", "平凉", "酒泉", "庆阳", "定西", "陇南", "临夏", "甘南"});
        PROVINCE_CITY_MAP.put("青海省",    new String[]{"西宁", "海东", "海北", "黄南", "海南", "果洛", "玉树", "海西"});
        PROVINCE_CITY_MAP.put("宁夏",      new String[]{"银川", "石嘴山", "吴忠", "固原", "中卫"});
        PROVINCE_CITY_MAP.put("新疆",      new String[]{"乌鲁木齐", "克拉玛依", "吐鲁番", "哈密", "昌吉", "博尔塔拉", "巴音郭楞", "阿克苏", "克孜勒苏", "喀什", "和田", "伊犁", "塔城", "阿勒泰"});
        PROVINCE_CITY_MAP.put("台湾省",    new String[]{"台北", "高雄", "台中", "台南", "基隆", "新竹", "嘉义"});
        PROVINCE_CITY_MAP.put("香港",      new String[]{"香港"});
        PROVINCE_CITY_MAP.put("澳门",      new String[]{"澳门"});
    }

    /** 获取所有省份名称列表 */
    public static List<String> getProvinces() {
        return new ArrayList<>(PROVINCE_CITY_MAP.keySet());
    }

    /** 根据省份获取城市列表 */
    public static String[] getCities(String province) {
        return PROVINCE_CITY_MAP.get(province);
    }

    /** 获取省份索引 */
    public static int getProvinceIndex(String cityName) {
        if (cityName == null || cityName.isEmpty()) return 0;
        List<String> provinces = getProvinces();
        for (int i = 0; i < provinces.size(); i++) {
            String[] cities = PROVINCE_CITY_MAP.get(provinces.get(i));
            if (cities != null) {
                for (String c : cities) {
                    if (c.equals(cityName)) return i;
                }
            }
        }
        // 如果只存了城市名，尝试模糊匹配
        for (int i = 0; i < provinces.size(); i++) {
            String province = provinces.get(i);
            if (province.contains(cityName) || cityName.contains(province.replace("省", "").replace("市", ""))) {
                return i;
            }
        }
        return 0;
    }

    /** 根据城市名反查省份名 */
    public static String getProvinceForCity(String cityName) {
        if (cityName == null || cityName.isEmpty()) return "北京市";
        for (Map.Entry<String, String[]> entry : PROVINCE_CITY_MAP.entrySet()) {
            for (String c : entry.getValue()) {
                if (c.equals(cityName)) return entry.getKey();
            }
        }
        return "北京市";
    }

    /** 获取省份下的城市索引 */
    public static int getCityIndex(String province, String cityName) {
        String[] cities = PROVINCE_CITY_MAP.get(province);
        if (cities == null) return 0;
        for (int i = 0; i < cities.length; i++) {
            if (cities[i].equals(cityName)) return i;
        }
        return 0;
    }
}

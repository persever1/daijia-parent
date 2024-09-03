package com.atguigu.daijia.common.util;

public class LocationUtil {

    // 地球赤道半径
    private static double EARTH_RADIUS = 6378.137;

        // 将角度转换为弧度
    // 此方法等同于 Math.toRadians()
    private static double rad(double d) {
        // 使用 Math.PI 作为 PI 的近似值，进行角度到弧度的转换
        return d * Math.PI / 180.0;
    }


    /**
     * @描述 经纬度获取距离，单位为米
     * @参数 [lat1, lng1, lat2, lng2]
     * @返回值 double
     **/
    public static double getDistance(double lat1, double lng1, double lat2,
                                     double lng2) {
        // 将纬度转换为弧度
        double radLat1 = rad(lat1);
        // 将纬度转换为弧度
        double radLat2 = rad(lat2);
        // 计算纬度之差
        double a = radLat1 - radLat2;
        // 计算经度之差
        double b = rad(lng1) - rad(lng2);
        // 根据经纬度差和地球半径计算两点之间的距离
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        // 将弧度值转换为实际距离
        s = s * EARTH_RADIUS;
        // 保留四位小数
        s = Math.round(s * 10000d) / 10000d;
        // 转换为米
        s = s * 1000;
        return s;
    }


    /**
     * 主函数入口
     * 计算并打印出两个地理坐标点之间的距离
     *
     * @param args 命令行参数，本程序未使用
     */
    public static void main(String[] args) {
        // 调用getDistance方法，计算两个地理坐标点之间的距离
        // 第一对参数：纬度和经度表示第一个地点
        // 第二对参数：纬度和经度表示第二个地点
        double distance = getDistance(30.57404, 104.073013,
                30.509376, 104.077001);
        // 输出计算得到的距离
        System.out.println("距离" + distance + "米");
    }

}

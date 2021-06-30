package cn.bupt.driverhailingserver.controller;

import cn.bupt.driverhailingserver.entity.*;
import cn.bupt.driverhailingserver.repository.*;
import cn.bupt.driverhailingserver.server.WebSocketServer;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


//@RequestMapping("/user/driver")
@RestController
public class DriverController {
    final CommentRepository commentRepository;
    final OrderForUserRepository orderForUserRepository;
    final OrderForCustomerRepository orderForCustomerRepository;
    final OrderForDriverRepository orderForDriverRepository;
    final DriverRepository driverRepository;
    final RequestOrderRepository requestOrderRepository;
    //    final OrderRepository orderRepository;
    final CustomerRepository customerRepository;
    final AreaRepository areaRepository;


    @Autowired
    public DriverController(CommentRepository commentRepository, DriverRepository driverRepository, OrderForDriverRepository orderForDriverRepository, OrderForCustomerRepository orderForCustomerRepository
            , RequestOrderRepository requestOrderRepository, OrderForUserRepository orderForUserRepository, CustomerRepository customerRepository, AreaRepository areaRepository) {
        this.commentRepository = commentRepository;
        this.orderForUserRepository = orderForUserRepository;
        this.orderForCustomerRepository = orderForCustomerRepository;
        this.orderForDriverRepository = orderForDriverRepository;
        this.driverRepository = driverRepository;
        this.requestOrderRepository = requestOrderRepository;
        this.customerRepository = customerRepository;
        this.areaRepository = areaRepository;
    }

    @Value("${eureka.instance.hostname}")
    private String name;
    /*注入“服务提供者”的端口号*/
    @Value("${server.port}")
    private String port;

    /*提供的接口，用于返回信息*/
    @RequestMapping("/test-info")
    public String testInfo() {
        //返回数据
        return name + " port:" + port;
    }


    @RequestMapping("/updateDriver")
    public String updateDriver(String driverName) {
        Driver driver = driverRepository.findByDriverName(driverName);

        //暂定市中心人最多，司机们都住在那附近，因此在空闲状态司机将会前往市中心
        //城市大小50km * 50km, 每1km有一个检测点
        int curX = driver.getCurX();
        int curY = driver.getCurY();
        double difX = Math.abs(curX - driver.getDesX());
        double difY = Math.abs(curY - driver.getDesY());
        if (difX == 0) {
            if (difY == 0) {
                //已到达 更新各种信息 busy free 订单状况 消息推送 等

            } else// difx = 0 dify != 0 y方向走
            {
                curY += (driver.getDesY() - curY) / difY;
            }
        } else {
            if (difY == 0)// difx!=0 dify=0 往x方向走
            {
                curX += (driver.getDesX() - curX) / difX;
            } else // 往短的一方走
            {
                if (difX <= difY) {
                    curX += (driver.getDesX() - curX) / difX;
                } else {
                    curY += (driver.getDesY() - curY) / difY;
                }
            }
        }
        driver.setCurX(curX);
        driver.setCurY(curY);

        // 更新司机积分信息
        driver.setDriverPoint(driver.getFinishCount() * 10 + driver.getFinishDistance());
        driver.setDriverLevel(driver.getDriverPoint() % 50);
        // 更新司机订单信息
        for (int i = 0; i < driver.getRequestOrderList().size(); ++i) {
            RequestOrder requestOrder = driver.getRequestOrderList().get(i);
            if (requestOrder.getIfCheck() == 1 && !requestOrder.getDriverName().equals(driverName)) {
                driver.getRequestOrderList().remove(requestOrder);
            }
        }
        Area area = areaRepository.findByDriverId(driver.getId());
        if (driver.getIfBusy() == 1) {
            //        area.setDriverId(driverRepository.findByDriverName(driver.getDriverName()).getId());
            area.setSectorId((driver.getDesY() / 3) * 17 + driver.getDesX() / 3);

        } else {
            //        area.setDriverId(driverRepository.findByDriverName(driver.getDriverName()).getId());
            area.setSectorId((driver.getCurY() / 3) * 17 + driver.getCurX() / 3);
        }
        areaRepository.save(area);

        driverRepository.save(driver);
        return "司机信息更新成功";
    }


    @RequestMapping("/handleRequestOrder")
    public String handleRequestOrder(String driverName, int orderNum) {
        Driver driver = driverRepository.findByDriverName(driverName);
        if (driver.getRequestOrderList() != null)//如果有订单请求
        {
            List<RequestOrder> driverRequestOrders = driver.getRequestOrderList();
//            System.out.println("---------------------------");
//            System.out.println(driver.getRequestOrderList().get(orderNum).getCustomer().getCustomerName());
//            System.out.println("---------------------------");
            if (driverRequestOrders != null && driverRequestOrders.size() > orderNum) {
                //接受orderNum号订单
                RequestOrder requestOrder = driverRequestOrders.get(orderNum);
                if (requestOrder.getIfCheck() == 0 || requestOrder.getDriverName().equals(driverName)) //该订单没有被其他用户接走
                {
                    requestOrder.setIfCheck(1);
                    List<Driver> tempList = requestOrder.getDriver();
                    tempList.add(driver);
                    requestOrder.setDriver(tempList);
                    requestOrder.setDriverName(driverName);
                    requestOrderRepository.save(requestOrder);

                    driver.getRequestOrderList().clear();
                    List<RequestOrder> tempOrder = new ArrayList<RequestOrder>();
                    tempOrder.add(requestOrder);
                    driver.setRequestOrderList(tempOrder);
//                    driver.getRequestOrderList().add(requestOrder);
                    driver.setIfBusy(1);
                    driver.setDesX(requestOrder.getCurX());
                    driver.setDesY(requestOrder.getCurY());
                    driverRepository.save(driver);

                    //消息推送给用户 已有司机接单
                    // 司机界面变成前往 界面
                    WebSocketServer webSocketServer = new WebSocketServer();
                    String message = "司机：" + driverName + "前往上车地点";
                    try {
                        webSocketServer.sendInfo(message, requestOrder.getCustomer().getId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return "接受" + orderNum + "号订单";
                } else //已被其他司机接走
                {
                    driver.getRequestOrderList().remove(orderNum);
                    driverRepository.save(driver);
                    return "该订单已被接走，请选择其他订单";
                }
            } else {
                return "无该约车订单";
            }
        } else {
            return "无约车订单";
        }
    }

    @RequestMapping("/takeCustomer")
    public String takeCustomer(String driverName) {
        Driver driver = driverRepository.findByDriverName(driverName);
        // TODO：需要删除MANYTOMANY关系 不然获取不到正确用户信息
        RequestOrder requestOrder = driver.getRequestOrderList().get(0);

        Customer customer = customerRepository.findByCustomerName(requestOrder.getCustomerName());
//        Order order = new Order();TODO
//        OrderForDriver order= new OrderForDriver();
        OrderForUser order = new OrderForUser();

        driver.setDesX(requestOrder.getDesX());
        driver.setDesY(requestOrder.getDesY());
        order.setCustomer(customer);
        order.setDriver(driver);
//        order.setComment(new Comment("默认五星好评",5));
        order.setCustomerName(customer.getCustomerName());
        order.setDriverName(driverName);
        order.setStartTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        order.setEndTime("");
        order.setServiceLevel(driver.getServiceLevel());
        order.setCurX(driver.getCurX());
        order.setCurY(driver.getCurY());
        order.setDesX(requestOrder.getDesX());
        order.setDesY(requestOrder.getDesY());
        order.setDistance(Math.abs(order.getCurX() - order.getDesX()) + Math.abs(order.getCurY() - order.getDesY()));
        order.setPrice(order.getDistance() * 3 + 13);
        order.setCurState(0);
        Long id = orderForUserRepository.save(order).getId();
//        driver.getOrderForDriverList().add(order);
//        driver.setCurOrder(order);
        driver.setCurOrderId(id);
        driver.setCurCustomerName(customer.getCustomerName());
        //TODO:乘客已上车 请求订单生命结束 从数据库中删去
//        requestOrderRepository.findById(requestOrder.getId());

        driverRepository.save(driver);


        WebSocketServer webSocketServer = new WebSocketServer();
        String message = "司机：" + driverName + "请乘客系好安全带，前往目的地";
        try {
            webSocketServer.sendInfo(message, customer.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "请乘客系好安全带，正在前往目的地";
    }

    @RequestMapping("/finishOrder")
    public String finishOrder(String driverName) {
        Driver driver = driverRepository.findByDriverName(driverName);
//        OrderForUser order  = driver.getCurOrder();
//        String orderId = driver.getCurOrderId();
        OrderForUser order = orderForUserRepository.findById(driver.getCurOrderId()).orElse(null);//TODO
        // 更新order 信息
        if (order == null) {
            return "乘客已送达";
        }
        order.setEndTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));//TODO
//        order.setEndTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));

        //更新driver 信息
//        driver.setCurOrderId((long) -1);
//        driver.setIfBusy(0);
//        driver.setFinishCount(driver.getFinishCount() + 1);
        driver.setCurX(order.getDesX());
        driver.setCurY(order.getDesY());
        driver.setFinishDistance(driver.getFinishDistance() + order.getDistance());
        System.out.println("-------------");
        System.out.println(driver.getFinishDistance());
        System.out.println(order.getDistance());
        System.out.println("-------------");
        driver.setDesX(25);
        driver.setDesY(25);

        Area area = areaRepository.findByDriverId(driver.getId());
//        area.setDriverId(driverRepository.findByDriverName(driver.getDriverName()).getId());
        area.setSectorId((driver.getDesY() / 3) * 17 + driver.getDesX() / 3);
        areaRepository.save(area);
        driverRepository.save(driver);
//        orderRepository.save(order);TODO
        return "乘客已送达";
    }

}


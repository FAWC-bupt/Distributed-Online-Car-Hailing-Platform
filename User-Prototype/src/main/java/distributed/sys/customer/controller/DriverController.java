package distributed.sys.customer.controller;

import com.fasterxml.jackson.annotation.JsonView;
import distributed.sys.customer.entity.*;
import distributed.sys.customer.repository.*;
import distributed.sys.customer.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/user/driver")
public class DriverController {
    final CommentRepository commentRepository;
    final OrderForUserRepository orderForUserRepository;
    final DriverRepository driverRepository;
    final RequestOrderRepository requestOrderRepository;
    //    final OrderRepository orderRepository;
    final CustomerRepository customerRepository;
    final AreaRepository areaRepository;
final RequestOrderForDriverRepository requestOrderForDriverRepository;

    @Autowired
    public DriverController(CommentRepository commentRepository, DriverRepository driverRepository,RequestOrderForDriverRepository requestOrderForDriverRepository
            , RequestOrderRepository requestOrderRepository, OrderForUserRepository orderForUserRepository, CustomerRepository customerRepository, AreaRepository areaRepository) {
        this.commentRepository = commentRepository;
        this.orderForUserRepository = orderForUserRepository;
        this.driverRepository = driverRepository;
        this.requestOrderRepository = requestOrderRepository;
        this.customerRepository = customerRepository;
        this.areaRepository = areaRepository;
        this.requestOrderForDriverRepository= requestOrderForDriverRepository;
    }

    @RequestMapping("/login")
    public String login(HttpServletRequest request) {
        String driverName = request.getParameter("driverName");
        String password = request.getParameter("password");

        if (null == driverName || null == password) {
            return "????????????????????????";
        } else {
            if (driverRepository.findByDriverName(driverName) != null) {
                if (driverRepository.findByDriverName(driverName).getPassword().equals(password)) {
                    System.out.println("??????" + driverName + "?????????");
                    Driver driver = driverRepository.findByDriverName(driverName);
                    driver.setIfLogin(1);
                    driverRepository.save(driver);
                    return driverName+ "????????????";
                } else {
                    return "????????????";
                }
            } else {
                return "???????????????";
            }
        }
    }

    @RequestMapping("/logout")
    public String logout(String driverName) {// TODO???????????????
        Driver driver = driverRepository.findByDriverName(driverName);
        if(driver == null)
        {
            return "??????????????????";
        }
        driver.setIfLogin(0);
        System.out.println("??????" + driverName + "?????????");
        return driverName+ "????????????";
    }

    @RequestMapping("/register")
    public String register(String driverName, String password) {
        if (driverRepository.findByDriverName(driverName) == null) {
            Driver driver = new Driver();
            driver.setDriverName(driverName);
            driver.setPassword(password);
            driver.setFinishCount(0);
            driver.setFinishDistance(0);
//        driver.setServiceLevel(1);
            driver.setDriverLevel(1);
            driver.setStars(0);
            driver.setIfBusy(0);
            driver.setIfLogin(0);
            driver.setStars(0);
            driver.setIfBusy(0);
            driver.setCurX(25);
            driver.setCurY(25);
            driver.setDesX(driver.getCurX());
            driver.setDesY(driver.getCurY());

//            driver.setOrderList(new ArrayList<>());
//            driver.setCurOrder(new Order());
//            driver.setCommentList(new ArrayList<>());
//            driver.setRequestOrderList(new ArrayList<>());
            driver.setCurCustomerName("");
            driverRepository.save(driver);
            Area area = new Area();
            area.setDriverId(driverRepository.findByDriverName(driver.getDriverName()).getId());
            area.setSectorId((driver.getCurY() / 3) * 17 + driver.getCurX() / 3);
            areaRepository.save(area);
            return driverName+ "????????????";
        } else {
            System.out.println("????????????????????????");
            return "???????????????????????????????????????";
        }

    }

    @RequestMapping("/edit")
    public String edit(String driverName, int serviceLevel) {
        Driver driver = driverRepository.findByDriverName(driverName);
        if (driver == null) {
            return driverName+ "??????????????????";
        }
        driver.setServiceLevel(serviceLevel);
        driverRepository.save(driver);
        return "????????????";
    }

    @RequestMapping("/Index")//???????????? ??????????????????
    @JsonView(Views.Public.class)
    public Driver Index(String driverName) {
        return driverRepository.findByDriverName(driverName);
    }


    @RequestMapping("/updateDriver")
    public String updateDriver(String driverName) {
        System.out.println("updateDriver: " + driverName);
        Driver driver = driverRepository.findByDriverName(driverName);
        if(driver == null)
        {
            return "??????????????????????????????";
        }
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????
        //????????????50km * 50km, ???1km??????????????????
        int curX = driver.getCurX();
        int curY = driver.getCurY();
        double difX = Math.abs(curX - driver.getDesX());
        double difY = Math.abs(curY - driver.getDesY());
        if (difX == 0) {
            if (difY == 0) {
                //????????? ?????????????????? busy free ???????????? ???????????? ???
            } else// difx = 0 dify != 0 y?????????
            {
                curY += (driver.getDesY() - curY) / difY;
            }
        } else {
            if (difY == 0)// difx!=0 dify=0 ???x?????????
            {
                curX += (driver.getDesX() - curX) / difX;
            } else // ??????????????????
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

        // ????????????????????????
        driver.setDriverPoint(driver.getFinishCount() * 10 + driver.getFinishDistance());
        driver.setDriverLevel(driver.getDriverPoint() % 50);
        // ????????????????????????
        for (int i = 0; i < driver.getRequestOrderForDriverList().size(); ++i) {
            RequestOrderForDriver requestOrderForDriver = driver.getRequestOrderForDriverList().get(i);
            RequestOrder requestOrder = requestOrderRepository.findByCustomerName(requestOrderForDriver.getCustomerName());
            if (requestOrder.getIfCheck() == 1 && !requestOrder.getDriverName().equals(driverName)) {
                driver.getRequestOrderForDriverList().remove(requestOrderForDriver);
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
        return driverName+ "????????????????????????";
    }


    @RequestMapping("/handleRequestOrder")
    public String handleRequestOrder(String driverName, int orderNum) {
        Driver driver = driverRepository.findByDriverName(driverName);
        if(driver == null)
        {
            return "???????????????????????????????????????????????????";
        }
        if (driver.getRequestOrderForDriverList() != null)//?????????????????????
        {
            List<RequestOrderForDriver> driverRequestOrderForDriverList = driver.getRequestOrderForDriverList();
//            System.out.println("---------------------------");
//            System.out.println(driver.getRequestOrderList().get(orderNum).getCustomer().getCustomerName());
//            System.out.println("---------------------------");
            if (driverRequestOrderForDriverList != null && driverRequestOrderForDriverList.size() > orderNum) {
                //??????orderNum?????????
                RequestOrderForDriver requestOrderForDriver = driverRequestOrderForDriverList.get(orderNum);
                RequestOrder requestOrder = requestOrderRepository.findByCustomerName(requestOrderForDriver.getCustomerName());
                if (requestOrder.getIfCheck() == 0 || requestOrder.getDriverName().equals(driverName)) //????????????????????????????????????
                {
                    requestOrder.setIfCheck(1);
                    requestOrderRepository.save(requestOrder);
                    requestOrder.setDriverName(driverName);
                    requestOrderRepository.save(requestOrder);

                    driver.getRequestOrderForDriverList().clear();
                    List<RequestOrderForDriver> tempOrder = new ArrayList<RequestOrderForDriver>();
                    tempOrder.add(requestOrderForDriver);
                    driver.setRequestOrderForDriverList(tempOrder);
//                    driver.getRequestOrderList().add(requestOrder);
                    driver.setIfBusy(1);
                    driver.setDesX(requestOrder.getCurX());
                    driver.setDesY(requestOrder.getCurY());
                    requestOrderForDriverRepository.save(requestOrderForDriver);
                    driverRepository.save(driver);

                    //????????????????????? ??????????????????
                    // ???????????????????????? ??????
                    WebSocketServer webSocketServer = new WebSocketServer();
                    String message = "?????????" + driverName + "??????????????????";
                    try {
                        webSocketServer.sendInfo(message, requestOrder.getCustomer().getId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return driverName+ "??????" + orderNum + "?????????";
                } else //????????????????????????
                {
                    driver.getRequestOrderForDriverList().remove(orderNum);
                    driverRepository.save(driver);
                    return "?????????????????????????????????????????????";
                }
            } else {
                return "??????????????????";
            }
        } else {
            return "???????????????";
        }
    }

    @RequestMapping("/takeCustomer")
    public String takeCustomer(String driverName) {
        Driver driver = driverRepository.findByDriverName(driverName);
        if(driver == null)
        {
            return "????????????";
        }
        // TODO???????????????MANYTOMANY?????? ????????????????????????????????????
//        RequestOrder requestOrder = driver.getRequestOrderList().get(0);
        List<RequestOrderForDriver> requestOrderForDriverList = driver.getRequestOrderForDriverList();
        if(requestOrderForDriverList == null)
        {
            return "???????????????";
        }
        RequestOrderForDriver requestOrderForDriver = requestOrderForDriverList.get(0);
        if(requestOrderForDriver == null)
        {
            return "????????????????????????";
        }
        Customer customer = customerRepository.findByCustomerName(requestOrderForDriver.getCustomerName());
        if(customer == null)
        {
            return "?????????????????????????????????";
        }
//        Order order = new Order();TODO
//        OrderForDriver order= new OrderForDriver();
        OrderForUser order = new OrderForUser();

        driver.setDesX(requestOrderForDriver.getDesX());
        driver.setDesY(requestOrderForDriver.getDesY());
        order.setCustomer(customer);
        order.setDriver(driver);
//        order.setComment(new Comment("??????????????????",5));
        order.setCustomerName(customer.getCustomerName());
        order.setDriverName(driverName);
        order.setStartTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        order.setEndTime("");
        order.setServiceLevel(driver.getServiceLevel());
        order.setCurX(customer.getCurX());
        order.setCurY(customer.getCurY());
        order.setDesX(requestOrderForDriver.getDesX());
        order.setDesY(requestOrderForDriver.getDesY());
        order.setDistance(Math.abs(order.getCurX() - order.getDesX()) + Math.abs(order.getCurY() - order.getDesY()));
        order.setPrice(order.getDistance() * 3 + 13);
        order.setCurState(0);
        Long id = orderForUserRepository.save(order).getId();
//        driver.getOrderForDriverList().add(order);
//        driver.setCurOrder(order);
        driver.setCurOrderId(id);
        driver.setCurCustomerName(customer.getCustomerName());
        driver.getRequestOrderForDriverList().clear();
        //TODO:??????????????? ???????????????????????? ?????????????????????
//        requestOrderRepository.findById(requestOrder.getId());
        requestOrderForDriverRepository.save(requestOrderForDriver);
        orderForUserRepository.save(order);
        driverRepository.save(driver);


        WebSocketServer webSocketServer = new WebSocketServer();
        String message = "?????????" + driverName + "??????????????????????????????????????????";
        try {
            webSocketServer.sendInfo(message, customer.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return customer.getCustomerName() + "????????????????????????????????????????????????";
    }

    @RequestMapping("/finishOrder")
    public String finishOrder(String driverName) {
        Driver driver = driverRepository.findByDriverName(driverName);
//        OrderForUser order  = driver.getCurOrder();
//        String orderId = driver.getCurOrderId();
        OrderForUser order = orderForUserRepository.findById(driver.getCurOrderId()).orElse(null);//TODO
        // ??????order ??????
        if (order == null) {
            return "???????????????";
        }
        order.setEndTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));//TODO
//        order.setEndTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));

        //??????driver ??????
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
        return "???????????????";
    }

    @GetMapping("/searchOrder")
    public String searchOrder(String driverName) {
//        Customer customer =customerRepository.findByCustomerName(customerName);
        Driver driver = driverRepository.findByDriverName(driverName);
        List<OrderForUser> orderList = orderForUserRepository.findByDriverName(driverName);
        if (orderList == null) {
            return "?????????";
        }
        StringBuilder retStr = new StringBuilder();
        for (OrderForUser orderForUser : orderList) {
            retStr.append("Order0:").append(orderForUser.toString()).append("\n");
            System.out.println("Order0:" + orderForUser.toString());
        }
        return retStr.toString();
    }
}


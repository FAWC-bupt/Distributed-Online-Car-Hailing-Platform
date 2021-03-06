package cn.bupt.userserver.repository;

import cn.bupt.userserver.entity.OrderForCustomer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderForCustomerRepository extends CrudRepository<OrderForCustomer, Long> {
    Optional<OrderForCustomer> findById(Long id);
//    List<OrderForCustomer> findByStartTime(String st);
//    List<OrderForCustomer> findByEndTime(String ed);
//    //    List<OrderRepository> findByUserEmail(String userEmail);
//    List<OrderForCustomer> findByDriverEmail(String driverEmail);
//    //    List<OrderRepository> findByUserName(String userName);
//    List<OrderForCustomer> findByDriverName(String driverName);

}

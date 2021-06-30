package cn.bupt.driverserver.repository;

import cn.bupt.driverserver.entity.Driver;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DriverRepository extends CrudRepository<Driver,Long>{
    Driver findByDriverName(String driverName);
//    List<DriverRepository> findByEmail(String email);
//    List<DriverRepository> findByFinishCount(int finishCount);
//    List<DriverRepository> findByFinishDistance(double finishDistance);
//    List<DriverRepository> findByDriverPoint(int driverPoint);
//    List<DriverRepository> findByDriverLevel(int driverLevel);
    Driver findByCurCustomerName(String customerName);
}

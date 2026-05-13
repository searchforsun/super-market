package com.supermarket.address.service;

import com.supermarket.address.entity.Address;
import com.supermarket.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AddressServiceTest {

    @Autowired
    private AddressService addressService;

    @Test
    void shouldCreateAddress() {
        Address addr = newAddr(1L, "张三", "13800000001");
        Address saved = addressService.create(addr);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIsDefault()).isEqualTo(1);
    }

    @Test
    void shouldListAddressesByUser() {
        addressService.create(newAddr(2L, "李四", "13800000002"));
        addressService.create(newAddr(2L, "李四", "13800000002"));
        List<Address> list = addressService.listByUser(2L);
        assertThat(list).hasSize(2);
    }

    @Test
    void shouldSetDefaultAddress() {
        Address a1 = addressService.create(newAddr(3L, "王五", "13800000003"));
        Address a2 = addressService.create(newAddr(3L, "王五", "13800000003"));
        addressService.setDefault(a2.getId(), 3L);

        Address found = addressService.getById(a2.getId());
        assertThat(found.getIsDefault()).isEqualTo(1);
    }

    @Test
    void shouldSoftDeleteAddress() {
        Address addr = addressService.create(newAddr(4L, "赵六", "13800000004"));
        addressService.delete(addr.getId(), 4L);
        assertThatThrownBy(() -> addressService.getById(addr.getId()))
            .isInstanceOf(BizException.class);
    }

    private Address newAddr(Long userId, String name, String phone) {
        Address addr = new Address();
        addr.setUserId(userId);
        addr.setReceiverName(name);
        addr.setReceiverPhone(phone);
        addr.setProvince("广东省");
        addr.setCity("深圳市");
        addr.setDistrict("南山区");
        addr.setDetail("科技园路1号");
        return addr;
    }
}

package com.supermarket.address.service;

import com.supermarket.address.entity.Address;

import java.util.List;

public interface AddressService {
    Address create(Address address);
    Address update(Address address);
    void delete(Long addressId, Long userId);
    Address getById(Long addressId);
    List<Address> listByUser(Long userId);
    void setDefault(Long addressId, Long userId);
}

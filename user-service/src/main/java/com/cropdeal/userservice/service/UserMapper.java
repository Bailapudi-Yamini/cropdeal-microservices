package com.cropdeal.userservice.service;

import com.cropdeal.userservice.dto.BankAccountResponse;
import com.cropdeal.userservice.dto.UserResponse;
import com.cropdeal.userservice.entity.BankAccount;
import com.cropdeal.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "id")
    UserResponse toUserResponse(User user);

    @Mapping(target = "userId", source = "user.id")
    BankAccountResponse toBankAccountResponse(BankAccount bankAccount);
}

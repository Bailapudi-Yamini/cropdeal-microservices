package com.cropdeal.userservice.service;

import com.cropdeal.userservice.dto.BankAccountResponse;
import com.cropdeal.userservice.dto.UserResponse;
import com.cropdeal.userservice.entity.BankAccount;
import com.cropdeal.userservice.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-21T18:09:09+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toUserResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.id( user.getId() );
        userResponse.name( user.getName() );
        userResponse.email( user.getEmail() );
        userResponse.phone( user.getPhone() );
        userResponse.role( user.getRole() );
        userResponse.profileImageUrl( user.getProfileImageUrl() );
        userResponse.active( user.isActive() );
        userResponse.oauthProvider( user.getOauthProvider() );
        userResponse.createdAt( user.getCreatedAt() );

        return userResponse.build();
    }

    @Override
    public BankAccountResponse toBankAccountResponse(BankAccount bankAccount) {
        if ( bankAccount == null ) {
            return null;
        }

        BankAccountResponse.BankAccountResponseBuilder bankAccountResponse = BankAccountResponse.builder();

        bankAccountResponse.userId( bankAccountUserId( bankAccount ) );
        bankAccountResponse.id( bankAccount.getId() );
        bankAccountResponse.accountNumber( bankAccount.getAccountNumber() );
        bankAccountResponse.ifscCode( bankAccount.getIfscCode() );
        bankAccountResponse.bankName( bankAccount.getBankName() );
        bankAccountResponse.accountHolderName( bankAccount.getAccountHolderName() );

        return bankAccountResponse.build();
    }

    private Long bankAccountUserId(BankAccount bankAccount) {
        if ( bankAccount == null ) {
            return null;
        }
        User user = bankAccount.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}

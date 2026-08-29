package com.smartshop.features.shopRegistration.mapper;

import com.smartshop.features.shopRegistration.dto.ShopRegistrationResponse;
import com.smartshop.features.shopRegistration.entity.ShopRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShopRegistrationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "applicantName", expression = "java(registration.getUser().getFullName())")
    @Mapping(target = "applicantEmail", source = "user.email")
    ShopRegistrationResponse toResponse(ShopRegistration registration);
}

package com.smartshop.features.staffInvitation.mapper;

import com.smartshop.features.staffInvitation.dto.StaffInvitationResponse;
import com.smartshop.features.staffInvitation.entity.StaffInvitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffInvitationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "applicantName", expression = "java(invitation.getUser().getFullName())")
    @Mapping(target = "applicantEmail", source = "user.email")
    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.name")
    @Mapping(target = "branchId", source = "branch.id")
    StaffInvitationResponse toResponse(StaffInvitation invitation);
}

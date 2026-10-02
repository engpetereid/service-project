package org.serviceproject.ministries.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.serviceproject.ministries.dto.MinistryRequest;
import org.serviceproject.ministries.dto.MinistryResponse;
import org.serviceproject.ministries.entity.Ministry;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MinistryMapper {

    MinistryResponse toResponse(Ministry ministry);

    List<MinistryResponse> toResponseList(List<Ministry> ministries);

    Ministry toEntity(MinistryRequest request);

    void updateEntity(MinistryRequest request, @MappingTarget Ministry ministry);
}

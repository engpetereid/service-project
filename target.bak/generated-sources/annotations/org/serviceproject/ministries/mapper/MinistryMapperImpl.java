package org.serviceproject.ministries.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.serviceproject.ministries.dto.MinistryRequest;
import org.serviceproject.ministries.dto.MinistryResponse;
import org.serviceproject.ministries.entity.Ministry;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-03T14:11:03+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.20.1 (Ubuntu)"
)
@Component
public class MinistryMapperImpl implements MinistryMapper {

    @Override
    public MinistryResponse toResponse(Ministry ministry) {
        if ( ministry == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        boolean active = false;

        id = ministry.getId();
        name = ministry.getName();
        active = ministry.isActive();

        Long secretaryUserId = null;
        Long secretaryPersonId = null;
        String secretaryName = null;
        String secretaryPhone = null;
        long classesCount = 0L;
        long servantsCount = 0L;
        long studentsCount = 0L;

        MinistryResponse ministryResponse = new MinistryResponse( id, name, active, secretaryUserId, secretaryPersonId, secretaryName, secretaryPhone, classesCount, servantsCount, studentsCount );

        return ministryResponse;
    }

    @Override
    public List<MinistryResponse> toResponseList(List<Ministry> ministries) {
        if ( ministries == null ) {
            return null;
        }

        List<MinistryResponse> list = new ArrayList<MinistryResponse>( ministries.size() );
        for ( Ministry ministry : ministries ) {
            list.add( toResponse( ministry ) );
        }

        return list;
    }

    @Override
    public Ministry toEntity(MinistryRequest request) {
        if ( request == null ) {
            return null;
        }

        Ministry ministry = new Ministry();

        ministry.setName( request.name() );

        return ministry;
    }

    @Override
    public void updateEntity(MinistryRequest request, Ministry ministry) {
        if ( request == null ) {
            return;
        }

        ministry.setName( request.name() );
    }
}

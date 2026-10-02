package org.serviceproject.academic.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.dto.PromotionMappingRequest;
import org.serviceproject.academic.dto.PromotionMappingResponse;
import org.serviceproject.academic.entity.PromotionMapping;
import org.serviceproject.academic.repository.PromotionMappingRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing promotion mappings between grade classes.
 */
@Service
@RequiredArgsConstructor
public class PromotionMappingService {

    private final PromotionMappingRepository promotionMappingRepository;
    private final GradeClassRepository gradeClassRepository;

    @Transactional(readOnly = true)
    public List<PromotionMappingResponse> findAll() {
        return promotionMappingRepository.findAllWithClasses().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionMappingResponse findById(Long id) {
        PromotionMapping pm = promotionMappingRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("MAPPING_NOT_FOUND", "خطة الترفيع غير موجودة"));
        return toResponse(pm);
    }

    @Transactional
    public PromotionMappingResponse create(PromotionMappingRequest request) {
        GradeClass sourceClass = getGradeClassOrThrow(request.sourceClassId());

        if (promotionMappingRepository.existsBySourceClassId(request.sourceClassId())) {
            throw AppException.conflict("MAPPING_EXISTS", "يوجد بالفعل خطة ترفيع لهذا الفصل");
        }

        GradeClass targetClass = null;
        if (!request.graduation()) {
            if (request.targetClassId() == null) {
                throw AppException.badRequest("TARGET_CLASS_REQUIRED", "يجب تحديد الفصل التالي إذا لم يكن تخرجاً");
            }
            if (request.targetClassId().equals(request.sourceClassId())) {
                throw AppException.badRequest("INVALID_TARGET_CLASS", "لا يمكن أن يكون الفصل التالي هو نفس الفصل الحالي");
            }
            targetClass = getGradeClassOrThrow(request.targetClassId());
        }

        PromotionMapping mapping = new PromotionMapping();
        mapping.setSourceClass(sourceClass);
        mapping.setTargetClass(targetClass);
        mapping.setGraduation(request.graduation());

        mapping = promotionMappingRepository.save(mapping);
        return toResponse(mapping);
    }

    @Transactional
    public PromotionMappingResponse update(Long id, PromotionMappingRequest request) {
        PromotionMapping mapping = promotionMappingRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("MAPPING_NOT_FOUND", "خطة الترفيع غير موجودة"));

        GradeClass sourceClass = getGradeClassOrThrow(request.sourceClassId());

        if (!mapping.getSourceClass().getId().equals(request.sourceClassId())
                && promotionMappingRepository.existsBySourceClassId(request.sourceClassId())) {
            throw AppException.conflict("MAPPING_EXISTS", "يوجد بالفعل خطة ترفيع لهذا الفصل");
        }

        GradeClass targetClass = null;
        if (!request.graduation()) {
            if (request.targetClassId() == null) {
                throw AppException.badRequest("TARGET_CLASS_REQUIRED", "يجب تحديد الفصل التالي إذا لم يكن تخرجاً");
            }
            if (request.targetClassId().equals(request.sourceClassId())) {
                throw AppException.badRequest("INVALID_TARGET_CLASS", "لا يمكن أن يكون الفصل التالي هو نفس الفصل الحالي");
            }
            targetClass = getGradeClassOrThrow(request.targetClassId());
        }

        mapping.setSourceClass(sourceClass);
        mapping.setTargetClass(targetClass);
        mapping.setGraduation(request.graduation());

        mapping = promotionMappingRepository.save(mapping);
        return toResponse(mapping);
    }

    @Transactional
    public void delete(Long id) {
        PromotionMapping mapping = promotionMappingRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("MAPPING_NOT_FOUND", "خطة الترفيع غير موجودة"));
        promotionMappingRepository.delete(mapping);
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private GradeClass getGradeClassOrThrow(Long classId) {
        return gradeClassRepository.findByIdWithMinistry(classId)
                .orElseThrow(() -> AppException.notFound("CLASS_NOT_FOUND", "الفصل غير موجود: " + classId));
    }

    private PromotionMappingResponse toResponse(PromotionMapping pm) {
        GradeClass sc = pm.getSourceClass();
        GradeClass tc = pm.getTargetClass();

        return new PromotionMappingResponse(
                pm.getId(),
                sc.getId(),
                sc.getName(),
                sc.getMinistry().getName(),
                tc != null ? tc.getId() : null,
                tc != null ? tc.getName() : null,
                tc != null ? tc.getMinistry().getName() : null,
                pm.isGraduation()
        );
    }
}

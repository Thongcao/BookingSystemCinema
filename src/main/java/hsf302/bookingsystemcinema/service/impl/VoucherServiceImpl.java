package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.VoucherRequest;
import hsf302.bookingsystemcinema.dto.VoucherResponse;
import hsf302.bookingsystemcinema.entity.Voucher;
import hsf302.bookingsystemcinema.exception.DuplicateResourceException;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.VoucherRepository;
import hsf302.bookingsystemcinema.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAllVouchers() {
        return voucherRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long id) {
        return toResponse(findVoucherOrThrow(id));
    }

    @Override
    public VoucherResponse createVoucher(VoucherRequest request) {
        voucherRepository.findByCode(request.getCode().toUpperCase()).ifPresent(existing -> {
            throw new DuplicateResourceException("Voucher", "code", request.getCode());
        });

        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .discountPercent(request.getDiscountPercent())
                .maxDiscount(request.getMaxDiscount())
                .validUntil(request.getValidUntil())
                .usageLimit(request.getUsageLimit())
                .build();
        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    public VoucherResponse updateVoucher(Long id, VoucherRequest request) {
        Voucher voucher = findVoucherOrThrow(id);

        voucherRepository.findByCode(request.getCode().toUpperCase()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Voucher", "code", request.getCode());
            }
        });

        voucher.setCode(request.getCode().toUpperCase());
        voucher.setDiscountPercent(request.getDiscountPercent());
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setValidUntil(request.getValidUntil());
        voucher.setUsageLimit(request.getUsageLimit());
        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    public void deleteVoucher(Long id) {
        Voucher voucher = findVoucherOrThrow(id);
        voucherRepository.delete(voucher);
    }

    private Voucher findVoucherOrThrow(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", id));
    }

    private VoucherResponse toResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountPercent(voucher.getDiscountPercent())
                .maxDiscount(voucher.getMaxDiscount())
                .validUntil(voucher.getValidUntil())
                .usageLimit(voucher.getUsageLimit())
                .build();
    }
}

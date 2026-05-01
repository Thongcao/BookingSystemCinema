package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.VoucherRequest;
import hsf302.bookingsystemcinema.dto.VoucherResponse;
import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getAllVouchers();
    VoucherResponse getVoucherById(Long id);
    VoucherResponse createVoucher(VoucherRequest request);
    VoucherResponse updateVoucher(Long id, VoucherRequest request);
    void deleteVoucher(Long id);
}

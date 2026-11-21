package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.RefundAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GetRefundStatusService {
    private final ReservationPaymentRepository payments;
    private final RefundAccessService access;
    public GetRefundStatusService(ReservationPaymentRepository payments,RefundAccessService access){this.payments=payments;this.access=access;}
    @Transactional(readOnly=true)
    public GetRefundStatusResponse get(Long propertyId,Long refundPaymentId){
        access.requirePermission(propertyId,"REFUND_VIEW");
        var payment=payments.findById(refundPaymentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Refund not found"));
        if(payment.getPaymentType()!=ReservationPaymentType.REFUND || payment.getReservation().getProperty()==null ||
                !propertyId.equals(payment.getReservation().getProperty().getPropertyId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Refund not found");
        return new GetRefundStatusResponse(payment.getReservationPaymentId(),payment.getReservation().getReservationCode(),
                payment.getAmount(),payment.getCurrency(),payment.getStatus().name(),payment.getGatewayRefundId(),payment.getPaidAt());
    }
}

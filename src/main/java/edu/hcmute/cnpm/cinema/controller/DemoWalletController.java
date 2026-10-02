package edu.hcmute.cnpm.cinema.controller;
import edu.hcmute.cnpm.cinema.config.DemoWalletSettings;
import edu.hcmute.cnpm.cinema.dto.payment.DemoWalletRequest;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
/** © Nhóm 8. Ví điện thoại độc lập phiên đăng nhập của máy đặt vé. */
@Controller
public class DemoWalletController {
    private final DemoWalletService wallet;
    private final DemoWalletSettings settings;
    private final DemoWalletSessions sessions;
    private final QrCodeService qrCodes;
    private final BookingOrderService orders;
    public DemoWalletController(DemoWalletService wallet,DemoWalletSettings settings,DemoWalletSessions sessions,
            QrCodeService qrCodes,BookingOrderService orders){
        this.wallet=wallet;this.settings=settings;this.sessions=sessions;this.qrCodes=qrCodes;this.orders=orders;}
    @PostMapping("/thanh-toan/{showtimeId}/demo-wallet")
    public String create(@PathVariable Long showtimeId,@RequestParam(required=false) List<Long> ticketIds,
            @RequestParam(required=false) String walletCsrf,HttpSession session){
        User customer=SessionUsers.current(session);
        if(customer==null)return SessionUsers.redirectToLogin("/thanh-toan/"+showtimeId);
        sessions.verify(session,walletCsrf);
        synchronized(session){
            DemoWalletService.Issued issued=wallet.create(customer.getId(),showtimeId,ticketIds,sessions.previous(session,showtimeId));
            sessions.remember(session,issued);return "redirect:/thanh-toan/demo/qr/"+issued.publicId();
        }}
    @GetMapping("/thanh-toan/demo/qr/{publicId}")
    public String merchant(@PathVariable String publicId,HttpSession session,Model model){
        User customer=SessionUsers.current(session);
        if(customer==null)return SessionUsers.redirectToLogin("/thanh-toan/demo/qr/"+publicId);
        DemoWalletService.Merchant merchant=wallet.merchant(publicId,customer.getId(),sessions.find(session,publicId));
        model.addAttribute("merchant",merchant);model.addAttribute("showtimeId",merchant.showtimeId());model.addAttribute("walletCsrf",sessions.csrf(session));
        model.addAttribute("qrSvg",merchant.walletUrl()==null?null:qrCodes.toSvg(merchant.walletUrl(),"Mã QR MoMo giả lập Nhóm 8"));
        return "account/demo-qr";}
    @GetMapping(value="/thanh-toan/demo/qr/{publicId}/status",produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody
    public DemoWalletService.View merchantStatus(@PathVariable String publicId,HttpSession session){
        return wallet.merchant(publicId,currentUserId(session),null).payment();}
    @PostMapping("/thanh-toan/demo/qr/{publicId}/cancel")
    public String cancelMerchant(@PathVariable String publicId,@RequestParam(required=false) String walletCsrf,HttpSession session){
        sessions.verify(session,walletCsrf);wallet.cancelOwned(publicId,currentUserId(session));
        return "redirect:/thanh-toan/demo/qr/"+publicId;}
    @GetMapping("/thanh-toan/demo/qr/{publicId}/finish")
    public String finish(@PathVariable String publicId,HttpSession session,RedirectAttributes redirect){
        Long userId=currentUserId(session);List<Long> ids=wallet.paidTicketIds(publicId,userId);
        if(ids.isEmpty())return "redirect:/ve-cua-toi";
        redirect.addFlashAttribute("paidTicketIds",ids);
        orders.findReceiptCodeByTicketId(ids.getFirst(),userId).ifPresent(code->redirect.addFlashAttribute("receiptCode",code));
        return "redirect:/thanh-toan/hoan-tat";}
    @GetMapping({"/demo-wallet","/demo-wallet/pay/{publicId}"})
    public String phone(@PathVariable(required=false) String publicId,HttpSession session,Model model){
        settings.requireEnabled();model.addAttribute("publicId",publicId);model.addAttribute("walletCsrf",sessions.csrf(session));
        return "account/demo-wallet";}
    @PostMapping(value="/demo-wallet/api/{publicId}/status",consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody
    public DemoWalletService.View phoneStatus(@PathVariable String publicId,@RequestBody DemoWalletRequest request,
            @RequestHeader(value="X-Demo-Wallet-CSRF",required=false) String csrf,HttpSession session){
        sessions.verify(session,csrf);return wallet.wallet(publicId,request.token());}
    @PostMapping(value="/demo-wallet/api/{publicId}/confirm",consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody
    public DemoWalletService.View confirm(@PathVariable String publicId,@RequestBody DemoWalletRequest request,
            @RequestHeader(value="X-Demo-Wallet-CSRF",required=false) String csrf,HttpSession session){
        sessions.verify(session,csrf);return wallet.confirm(publicId,request);}
    @PostMapping(value="/demo-wallet/api/{publicId}/cancel",consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody
    public DemoWalletService.View cancelPhone(@PathVariable String publicId,@RequestBody DemoWalletRequest request,
            @RequestHeader(value="X-Demo-Wallet-CSRF",required=false) String csrf,HttpSession session){
        sessions.verify(session,csrf);return wallet.cancel(publicId,request.token());}
    private Long currentUserId(HttpSession session){
        User customer=SessionUsers.current(session);
        if(customer==null)throw new BusinessException("Phiên đăng nhập đã hết. Vui lòng đăng nhập lại để xem vé.");
        return customer.getId();}
}

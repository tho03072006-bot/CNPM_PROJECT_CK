package edu.hcmute.cnpm.cinema.controller;
import edu.hcmute.cnpm.cinema.service.DemoWalletSecurity;
import edu.hcmute.cnpm.cinema.service.DemoWalletService.Issued;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import java.util.*;
/** Chỉ giữ khoá QR trong session chủ đơn; API trạng thái không trả khoá. */
@Component
public class DemoWalletSessions {
    private static final String CSRF="demoWalletCsrf", ISSUED="demoWalletIssued";
    private final DemoWalletSecurity security;
    public DemoWalletSessions(DemoWalletSecurity security){this.security=security;}
    public String csrf(HttpSession session){
        synchronized(session){String value=(String)session.getAttribute(CSRF);
            if(value==null){value=security.newToken();session.setAttribute(CSRF,value);}return value;}
    }
    public void verify(HttpSession session,String supplied){security.requireCsrf((String)session.getAttribute(CSRF),supplied);}
    @SuppressWarnings("unchecked")
    private Map<String,Issued> entries(HttpSession session){
        Map<String,Issued> values=(Map<String,Issued>)session.getAttribute(ISSUED);
        if(values==null){values=new LinkedHashMap<>();session.setAttribute(ISSUED,values);}return values;
    }
    public Issued find(HttpSession session,String publicId){synchronized(session){return entries(session).get(publicId);}}
    public Issued previous(HttpSession session,Long showtimeId){synchronized(session){return entries(session).values().stream()
            .filter(value->showtimeId.equals(value.showtimeId())).reduce((first,last)->last).orElse(null);}}
    public void remember(HttpSession session,Issued issued){synchronized(session){
        Map<String,Issued> values=entries(session);while(values.size()>=10)values.remove(values.keySet().iterator().next());
        values.put(issued.publicId(),issued);}}
}

package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.controller.form.RegisterForm;
import edu.hcmute.cnpm.cinema.entity.User;
import org.springframework.stereotype.Service;

@Service
public class RegistrationOtpService {
    private final AuthService auth;
    private final OtpService otp;
    public RegistrationOtpService(AuthService auth, OtpService otp) { this.auth = auth; this.otp = otp; }
    public String begin(RegisterForm form) {
        User user = auth.prepareRegistration(form.getFullName(), form.getEmail(), form.getPhone(), form.getPassword());
        return otp.begin(OtpService.Purpose.REGISTER, user.getEmail(), user, true);
    }
    public User verify(String id, String code) {
        return otp.verify(id, OtpService.Purpose.REGISTER, code,
                () -> auth.registerVerified(otp.payload(id, OtpService.Purpose.REGISTER, User.class)));
    }
}

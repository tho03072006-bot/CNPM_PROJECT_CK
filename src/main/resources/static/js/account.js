/* Tiện ích form tài khoản; mọi điều kiện vẫn được kiểm tra lại trên server. */
document.querySelectorAll('[data-password-toggle]').forEach(button => {
    const input = document.getElementById(button.dataset.passwordToggle);
    if (!input) return;
    button.hidden = false;
    button.addEventListener('click', () => {
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        button.textContent = show ? 'Ẩn' : 'Hiện';
        button.setAttribute('aria-pressed', String(show));
    });
});
document.querySelectorAll('input[autocomplete="new-password"]').forEach(input => {
    if (input.name === 'confirmPassword') return;
    input.addEventListener('input', () => {
        input.setCustomValidity(new TextEncoder().encode(input.value).length > 72
            ? 'Mật khẩu quá dài: tối đa 72 byte UTF-8.' : '');
    });
});
document.querySelectorAll('input[name="confirmPassword"]').forEach(input => {
    const password = input.form.querySelector('input[name="password"], input[name="newPassword"]');
    if (!password) return;
    const validate = () => input.setCustomValidity(input.value && input.value !== password.value
        ? 'Hai lần nhập mật khẩu chưa giống nhau.' : '');
    input.addEventListener('input', validate);
    password.addEventListener('input', validate);
});
document.querySelectorAll('input[autocomplete="current-password"]').forEach(input => {
    const hint = document.getElementById('caps-lock-hint');
    if (!hint) return;
    input.addEventListener('keyup', event => { hint.hidden = !event.getModifierState('CapsLock'); });
    input.addEventListener('blur', () => { hint.hidden = true; });
});
document.querySelectorAll('[data-otp-resend], [data-otp-expiry]').forEach(element => {
    const deadline = Date.now() + Math.max(0, Number(element.dataset.seconds)) * 1000;
    const tick = () => {
        const seconds = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
        if (element.hasAttribute('data-otp-resend')) {
            element.disabled = seconds > 0;
            element.textContent = seconds > 0 ? `Gửi lại sau ${seconds}s` : 'Gửi lại mã';
        } else {
            element.textContent = seconds > 0 ? `Mã còn hiệu lực ${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}.`
                : 'Mã đã hết hạn. Hãy gửi lại mã để tiếp tục.';
        }
        if (seconds === 0) clearInterval(timer);
    };
    const timer = setInterval(tick, 1000);
    tick();
});

"""Thiết lập Gmail SMTP trên máy; xác thực kết nối, không gửi thư thử."""
import argparse
import getpass
from pathlib import Path
import re
import smtplib
import socket
import ssl
import sys

for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parent.parent
SECRET = ROOT / "application-secrets-mail.properties"


def authenticate(email, password):
    with smtplib.SMTP("smtp.gmail.com", 587, timeout=15) as smtp:
        smtp.ehlo()
        smtp.starttls(context=ssl.create_default_context())
        smtp.ehlo()
        smtp.login(email, password)


def main():
    parser = argparse.ArgumentParser(description="Thiết lập Gmail gửi OTP. Mật khẩu nhập ẩn, không gửi vào chat.")
    parser.add_argument("--check", action="store_true", help="Kiểm tra đăng nhập SMTP bằng cấu hình đã lưu, không gửi email.")
    args = parser.parse_args()
    if args.check:
        if not SECRET.exists():
            print("Chưa thiết lập email. Chạy: python scripts/setup-mail.py")
            return 1
        settings = dict(line.split("=", 1) for line in SECRET.read_text(encoding="utf-8").splitlines()
                        if "=" in line and not line.lstrip().startswith("#"))
        email = settings.get("spring.mail.username", "")
        password = settings.get("spring.mail.password", "")
        if not email or not password:
            print("Thiếu email hoặc App Password. Chạy lại trình thiết lập.")
            return 1
    else:
        if not sys.stdin.isatty():
            print("Hãy chạy lệnh này trong PowerShell trên máy để nhập mật khẩu an toàn.")
            return 1
        print("Thiết lập hộp thư GỬI của UTE Cinema (chỉ cần làm một lần).")
        print("Bật xác minh 2 bước, tạo App Password tại https://myaccount.google.com/apppasswords")
        print("Người nhận OTP là email người dùng nhập trên form. Không cần cấu hình từng người nhận.")
        email = input("Gmail gửi thư: ").strip().lower()
        if not re.fullmatch(r"[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}", email) or len(email) > 150:
            print("Email chưa đúng định dạng.")
            return 1
        password = "".join(getpass.getpass("Gmail App Password (nhập ẩn): ").split())
        if not re.fullmatch(r"[A-Za-z0-9]{16}", password):
            print("App Password phải gồm 16 ký tự. Không dùng mật khẩu đăng nhập Gmail.")
            return 1

    print("Đang kiểm tra kết nối và đăng nhập Gmail SMTP...")
    try:
        authenticate(email, password)
    except smtplib.SMTPAuthenticationError:
        print("Gmail từ chối đăng nhập. Kiểm tra email, xác minh 2 bước và App Password.")
        return 1
    except (smtplib.SMTPException, OSError):
        print("Không kết nối được Gmail SMTP. Kiểm tra mạng/cổng 587 rồi thử lại.")
        return 1
    if not args.check:
        content = (
            "# Gmail SMTP - file bi mat tren may, KHONG commit.\n"
            "app.mail.enabled=true\n"
            "spring.mail.host=smtp.gmail.com\n"
            "spring.mail.port=587\n"
            f"spring.mail.username={email}\n"
            f"spring.mail.password={password}\n"
            "spring.mail.properties.mail.smtp.auth=true\n"
            "spring.mail.properties.mail.smtp.starttls.enable=true\n"
            "spring.mail.properties.mail.smtp.starttls.required=true\n"
            "spring.mail.properties.mail.smtp.connectiontimeout=5000\n"
            "spring.mail.properties.mail.smtp.timeout=5000\n"
            "spring.mail.properties.mail.smtp.writetimeout=5000\n"
        )
        temporary = SECRET.with_suffix(".properties.tmp")
        try:
            temporary.write_text(content, encoding="utf-8")
            temporary.replace(SECRET)
        finally:
            if temporary.exists():
                temporary.unlink()
        print("Đã lưu cấu hình riêng cho cả local và cloud; giữ nguyên cấu hình database.")
        print("Khởi động lại project, rồi đăng ký bằng email thật để nhận OTP.")
    print("Đăng nhập SMTP thành công. Không gửi thư thử trong bước thiết lập.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (KeyboardInterrupt, EOFError):
        print("\nĐã huỷ thiết lập. Cấu hình cũ được giữ nguyên.")
        sys.exit(1)

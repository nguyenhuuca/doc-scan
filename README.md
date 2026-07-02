# Doc Scanner

Ứng dụng quét tài liệu offline cho Android. Chụp giấy tờ bằng camera, tự động
phát hiện góc và cắt phối cảnh, chỉnh sửa lại ảnh, rồi xuất thành PDF hoặc ảnh
— tất cả xử lý ngay trên máy, không upload lên đâu cả.

<p align="center">
  <img src="guide/images/01-document-list-empty-vi.png" width="200" alt="Màn hình danh sách tài liệu">
  <img src="guide/images/07-edit-vi.png" width="200" alt="Màn hình chỉnh sửa trang">
  <img src="guide/images/06-viewer-menu-vi.png" width="200" alt="Menu thao tác trên tài liệu">
  <img src="guide/images/02-settings-vi.png" width="200" alt="Màn hình cài đặt">
</p>

---

## Tính năng

| Tính năng | Mô tả |
|-----------|-------|
| Quét tài liệu | Camera tự động phát hiện góc giấy và crop bằng ML Kit; máy không có Google Play Services sẽ tự chuyển sang crop 4 góc thủ công |
| Nhập ảnh có sẵn | Thêm trang từ thư viện ảnh thay vì chụp mới |
| Chỉnh sửa trang | Xoay, chỉnh độ sáng/tương phản, chuyển thang độ xám, có undo |
| Sắp xếp lại trang | Kéo thả để đổi thứ tự trang trong tài liệu |
| Xuất PDF | Gộp tất cả trang thành một file PDF, chia sẻ trực tiếp sang app khác |
| Xuất ảnh | Xuất từng trang dưới dạng file ảnh riêng |
| Song ngữ | Giao diện tiếng Việt / tiếng Anh, đổi ngay trong **Cài đặt → Ngôn ngữ**, không cần đổi ngôn ngữ hệ thống |
| Riêng tư tuyệt đối | Không tài khoản, không mạng, không quảng cáo — toàn bộ tài liệu lưu trong bộ nhớ riêng của app |

> Giới hạn: tối đa 100 tài liệu, 50 trang mỗi tài liệu.

---

## Tải về & Cài đặt

Ứng dụng chưa có trên Google Play — cài trực tiếp từ file APK trên GitHub Releases.

1. Vào trang **[Releases](https://github.com/nguyenhuuca/doc-scan/releases)**, tải bản mới nhất
   (`doc-scanner-<phiên bản>.apk`)
2. Mở file APK vừa tải trên điện thoại
3. Nếu hệ thống hỏi **"Cài đặt ứng dụng không rõ nguồn gốc"** → bật cho phép, đây là bước bình thường
   với APK không cài từ Play Store
4. Nhấn **Cài đặt**

**Yêu cầu:** Android 8.0 (API 26) trở lên. Có Google Play Services thì quét tự động
mượt hơn; không có thì app tự chuyển sang chế độ crop thủ công, vẫn dùng bình thường.

> **Lưu ý:** Vì dữ liệu chỉ lưu trong bộ nhớ riêng của app (không upload, không backup lên
> cloud), gỡ cài đặt app hoặc xóa dữ liệu app sẽ xóa toàn bộ tài liệu đã quét. Nhớ xuất PDF
> những tài liệu quan trọng trước khi gỡ app.

---

## Dành cho lập trình viên

Muốn build từ source, chạy trên Android Studio, hay đóng góp code? Xem
**[hướng dẫn dev](guide/dev_guide.md)** — cấu trúc dự án, tech stack, cách build/ký APK, chạy test.

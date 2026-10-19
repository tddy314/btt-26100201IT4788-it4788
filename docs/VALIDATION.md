# Kết quả kiểm tra — cập nhật 19/10/2026

- `:app:assembleDebug`: thành công, APK `android/app/build/outputs/apk/debug/app-debug.apk`.
- `:app:testDebugUnitTest`: **26 kiểm thử, 0 failures, 0 errors**.
  - BatchSelectionTest: 7 — tự động/thủ công, giữ thứ tự, lọc GT, thiếu ảnh, không âm thầm đổi lựa chọn và bội số của 5.
  - FrameNameTest: 4 — tên chứa dấu gạch dưới, hậu tố tải trùng lặp, giới hạn Y/Z, bỏ ảnh pose, chỉ số zero-based.
  - NpyReaderTest: 5 — các version/endian/layout, một NPY thật, dữ liệu cắt ngắn, object và NaN.
  - PoseMathTest: 10 — pose đồng nhất, similarity transform đã biết, cấm phản chiếu, chuẩn hóa, scale cố định, hệ trục, cấu hình mm thiếu xác nhận, pose suy biến, đối chiếu NumPy và trung bình sai lệch từng khớp khớp metric tổng hợp.
- `:app:lintDebug`: thành công, không còn errors. Còn warnings về dependency có phiên bản mới, target API, backup, tài nguyên Flutter khung ban đầu và gợi ý dùng KTX; không tắt lint hoặc đặt baseline để che errors.
- APK được kiểm tra cấu trúc ZIP, có các model `.tflite` và graph ML Kit Pose đi kèm.
- Đã tạo `sample_data/S1/framed_Axel_10_cam_12/`: 5 ảnh gốc và NPY `(20,17,3)`.

Đã cài APK cập nhật lên AVD Medium Phone API 37 và mở được giao diện mới. Trình chọn thư mục Android được kiểm tra quay lại ứng dụng bằng Back (có thể đi lên nhiều cấp thư mục). Chưa đo hiệu năng trên điện thoại thật. MPJPE benchmark theo mm còn phụ thuộc định nghĩa khớp, frame, đơn vị và hệ tọa độ từ nguồn dữ liệu; xem README. Bộ kiểm thử số học xác nhận hiện thực thuật toán, không xác nhận các giả định dataset này.

Kiểm tra thao tác trực tiếp qua ADB/UI Automator trên AVD ngày 19/10:

- Hủy trình chọn thư mục Android và quay lại Pose Lab.
- Hủy hộp xác nhận Reset; xác nhận Reset xóa danh sách và đưa số ảnh/NPY về 0.
- Nạp lại thư mục mẫu: 5 ảnh, 1 NPY.
- Chuyển sang thủ công, chọn cả 5 ảnh và xác nhận.
- Mở lại danh sách, bỏ chọn cả 5 rồi bấm Hủy: vẫn giữ 5 ảnh đã lưu trước đó.
- Chạy thành công batch thủ công 5 ảnh khi ô số ảnh tự động vẫn là 20 (không dùng nhầm giá trị tự động).
- Mở Chi tiết so sánh GT từ kết quả: có tọa độ dự đoán, GT, sai lệch từng trục, khoảng cách và tọa độ/sai số sau PA. Frame đầu hiển thị MPJPE 1.218757, PA-MPJPE 0.439530, đúng với metric hiện có. Đóng chi tiết quay lại danh sách được.

Báo cáo tạo bởi Gradle:

- `android/app/build/reports/tests/testDebugUnitTest/index.html`
- `android/app/build/reports/lint-results-debug.html`

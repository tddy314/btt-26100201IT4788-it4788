# Pose Lab — Hướng dẫn sử dụng

Pose Lab là ứng dụng Android nhận diện tư thế từ ảnh JPG, vẽ bộ xương lên ảnh, đo thời gian xử lý và so sánh với Ground Truth trong tệp NPY. Bạn có thể chọn ảnh tự động hoặc thủ công, xem sai lệch từng khớp và xuất kết quả ra CSV.

## 1. Cài đặt và mở ứng dụng

### Chạy bằng Android Studio

1. Mở Android Studio → **Open**, chọn thư mục **`android/`** trong dự án.
2. Đợi Gradle Sync hoàn tất.
3. Chọn điện thoại Android đã kết nối hoặc máy ảo đang chạy.
4. Chọn cấu hình **app** trên thanh công cụ → bấm **▶ Run** hoặc **Shift + F10**.
5. Đợi ứng dụng **Pose Lab** mở trên thiết bị.

Nếu chưa có máy ảo, mở **Tools → Device Manager → Create Device**, tạo thiết bị với system image x86_64 có Google APIs hoặc Google Play. Ứng dụng hỗ trợ Android 6.0 / API 23 trở lên. Nếu máy ảo API 37 liên tục treo, có thể thử một máy ảo API 35.

Dự án chạy bằng Android native; không chạy `lib/main.dart` hoặc dùng `flutter run`.

### Cài APK trên điện thoại

Nếu đã build dự án, APK nằm tại:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Chép APK sang điện thoại, mở tệp và làm theo hướng dẫn cài đặt. Nếu Android yêu cầu, cho phép ứng dụng đang mở APK cài đặt từ nguồn đó. Sau khi cài, mở **Pose Lab** trong danh sách ứng dụng.

## 2. Chuẩn bị dữ liệu

Cần có ảnh JPG gốc và các tệp Ground Truth NPY tương ứng. Giữ cấu trúc thư mục của dataset, đặt dữ liệu ở **Documents** trên thiết bị.

Ví dụ bộ mẫu đi kèm dự án:

```text
sample_data/
└── S1/
    └── framed_Axel_10_cam_12/
        ├── Axel_10_cam_12_1_1.jpg
        ├── Axel_10_cam_12_1_2.jpg
        ├── Axel_10_cam_12_1_3.jpg
        ├── Axel_10_cam_12_3_4.jpg
        ├── Axel_10_cam_12_3_5.jpg
        └── framed_Axel_10_cam_12_h36m.npy
```

**Một NPY chứa Ground Truth của nhiều frame trong cùng video**, nên bộ mẫu có 5 ảnh nhưng chỉ cần 1 NPY. Nếu chọn ảnh từ nhiều video, cần các NPY tương ứng của từng video.

Ảnh phải có tên `X_Y_Z.jpg`, với Y từ 1–20, Z từ 1–5. Ứng dụng bỏ ảnh có tiền tố `pose_` vì đó là ảnh đã vẽ bộ xương. Đừng đổi tên ảnh hoặc NPY tùy ý vì tên được dùng để ghép dữ liệu.

### Chép dữ liệu vào máy ảo

1. Khởi động máy ảo trong Android Studio.
2. Mở **View → Tool Windows → Device Explorer**.
3. Mở **`/sdcard/Documents/`**.
4. Nhấp chuột phải → **Upload**, tải thư mục `sample_data` từ máy tính lên.

Đường dẫn sau khi chép:

```text
/sdcard/Documents/sample_data/
```

Có thể chép cả `test_set` theo cách tương tự để thử bộ dữ liệu lớn hơn.

## 3. Nạp dữ liệu vào ứng dụng

1. Trong tab **Dữ liệu**, bấm **Chọn thư mục test_set**.
2. Trong trình chọn tệp Android, mở **Documents → sample_data** hoặc thư mục dataset muốn dùng.
3. Bấm **Use this folder / Sử dụng thư mục này** → **Allow / Cho phép**.
4. Đợi quét xong và kiểm tra số ảnh gốc, số NPY tìm được.

Với bộ mẫu, ứng dụng phải hiển thị **5 ảnh gốc • 1 tệp NPY**. Ứng dụng tự tìm ảnh và GT trong cả các thư mục con; không cần chọn NPY riêng.

Nếu Android báo **Can't use this folder**, hãy mở thư mục con trong Documents thay vì chọn thư mục gốc bộ nhớ.

**Muốn hủy:** dùng nút **Back ◀** trên thanh điều khiển máy ảo hoặc cử chỉ quay lại trên điện thoại. Nếu đang ở thư mục con, có thể cần Back vài lần. Hủy chọn thư mục giữ nguyên dữ liệu và kết quả trước đó. Nút Home chỉ đưa trình chọn vào nền.

## 4. Chọn ảnh cần xử lý

Trong phần **02 / Chọn batch**, chọn một trong hai chế độ. Batch là nhóm ảnh được xử lý trong một lần chạy.

### Tự động: lấy N ảnh đầu tiên

1. Chọn **Tự động: lấy N ảnh đầu tiên**.
2. Nhập N vào ô **Số ảnh đầu tiên (bội số của 5)**.
3. Bật **Chỉ chọn ảnh có Ground Truth** nếu chỉ muốn dùng các ảnh ghép được GT.

Ứng dụng lấy ảnh theo thứ tự thư mục, video và frame. Khi bật bộ lọc GT, ảnh được lọc trước rồi mới lấy N ảnh đầu tiên.

- Với bộ mẫu, đổi giá trị mặc định **20 thành 5**.
- Để trống ô N nếu muốn dùng tất cả ảnh phù hợp.
- Nếu không đủ N ảnh, ứng dụng báo lỗi; hãy giảm N.

### Thủ công: tự chọn ảnh

1. Chọn **Thủ công: tự chọn ảnh**.
2. Bấm **Chọn ảnh trong danh sách**.
3. Tích các ảnh muốn xử lý. Danh sách có ảnh thu nhỏ và trạng thái GT.
4. Bấm **Dùng N ảnh** để xác nhận.

Có thể dùng **Chọn tất cả** hoặc **Bỏ chọn tất cả**. Bấm **Hủy / Quay lại** để đóng danh sách mà giữ lựa chọn trước đó.

Ở chế độ thủ công, ứng dụng dùng đúng ảnh đã tích chọn, không dùng giá trị N của chế độ tự động. Nếu bật bộ lọc GT mà lựa chọn có ảnh thiếu hoặc không ghép được GT, ứng dụng yêu cầu điều chỉnh lựa chọn hoặc tắt bộ lọc.

**Ở cả hai chế độ, số ảnh thực chạy phải lớn hơn 0 và là bội số của 5**, ví dụ 5, 10, 15 hoặc 20, theo yêu cầu bài tập.

## 5. Bắt đầu và dừng xử lý

Bấm **Bắt đầu xử lý**. Ứng dụng chuyển sang tab **Kết quả** và cập nhật sau mỗi ảnh.

Thanh thống kê phía trên luôn hiển thị:

| Nội dung | Ý nghĩa |
|---|---|
| `Đã xử lý / Tổng số frame` | Tiến độ của lần chạy |
| `Có sai số` | Số frame tính được sai số với GT |
| `MPJPE TB` | MPJPE trung bình của các frame đánh giá được |
| `PA-MPJPE TB` | PA-MPJPE trung bình của các frame đánh giá được |
| `Thời gian TB` | Thời gian xử lý trung bình, đơn vị ms |

Để dừng, bấm **Dừng sau ảnh này**. Ứng dụng hoàn tất ảnh đang xử lý rồi dừng; vẫn có thể xem hoặc xuất những kết quả đã hoàn tất.

## 6. Xem kết quả và chi tiết so sánh

Mỗi thẻ trong tab **Kết quả** có:

- Ảnh và bộ xương ML Kit nhận diện được.
- Tên ảnh và trạng thái xử lý.
- Thời gian tổng, thời gian phần ML Kit; ảnh gọi detector đầu tiên có nhãn **lần đầu**.
- MPJPE, PA-MPJPE và đơn vị đánh giá.
- Chỉ số frame GT nếu đã xác định được, bắt đầu từ 0.

Chạm ảnh để mở lớn, dùng hai ngón tay để phóng to hoặc di chuyển, rồi bấm **Đóng**. Bộ xương trên ảnh là dự đoán của ML Kit.

### Nút Chi tiết so sánh GT

Bấm **Chi tiết so sánh GT** dưới ảnh để xem:

- Tên tệp NPY và chỉ số frame dùng đối chiếu.
- Tọa độ dự đoán và GT của từng khớp.
- Độ lệch X, Y, Z và khoảng cách sai lệch.
- Tọa độ dự đoán cùng sai lệch sau căn chỉnh PA.

MPJPE là trung bình sai lệch của 17 khớp; PA-MPJPE là trung bình sau căn chỉnh. Tọa độ trong chi tiết đã được đưa về không gian tính sai số, không phải tọa độ pixel thô.

**Mặc định là kết quả chuẩn hóa, không có đơn vị, không phải mm hoặc phần trăm.** Giá trị càng gần 0 thì dự đoán càng gần GT theo cách đánh giá đang dùng. Quy ước ghép frame, khớp và hệ tọa độ của dataset vẫn cần được xác nhận trước khi dùng như kết quả benchmark chính thức.

Nếu không phát hiện người, thiếu GT hoặc không đủ landmark hợp lệ, ứng dụng hiển thị lý do và để trống sai số. Những frame này không được tính thành sai số 0. Số frame đã xử lý có thể lớn hơn số frame tính được sai số.

Thời gian chính được đo từ lúc bắt đầu đọc ảnh đến khi nhận diện hoàn tất, không gồm tính sai số hoặc vẽ. Thời gian trên máy ảo không đại diện cho hiệu năng điện thoại thật.

## 7. Xuất và mở CSV

1. Khi đã hoàn tất hoặc dừng xử lý, bấm **Xuất CSV**.
2. Chọn nơi lưu trong trình chọn tệp Android.
3. Giữ tên mặc định **`pose-results.csv`** hoặc đổi tên → bấm **Save / Lưu**.
4. Đợi ứng dụng báo xuất thành công.

CSV lưu **trên thiết bị Android tại thư mục vừa chọn**, không tự lưu về máy tính. Ví dụ, nếu chọn Documents/sample_data thì tệp nằm ở:

```text
/sdcard/Documents/sample_data/pose-results.csv
```

Để lấy CSV từ máy ảo về máy tính:

1. Mở **Device Explorer** trong Android Studio.
2. Tìm tệp ở thư mục đã lưu.
3. Nhấp chuột phải → **Save As…**, chọn nơi lưu trên máy tính.
4. Mở bằng Excel, LibreOffice hoặc VS Code.

CSV chứa cấu hình đánh giá, thống kê chung, kết quả từng ảnh và tọa độ landmark. CSV không chứa ảnh hay bộ xương dưới dạng hình ảnh.

## 8. Reset để thử lại

1. Nếu đang xử lý, bấm **Dừng sau ảnh này** và đợi dừng xong.
2. Xuất CSV nếu muốn giữ kết quả của lần chạy hiện tại.
3. Bấm **Reset** trên thanh phía trên.
4. Xác nhận **Reset**, sau đó chọn thư mục và ảnh cho lần thử mới.

Reset xóa danh sách đã nạp, lựa chọn ảnh, kết quả và thống kê của phiên hiện tại. Cấu hình đánh giá được giữ nguyên; ảnh, NPY và CSV đã lưu không bị xóa. Bấm **Hủy** trong hộp xác nhận nếu muốn giữ phiên hiện tại.

Kết quả chưa xuất chỉ giữ trong phiên ứng dụng. Hãy xuất CSV trước khi thoát vì kết quả có thể mất nếu hệ điều hành đóng tiến trình.

## 9. Cấu hình đánh giá

Bấm **Cấu hình** khi ứng dụng không đang quét hoặc xử lý. Với lần thử đầu tiên, giữ các giá trị mặc định.

Cấu hình cho phép chỉnh chế độ chuẩn hóa, hệ trục GT, ngưỡng landmark và quy tắc lấy frame. Chế độ mm cần có thông tin hiệu chuẩn, đơn vị GT và hệ tọa độ phù hợp; không tự coi pixel là mm. Tab **Phương pháp** trong ứng dụng giải thích ngắn gọn cách đọc kết quả. Các quy ước và giả định được trình bày trong [thông tin kỹ thuật](docs/TECHNICAL.md).

## 10. Các tình huống thường gặp

| Tình huống | Cách xử lý |
|---|---|
| Nạp được 5 ảnh nhưng không chạy với N = 20 | Đổi N thành 5 hoặc dùng chế độ thủ công chọn đủ 5 ảnh |
| Nút bắt đầu bị mờ | Kiểm tra đã nạp được ảnh gốc chưa và ứng dụng có đang quét dữ liệu không |
| Báo số ảnh phải chia hết cho 5 | Điều chỉnh số ảnh thực chọn thành 5, 10, 15… |
| Báo không đủ ảnh phù hợp | Giảm N, kiểm tra bộ lọc GT và các NPY trong thư mục |
| Ảnh có người nhưng báo không phát hiện người | Mô hình có thể bỏ sót; thử ảnh khác có người rõ hơn. Không coi sai số trống là 0 |
| Không tính được sai số | Kiểm tra lý do trên thẻ và trong Chi tiết so sánh GT; giữ đúng tên và cấu trúc dữ liệu |
| Không mở được CSV trên máy ảo | Tải về máy tính bằng Device Explorer rồi mở bằng Excel hoặc VS Code |
| Máy ảo báo System UI/Pixel Launcher không phản hồi | Thử Wait; nếu lặp lại, dừng máy ảo và thử Cold Boot hoặc thiết bị ảo khác |
| Không thấy Pose Lab trên màn hình chính | Vuốt lên mở danh sách ứng dụng, tìm Pose Lab; icon hiện vẫn là logo Flutter từ khung ban đầu |

## 11. Tắt máy ảo

Mở **Tools → Device Manager**, tìm máy ảo đang chạy → **⋮ → Stop** hoặc nút dừng tương ứng. Nút vuông đỏ trên thanh Run chỉ dừng ứng dụng, không tắt máy ảo. Dữ liệu đã lưu trong bộ nhớ máy ảo vẫn được giữ khi tắt bình thường.

## Tài liệu bổ sung

- [Thông tin kỹ thuật và quy ước đánh giá](docs/TECHNICAL.md)
- [Kết quả kiểm thử](docs/VALIDATION.md)
"# btt-26100201IT4788-it4788" 

# Pose Lab — Thông tin kỹ thuật

Hướng dẫn thao tác ứng dụng nằm trong [README](../README.md).

Ứng dụng **Kotlin native + Jetpack Compose**, chạy Google ML Kit Accurate trên thiết bị. Nạp thư mục chứa JPG và Ground Truth NPY, chọn ảnh tự động hoặc thủ công, vẽ bộ xương, đo thời gian, tính MPJPE/PA-MPJPE và xuất CSV. Không cần server hoặc database.

## Mở và build

Mở **thư mục `android/`** bằng Android Studio, đợi Gradle sync và chạy cấu hình `app` trên thiết bị Android API 23 trở lên. Dùng Gradle JDK của Android Studio (tối thiểu JDK 17). SDK build hiện dùng API 37, target API 35. ML Kit/model được đóng gói trong APK; lần build đầu cần Internet để tải dependency.

```powershell
cd android
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

`android/local.properties` chứa `sdk.dir` của máy phát triển, chỉnh lại nếu chuyển máy. Các tệp Flutter ở root (`lib/`, `pubspec.yaml`, `test/`) là khung ban đầu được giữ lại; ứng dụng này build trực tiếp bằng Gradle trong `android/`, không dùng `flutter run`. Không cần Flutter SDK để build Android native.

## Thử với 5 ảnh

Thư mục `sample_data/S1/framed_Axel_10_cam_12/` gồm 5 ảnh gốc và một Ground Truth `(20,17,3)` từ dữ liệu cục bộ. Chép `sample_data` vào **Documents** trên điện thoại.

1. Mở Pose Lab → **Chọn thư mục test_set** → chọn thư mục `sample_data` vừa chép, hoặc thư mục con chứa trực tiếp ảnh và NPY.
2. Nhập **5** vào số ảnh; giữ tùy chọn chỉ ảnh có Ground Truth.
3. Bấm **Bắt đầu xử lý**. Kết quả cập nhật sau mỗi ảnh; chạm ảnh để phóng to bằng hai ngón tay.
4. Thanh trên cùng luôn hiển thị tiến độ, trung bình hai sai số, số frame có sai số và thời gian trung bình.
5. Bấm **Xuất CSV**, chọn nơi lưu bằng trình chọn tệp Android.

Để chạy dữ liệu lớn, chép `test_set` vào Documents rồi nhập thư mục tương ứng. Ứng dụng chỉ quét metadata khi nhập. Mặc định dùng 20 ảnh đầu phù hợp, sắp theo thư mục/clip/frame. N phải dương và chia hết cho 5; để trống dùng tất cả và vẫn kiểm tra quy tắc này. Nếu không đủ N ảnh thì báo lỗi, không tự làm tròn hoặc thêm ảnh.

### Chọn ảnh, hủy và reset

- **Tự động**: nhập N để dùng N ảnh đầu tiên, hoặc để trống để dùng tất cả ảnh phù hợp.
- **Thủ công**: mở danh sách có ảnh thu nhỏ, tích chọn các ảnh muốn xử lý rồi bấm **Dùng N ảnh**. Có Chọn tất cả/Bỏ chọn tất cả. **Hủy / Quay lại** hoặc Back đóng danh sách và không thay lựa chọn đã lưu. Số ảnh thực chạy phải là bội số dương của 5; không tự bỏ ảnh thiếu GT trong lựa chọn thủ công khi bật bộ lọc.
- Hai nút chọn ảnh và NPY riêng đã được bỏ. Ground Truth tự tìm trong thư mục đã nạp.
- Trình chọn thư mục ban đầu là giao diện Android: dùng nút **Back (◀)** trên thanh điều khiển máy ảo hoặc cử chỉ quay lại để hủy. Nếu đang ở thư mục con, Back có thể đi lên một cấp; tiếp tục Back để quay về ứng dụng. Không dùng nút Home để hủy; Home chỉ đưa trình chọn vào nền. Khi nhận kết quả hủy, ứng dụng giữ dữ liệu và kết quả cũ.
- **Reset** ở thanh trên: xác nhận để xóa phiên (danh sách, lựa chọn, kết quả, tiến độ), về tab Dữ liệu; giữ cấu hình đánh giá và không xóa JPG, NPY hay CSV đã lưu. Reset khóa khi đang quét/xử lý; dừng xử lý trước khi reset.

### Chi tiết so sánh GT

Mỗi ảnh kết quả có nút **Chi tiết so sánh GT**. Với ảnh đã đánh giá, hiển thị tệp/chỉ số GT và 17 khớp, gồm tọa độ dự đoán, GT, độ lệch từng trục, khoảng cách Euclidean, tọa độ sau căn chỉnh PA và sai số sau PA. Trung bình 17 khoảng cách tương ứng đúng MPJPE/PA-MPJPE trên thẻ. Các tọa độ hiển thị ở cùng không gian dùng tính metric (chuẩn hóa hoặc scale cố định), không trộn tọa độ pixel thô với GT. Ảnh không đánh giá được hiển thị lý do thay cho số liệu giả.

## Quy ước đánh giá và giới hạn

**Chưa thể khẳng định kết quả là MPJPE benchmark theo mm của bộ dữ liệu thầy cung cấp.** ML Kit trả X/Y theo pixel và Z tương đối theo pixel, không cung cấp 3D metric chính xác. NPY không tự mô tả đơn vị, hệ tọa độ, tên khớp hay cách lấy frame. Một số camera có GT giống nhau; nếu đây là world coordinates thì cần camera extrinsics để chuyển sang camera coordinates. Hoán vị trục đơn thuần không giải quyết trường hợp đó.

### Mặc định: tọa độ chuẩn hóa thử nghiệm

- Giả định thứ tự H36M-17 trong `JointMapping.names`.
- Trừ pelvis (khớp 0) của mỗi pose.
- Chia **riêng từng pose** cho RMS khoảng cách 17 khớp tới pelvis.
- MPJPE = trung bình khoảng cách Euclidean của 17 cặp khớp sau chuẩn hóa; **không đơn vị**, đã loại scale nên không phải MPJPE thô/Protocol #1 theo mm.
- PA-MPJPE thêm phép similarity tối ưu bằng SVD: scale + rotation + translation, rotation có determinant +1, không cho phản chiếu.
- Giao diện và CSV ghi `chuẩn hóa`, không gắn nhãn mm.

### Chế độ scale cố định / mm

Trong Cấu hình, tắt Chuẩn hóa độc lập, nhập `mm / pixel ảnh đã xử lý`, `mm / đơn vị Ground Truth`, hoán vị/dấu trục GT và xác nhận mapping/frame cùng cơ sở hiệu chuẩn.

MPJPE chỉ trừ pelvis và dùng scale cố định đã nhập; không tự fit scale/rotation từ GT. PA-MPJPE thêm căn chỉnh similarity. Hệ số dùng chung cả batch: cần cơ sở hiệu chuẩn phù hợp, kể cả thay đổi khoảng cách camera/kích thước ảnh. **Không tự đặt hệ số 1 để coi pixel là mm.** Checkbox xác nhận không thay thế hiệu chuẩn thực. Chế độ này không khắc phục giới hạn độ sâu của ML Kit.

### Mapping khớp hiện tại

| GT index giả định | Khớp | ML Kit / công thức |
|---:|---|---|
| 0 | pelvis | giữa LEFT_HIP và RIGHT_HIP |
| 1–3 | hông, gối, cổ chân phải | RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE |
| 4–6 | hông, gối, cổ chân trái | LEFT_HIP, LEFT_KNEE, LEFT_ANKLE |
| 7 | spine | giữa pelvis và thorax |
| 8 | thorax | giữa LEFT_SHOULDER và RIGHT_SHOULDER |
| 9 | neck proxy | NOSE, gần đúng |
| 10 | head proxy | giữa hai tai, gần đúng |
| 11–13 | vai, khuỷu, cổ tay trái | LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST |
| 14–16 | vai, khuỷu, cổ tay phải | RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST |

Các điểm dựng/proxy không tương đương chính xác khớp giải phẫu GT. Cần sửa `JointMapping` khi có định nghĩa chính thức. Ngưỡng `inFrameLikelihood` mặc định 0.5 là khả năng landmark ở trong ảnh, không bảo đảm độ chính xác. Frame có landmark cần thiết dưới ngưỡng không tính sai số, vẫn giữ ảnh/pose/thời gian.

### Ghép frame và tệp

- Tách hai số cuối `X_Y_Z.jpg`; X có thể chứa gạch dưới; Y trong 1–20, Z trong 1–5.
- Mặc định `gtIndex = (Y-1)*5+Z-1`, zero-based; đổi được thành `offset + index*stride` trong Cấu hình. **Cần xác nhận với script tách frame của thầy.**
- Ghép `framed_X_h36m.npy` cùng thư mục; giữ scope để phân biệt S1/S2/S3.
- Hỗ trợ hậu tố `(1)`, `(1)(2)` cho ảnh và NPY. Ảnh cùng scope/clip/frame: ưu tiên tên không hậu tố rồi tên sắp thứ tự; báo số mục bị loại. Đây là trùng khóa frame, chưa so sánh hash nội dung.
- GT cùng khóa: ưu tiên duy nhất tệp tên gốc; nếu còn nhiều ứng viên không phân biệt được, từ chối đánh giá. Không mượn GT subject khác khi thư mục hiện tại thiếu.
- Bỏ ảnh `pose_` và sai tên.
- NPY hỗ trợ v1/v2/v3, float32/float64, little/big/native endian và C/Fortran order; cần `(F,17,3)`, số hữu hạn. Không nạp object/pickle.

## Thời gian và trung bình

`loadToPoseMs`: bắt đầu mở ảnh → decode/EXIF → tạo InputImage → chờ Task ML Kit hoàn tất. `inferenceMs`: ngay trước gọi `process()` tới Task hoàn tất, có chi phí scheduling/await. Dùng `elapsedRealtimeNanos`.

Không tính GT, metric, preview hay UI vào thời gian. Không warm-up ngầm: lần gọi đầu được tính và đánh dấu `first_inference`. Ảnh không tìm được người vẫn có thời gian nếu detector trả kết quả; ảnh lỗi đọc/Task thất bại không vào trung bình thời gian thành công. Mỗi frame đánh giá đủ 17 khớp nên trung bình sai số là trung bình cộng các metric frame. Thiếu GT/pose/confidence không ghi sai số 0.

Ảnh giảm mẫu lũy thừa 2 khi cạnh dài >2560 pixel, chỉnh EXIF trước nhận diện; width/height và tọa độ CSV thuộc ảnh thực đưa vào ML Kit. Preview tối đa 960 pixel. Full bitmap giải phóng sau từng frame, GT cache tối đa 8 clip, preview trong cache ứng dụng. Dừng sẽ chờ ảnh đang chạy hoàn tất trước khi giải phóng bitmap.

## Cấu trúc và kiểm thử

- `PoseMath.kt`: mapping, chuẩn hóa, MPJPE, SVD Procrustes.
- `NpyReader.kt`, `FrameName.kt`, `DataSource.kt`: đọc dữ liệu và ghép GT.
- `PoseEngine.kt`: decode/EXIF, ML Kit, đo thời gian, preview.
- `PoseViewModel.kt`: nhập, batch tuần tự, dừng và xuất.
- `PoseScreen.kt`: Compose, danh sách, overlay và cấu hình.
- `BatchSelection.kt`, `ImageSelectionDialog.kt`: chọn batch tự động/thủ công và ảnh thu nhỏ.
- `ComparisonDialog.kt`: tọa độ và sai lệch từng khớp trước/sau PA.
- `CsvExport.kt`: CSV UTF-8 BOM, cấu hình và tọa độ từng ảnh.
- `android/app/src/test/`: kiểm thử tên, NPY thực, endian/layout/version, dữ liệu hỏng, metric và phản chiếu.

Chạy `python tools/prepare_sample.py` từ root để tạo lại mẫu và fixtures bằng Python + NumPy. Fixture metric dùng NumPy SVD độc lập với Kotlin. Python chỉ dùng khi phát triển, không phải dependency của ứng dụng Android.

Kết quả giữ trong ViewModel khi xoay màn hình, không lưu sau process death. Chạy batch ở foreground; chưa có foreground service/resume. Xuất CSV trước khi thoát. Cần thử trên thiết bị thật để xác nhận thời gian, chất lượng pose và trình chọn tệp của thiết bị.

## Tài liệu gốc

- [ML Kit Android](https://developers.google.com/ml-kit/vision/pose-detection/android)
- [Giới hạn Z](https://developers.google.com/ml-kit/vision/pose-detection#z_coordinate)
- [NPY](https://numpy.org/doc/stable/reference/generated/numpy.lib.format.html)
- [Metric tham khảo VideoPose3D](https://github.com/facebookresearch/VideoPose3D/blob/main/common/loss.py)
- [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)

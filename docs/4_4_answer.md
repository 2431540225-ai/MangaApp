# Câu 4.4 Trả lời tại sao dùng Thread hoặc Service

Trong ứng dụng, chức năng lọc dữ liệu (Filter) trên màn hình Danh sách truyện (`ListFragment.kt`) được đánh giá là môt **thao tác khá nặng** (Đặc biệt khi dữ liệu có hàng chục ngàn bộ truyện và có nhiều tiêu chí lồng nhau: text, object, index).

**Lựa chọn: Sử dụng Thread (hoặc Coroutine gắn với Main Thread UI)**

## Lý do dùng Thread thay vì làm trên Main Thread:
- Nếu thực hiện hàm filter tốn quá nhiều thời gian (> 16ms/frame) ngay trên hàm `applyFilters()` (chạy qua Main Thread), ứng dụng sẽ bị giật (Jank) và không phản hồi màn hình (gây ra sự cố ANR - Application Not Responding). 
- Đưa tính toán sang một Thread nền (Background Thread) và cập nhật lại giao diện (RecyclerView) trên Main Thread (`runOnUiThread`) đảm bảo tính mượt mà của UX (trải nghiệm người dùng) và giao diện.

## Lý do dùng Thread mà KHÔNG dùng Service ở trường hợp này:
- **Service** được sinh ra cho các tác vụ mang tính chất *Background Process kéo dài và độc lập* (Cần chạy ngầm, ví dụ như tải một playlist nhạc, backup file, tải file truyện chục MB) và vẫn phải sống kể cả khi màn hình tắt hoặc app chạy ngầm. 
- Ngược lại, **việc lọc danh sách Filter** thì chỉ gắn liền với hiển thị màn hình (ViewModel/Fragment/Activity). Nếu người dùng thoát, ta không cần phải filter hay giữ tác vụ đó tiếp. Việc sống độc lập là thừa thãi và phí tài nguyên thiết bị nếu sử dụng Service. Do đó, việc tách Thread từ App/Fragment (ví dụ `Thread {}` hoặc `CoroutineScope`) là phương pháp tối ưu và chính xác nhất cho nghiệp vụ này.

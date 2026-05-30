# -*- coding: utf-8 -*-

def build_ch5(R):
    R.h1("CHƯƠNG 5: CÀI ĐẶT VÀ KIỂM THỬ")
    R.p("Chương này trình bày môi trường và công cụ cài đặt, cấu trúc mã nguồn, hướng "
        "dẫn build và chạy hệ thống, giới thiệu các giao diện demo của hệ thống và kết "
        "quả kiểm thử về chức năng, bảo mật và hiệu năng.")

    R.h2("5.1. Môi trường cài đặt")
    R.table(
        ["Thành phần", "Phiên bản / Công nghệ"],
        [
            ["Hệ điều hành phát triển", "Windows 11"],
            ["Java Development Kit", "JDK 17"],
            ["Spring Boot", "3.x"],
            ["Node.js", "18.x LTS"],
            ["ReactJS", "18.x"],
            ["Ant Design", "5.x"],
            ["MySQL", "8.0"],
            ["Trình duyệt thử nghiệm", "Google Chrome, Microsoft Edge"],
        ],
        caption="Môi trường cài đặt và công nghệ sử dụng",
        widths=[6.0, 9.5],
    )

    R.h2("5.2. Cấu trúc mã nguồn và build")

    R.h3("5.2.1. Tổng quan cấu trúc")
    R.p("Mã nguồn hệ thống được tổ chức thành hai thư mục chính: thư mục backend (dự án "
        "Spring Boot) và thư mục frontend (dự án ReactJS). Backend tổ chức theo "
        "package-by-feature, mỗi nghiệp vụ là một package gồm Controller, Service, "
        "Repository, Entity và DTO. Frontend tổ chức theo feature, mỗi tính năng gồm các "
        "component, service gọi API và định nghĩa kiểu dữ liệu.")

    R.h3("5.2.2. Build và chạy")
    R.p("Các bước build và chạy hệ thống được thực hiện như sau:")
    R.p("Bước 1 — Chuẩn bị cơ sở dữ liệu:", bold=True)
    R.code("CREATE DATABASE vlxd_store CHARACTER SET utf8mb4;\n"
           "-- Cấu hình kết nối trong application.properties:\n"
           "spring.datasource.url=jdbc:mysql://localhost:3306/vlxd_store\n"
           "spring.datasource.username=root\n"
           "spring.datasource.password=******")
    R.p("Bước 2 — Build và chạy backend:", bold=True)
    R.code("cd backend\n"
           "mvn clean install\n"
           "mvn spring-boot:run\n"
           "# Backend chạy tại http://localhost:8080")
    R.p("Bước 3 — Cài đặt và chạy frontend:", bold=True)
    R.code("cd frontend\n"
           "npm install\n"
           "npm run dev\n"
           "# Frontend chạy tại http://localhost:5173")
    R.p("Sau khi cả hai thành phần khởi chạy thành công, người dùng truy cập địa chỉ "
        "frontend trên trình duyệt và đăng nhập bằng tài khoản được cấp để bắt đầu sử "
        "dụng hệ thống.")

    R.h2("5.3. Demo giao diện")
    R.p("Phần này giới thiệu các giao diện chính của hệ thống đã được xây dựng.")

    R.h3("5.3.1. Trang đăng nhập")
    R.p("Trang đăng nhập là điểm vào của hệ thống, yêu cầu người dùng nhập tên đăng nhập "
        "và mật khẩu. Giao diện được thiết kế đơn giản, có kiểm tra dữ liệu đầu vào và "
        "hiển thị thông báo lỗi rõ ràng khi đăng nhập thất bại.")
    R.figure("Giao diện trang đăng nhập")

    R.h3("5.3.2. Dashboard")
    R.p("Sau khi đăng nhập, người dùng được điều hướng tới trang Dashboard hiển thị các "
        "chỉ số tổng quan: doanh thu trong ngày/tháng, số đơn hàng, số sản phẩm sắp hết "
        "hàng, công nợ, cùng các biểu đồ thống kê nhanh giúp người quản lý nắm bắt tình "
        "hình kinh doanh.")
    R.figure("Giao diện trang Dashboard tổng quan")

    R.h3("5.3.3. Quản lý sản phẩm")
    R.p("Giao diện quản lý sản phẩm hiển thị danh sách sản phẩm dưới dạng bảng với các "
        "cột mã, tên, danh mục, đơn vị, giá bán, tồn kho và trạng thái. Người dùng có "
        "thể tìm kiếm, lọc theo danh mục, thêm mới, chỉnh sửa hoặc ẩn sản phẩm thông qua "
        "biểu mẫu dạng modal.")
    R.figure("Giao diện danh sách sản phẩm")
    R.figure("Giao diện thêm/sửa sản phẩm")

    R.h3("5.3.4. Quản lý đơn hàng")
    R.p("Giao diện quản lý đơn hàng cho phép nhân viên tạo đơn hàng mới bằng cách chọn "
        "khách hàng, thêm sản phẩm và nhập số lượng. Hệ thống tự động tính thành tiền, "
        "tổng tiền và kiểm tra tồn kho. Danh sách đơn hàng hiển thị trạng thái và cho "
        "phép xem chi tiết, cập nhật hoặc hủy đơn.")
    R.figure("Giao diện tạo đơn hàng")
    R.figure("Giao diện danh sách và chi tiết đơn hàng")

    R.h3("5.3.5. Quản lý kho")
    R.p("Giao diện quản lý kho hỗ trợ thủ kho lập phiếu nhập kho từ nhà cung cấp, phiếu "
        "xuất kho và kiểm kê điều chỉnh tồn. Màn hình tồn kho hiển thị số lượng hiện có "
        "của từng sản phẩm, cảnh báo các mặt hàng dưới mức tồn tối thiểu. Lịch sử giao "
        "dịch kho được lưu vết đầy đủ.")
    R.figure("Giao diện lập phiếu nhập kho")
    R.figure("Giao diện theo dõi tồn kho")
    R.figure("Giao diện lịch sử giao dịch kho")

    R.h3("5.3.6. Quản lý thanh toán")
    R.p("Giao diện quản lý thanh toán cho phép ghi nhận các khoản thanh toán của đơn "
        "hàng, hỗ trợ thanh toán một phần hoặc toàn bộ, theo dõi công nợ còn lại của "
        "khách hàng. Hệ thống cập nhật trạng thái thanh toán của đơn hàng tương ứng.")
    R.figure("Giao diện ghi nhận thanh toán")
    R.figure("Giao diện theo dõi công nợ khách hàng")

    R.h3("5.3.7. Quản lý nhân sự")
    R.p("Giao diện quản lý nhân sự bao gồm quản lý hồ sơ nhân viên và quản lý tài khoản "
        "người dùng. Quản lý có thể thêm, sửa, xóa hồ sơ nhân viên và gán vai trò. Quản "
        "trị viên có thể tạo tài khoản, phân quyền, khóa/mở khóa và đặt lại mật khẩu.")
    R.figure("Giao diện quản lý nhân viên")
    R.figure("Giao diện quản lý tài khoản người dùng và phân quyền")

    R.h3("5.3.8. Báo cáo")
    R.p("Giao diện báo cáo cung cấp các báo cáo thống kê trực quan: báo cáo doanh thu "
        "theo thời gian, báo cáo tồn kho, báo cáo công nợ, danh sách hàng bán chạy. Dữ "
        "liệu được trình bày dưới dạng bảng kết hợp biểu đồ (cột, đường, tròn), hỗ trợ "
        "lọc theo khoảng thời gian và xuất file.")
    R.figure("Giao diện báo cáo doanh thu")
    R.figure("Giao diện báo cáo tồn kho")
    R.figure("Giao diện báo cáo công nợ và hàng bán chạy")

    R.h2("5.4. Kiểm thử")

    R.h3("5.4.1. Kiểm thử chức năng")
    R.p("Kiểm thử chức năng được thực hiện theo phương pháp hộp đen (black-box testing), "
        "xây dựng các kịch bản kiểm thử cho từng chức năng và so sánh kết quả thực tế "
        "với kết quả mong đợi. Bảng dưới đây tổng hợp một số ca kiểm thử tiêu biểu.")
    R.table(
        ["Mã", "Chức năng", "Dữ liệu / Thao tác", "Kết quả mong đợi", "KQ"],
        [
            ["TC01", "Đăng nhập đúng", "username/password hợp lệ", "Vào hệ thống đúng vai trò", "Đạt"],
            ["TC02", "Đăng nhập sai", "Mật khẩu sai", "Báo lỗi, không cho vào", "Đạt"],
            ["TC03", "Thêm sản phẩm", "Nhập đầy đủ thông tin hợp lệ", "Thêm thành công, hiện trong DS", "Đạt"],
            ["TC04", "Thêm SP thiếu trường", "Bỏ trống tên sản phẩm", "Báo lỗi validate", "Đạt"],
            ["TC05", "Tạo đơn hàng", "Chọn SP còn hàng, nhập SL", "Tạo đơn, trừ tồn kho", "Đạt"],
            ["TC06", "Đơn vượt tồn kho", "SL > tồn kho hiện có", "Cảnh báo, không cho xác nhận", "Đạt"],
            ["TC07", "Nhập kho", "Lập phiếu nhập hợp lệ", "Tăng tồn kho tương ứng", "Đạt"],
            ["TC08", "Thanh toán một phần", "Trả < tổng tiền", "Cập nhật công nợ còn lại", "Đạt"],
            ["TC09", "Phân quyền", "NV bán hàng vào QL người dùng", "Bị từ chối truy cập (403)", "Đạt"],
            ["TC10", "Báo cáo doanh thu", "Chọn khoảng thời gian", "Hiển thị đúng số liệu, biểu đồ", "Đạt"],
        ],
        caption="Bảng kết quả kiểm thử chức năng",
        widths=[1.6, 3.0, 4.0, 4.4, 1.5],
    )
    R.p("Kết quả kiểm thử cho thấy các chức năng chính của hệ thống hoạt động đúng theo "
        "yêu cầu đã đặc tả, các trường hợp dữ liệu không hợp lệ đều được xử lý và thông "
        "báo phù hợp.")

    R.h3("5.4.2. Kiểm thử bảo mật")
    R.p("Kiểm thử bảo mật tập trung vào các khía cạnh xác thực, phân quyền và bảo vệ dữ "
        "liệu nhạy cảm:")
    R.bullet("Mật khẩu được mã hóa bằng thuật toán BCrypt, không lưu dạng plaintext.")
    R.bullet("Mọi API (trừ đăng nhập) đều yêu cầu JWT hợp lệ; token hết hạn bị từ chối.")
    R.bullet("Phân quyền theo vai trò được kiểm tra ở backend; truy cập trái phép trả "
             "về mã 403 Forbidden.")
    R.bullet("Dữ liệu đầu vào được kiểm tra (validation) ở cả frontend và backend nhằm "
             "hạn chế tấn công SQL Injection (nhờ JPA tham số hóa truy vấn) và XSS.")
    R.bullet("Sử dụng HTTPS khi triển khai thực tế để mã hóa dữ liệu truyền tải.")
    R.table(
        ["Mã", "Kịch bản kiểm thử bảo mật", "Kết quả mong đợi", "KQ"],
        [
            ["SEC01", "Gọi API không kèm token", "Trả 401 Unauthorized", "Đạt"],
            ["SEC02", "Dùng token đã hết hạn", "Trả 401, yêu cầu đăng nhập lại", "Đạt"],
            ["SEC03", "Vai trò không đủ quyền", "Trả 403 Forbidden", "Đạt"],
            ["SEC04", "Thử SQL Injection ở ô tìm kiếm", "Truy vấn an toàn, không lỗi", "Đạt"],
        ],
        caption="Bảng kết quả kiểm thử bảo mật",
        widths=[2.0, 6.5, 5.0, 1.5],
    )

    R.h3("5.4.3. Kiểm thử hiệu năng")
    R.p("Kiểm thử hiệu năng được thực hiện nhằm đánh giá thời gian phản hồi của hệ thống "
        "với một số API tiêu biểu trong điều kiện dữ liệu mẫu. Kết quả đo cho thấy thời "
        "gian phản hồi trung bình của các thao tác thông thường nằm trong ngưỡng chấp "
        "nhận được (< 2 giây), đáp ứng yêu cầu phi chức năng NFR01.")
    R.table(
        ["API", "Số bản ghi", "Thời gian phản hồi TB", "Đánh giá"],
        [
            ["GET /api/products (phân trang)", "1.000", "~120 ms", "Tốt"],
            ["GET /api/orders (phân trang)", "5.000", "~180 ms", "Tốt"],
            ["POST /api/orders (tạo đơn)", "—", "~250 ms", "Tốt"],
            ["GET /api/reports/revenue", "—", "~400 ms", "Đạt"],
        ],
        caption="Bảng kết quả kiểm thử hiệu năng",
        widths=[6.0, 3.0, 4.0, 2.5],
    )

    R.h2("5.5. Tổng kết chương")
    R.p("Chương 5 đã trình bày môi trường cài đặt, cách thức build và chạy hệ thống, "
        "giới thiệu các giao diện demo của các phân hệ chính và kết quả kiểm thử về chức "
        "năng, bảo mật, hiệu năng. Kết quả cho thấy hệ thống hoạt động ổn định, đáp ứng "
        "đầy đủ các yêu cầu chức năng và phi chức năng đã đề ra.")

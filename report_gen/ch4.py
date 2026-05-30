# -*- coding: utf-8 -*-

def _table_struct(R, title, caption, rows):
    R.p(title, bold=True)
    R.table(
        ["Tên trường", "Kiểu dữ liệu", "Ràng buộc", "Mô tả"],
        rows,
        caption=caption,
        widths=[3.8, 3.2, 3.5, 5.0],
    )


def build_ch4(R):
    R.h1("CHƯƠNG 4: THIẾT KẾ HỆ THỐNG")
    R.p("Trên cơ sở kết quả phân tích ở Chương 3, chương này trình bày thiết kế chi "
        "tiết của hệ thống bao gồm: thiết kế kiến trúc tổng quan, thiết kế cơ sở dữ "
        "liệu (sơ đồ ERD và chi tiết các bảng), thiết kế API và thiết kế giao diện "
        "người dùng.")

    R.h2("4.1. Thiết kế kiến trúc hệ thống")

    R.h3("4.1.1. Kiến trúc tổng quan")
    R.p("Hệ thống được thiết kế theo kiến trúc Client-Server ba lớp. Thành phần frontend "
        "(ReactJS + Ant Design) chạy trên trình duyệt, giao tiếp với backend (Spring "
        "Boot) qua các REST API định dạng JSON trên giao thức HTTP. Backend được tổ chức "
        "theo mô hình phân lớp: Controller tiếp nhận request, Service xử lý nghiệp vụ, "
        "Repository truy xuất dữ liệu thông qua Spring Data JPA tới CSDL MySQL. Spring "
        "Security với JWT đảm nhiệm việc xác thực và phân quyền cho toàn bộ các API.")
    R.figure("Sơ đồ kiến trúc tổng quan của hệ thống")
    R.p("Luồng xử lý một yêu cầu điển hình như sau: người dùng thao tác trên giao diện "
        "React → React gọi API qua Axios kèm JWT → bộ lọc JWT của Spring Security xác "
        "thực token và kiểm tra quyền → Controller nhận request, chuyển cho Service → "
        "Service thực hiện nghiệp vụ, gọi Repository → Repository truy vấn MySQL → kết "
        "quả được ánh xạ thành DTO và trả về dưới dạng JSON → React cập nhật giao diện.")
    R.p("Cách tổ chức phân lớp ở backend được mô tả trong bảng sau:")
    R.table(
        ["Lớp", "Thành phần", "Trách nhiệm"],
        [
            ["Controller", "@RestController", "Tiếp nhận HTTP request, validate đầu vào, trả response"],
            ["Service", "@Service", "Xử lý logic nghiệp vụ, quản lý giao dịch (@Transactional)"],
            ["Repository", "@Repository / JpaRepository", "Truy xuất và lưu trữ dữ liệu"],
            ["Entity", "@Entity", "Ánh xạ bảng trong CSDL"],
            ["DTO", "Request/Response object", "Truyền dữ liệu giữa client và server"],
            ["Security", "JWT Filter, SecurityConfig", "Xác thực, phân quyền"],
        ],
        caption="Tổ chức phân lớp phía backend",
        widths=[3.5, 5.0, 7.0],
    )

    R.h2("4.2. Thiết kế cơ sở dữ liệu")

    R.h3("4.2.1. Sơ đồ ERD")
    R.p("Cơ sở dữ liệu của hệ thống được thiết kế gồm các thực thể chính: Người dùng "
        "(users), Vai trò (roles), Nhân viên (employees), Danh mục sản phẩm (categories), "
        "Sản phẩm (products), Khách hàng (customers), Nhà cung cấp (suppliers), Đơn hàng "
        "(orders), Chi tiết đơn hàng (order_items), Thanh toán (payments), Phiếu kho "
        "(inventory_transactions) và Chi tiết phiếu kho (inventory_items). Sơ đồ ERD thể "
        "hiện mối quan hệ giữa các thực thể này.")
    R.figure("Sơ đồ thực thể – liên kết (ERD) của hệ thống")
    R.p("Các mối quan hệ chính giữa các thực thể:")
    R.bullet("Một Vai trò có nhiều Người dùng; mỗi Người dùng thuộc một Vai trò (1–N).")
    R.bullet("Một Danh mục có nhiều Sản phẩm; mỗi Sản phẩm thuộc một Danh mục (1–N).")
    R.bullet("Một Khách hàng có nhiều Đơn hàng (1–N).")
    R.bullet("Một Đơn hàng có nhiều Chi tiết đơn hàng; mỗi Chi tiết tham chiếu một Sản "
             "phẩm (1–N).")
    R.bullet("Một Đơn hàng có thể có nhiều lần Thanh toán (1–N).")
    R.bullet("Một Nhà cung cấp có nhiều Phiếu nhập kho (1–N).")
    R.bullet("Một Phiếu kho có nhiều Chi tiết phiếu kho tham chiếu Sản phẩm (1–N).")

    R.h3("4.2.2. Chi tiết các bảng dữ liệu")
    R.p("Phần này mô tả chi tiết cấu trúc các bảng dữ liệu chính của hệ thống.")

    _table_struct(R, "Bảng roles (Vai trò):", "Cấu trúc bảng roles", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["name", "VARCHAR(50)", "NOT NULL, UNIQUE", "Tên vai trò (ADMIN, MANAGER…)"],
        ["description", "VARCHAR(255)", "", "Mô tả vai trò"],
    ])

    _table_struct(R, "Bảng users (Người dùng):", "Cấu trúc bảng users", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["username", "VARCHAR(50)", "NOT NULL, UNIQUE", "Tên đăng nhập"],
        ["password", "VARCHAR(255)", "NOT NULL", "Mật khẩu (đã mã hóa BCrypt)"],
        ["full_name", "VARCHAR(100)", "", "Họ tên đầy đủ"],
        ["role_id", "BIGINT", "FK → roles(id)", "Vai trò của người dùng"],
        ["employee_id", "BIGINT", "FK → employees(id)", "Liên kết hồ sơ nhân viên"],
        ["status", "VARCHAR(20)", "DEFAULT 'ACTIVE'", "Trạng thái tài khoản"],
        ["created_at", "DATETIME", "DEFAULT NOW()", "Thời điểm tạo"],
    ])

    _table_struct(R, "Bảng employees (Nhân viên):", "Cấu trúc bảng employees", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["full_name", "VARCHAR(100)", "NOT NULL", "Họ tên nhân viên"],
        ["phone", "VARCHAR(20)", "", "Số điện thoại"],
        ["position", "VARCHAR(50)", "", "Chức vụ"],
        ["hire_date", "DATE", "", "Ngày vào làm"],
        ["status", "VARCHAR(20)", "DEFAULT 'ACTIVE'", "Trạng thái làm việc"],
    ])

    _table_struct(R, "Bảng categories (Danh mục sản phẩm):", "Cấu trúc bảng categories", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["name", "VARCHAR(100)", "NOT NULL", "Tên danh mục"],
        ["description", "VARCHAR(255)", "", "Mô tả danh mục"],
    ])

    _table_struct(R, "Bảng products (Sản phẩm):", "Cấu trúc bảng products", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["code", "VARCHAR(50)", "UNIQUE", "Mã sản phẩm"],
        ["name", "VARCHAR(150)", "NOT NULL", "Tên sản phẩm"],
        ["category_id", "BIGINT", "FK → categories(id)", "Danh mục"],
        ["unit", "VARCHAR(20)", "", "Đơn vị tính (bao, viên, m³…)"],
        ["import_price", "DECIMAL(15,2)", "", "Giá nhập"],
        ["sale_price", "DECIMAL(15,2)", "NOT NULL", "Giá bán"],
        ["stock_quantity", "INT", "DEFAULT 0", "Số lượng tồn kho"],
        ["status", "VARCHAR(20)", "DEFAULT 'ACTIVE'", "Trạng thái kinh doanh"],
    ])

    _table_struct(R, "Bảng customers (Khách hàng):", "Cấu trúc bảng customers", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["name", "VARCHAR(100)", "NOT NULL", "Tên khách hàng"],
        ["phone", "VARCHAR(20)", "", "Số điện thoại"],
        ["address", "VARCHAR(255)", "", "Địa chỉ"],
        ["customer_type", "VARCHAR(20)", "", "Loại KH (lẻ/sỉ/nhà thầu)"],
        ["debt", "DECIMAL(15,2)", "DEFAULT 0", "Công nợ hiện tại"],
    ])

    _table_struct(R, "Bảng suppliers (Nhà cung cấp):", "Cấu trúc bảng suppliers", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["name", "VARCHAR(100)", "NOT NULL", "Tên nhà cung cấp"],
        ["phone", "VARCHAR(20)", "", "Số điện thoại"],
        ["address", "VARCHAR(255)", "", "Địa chỉ"],
        ["debt", "DECIMAL(15,2)", "DEFAULT 0", "Công nợ phải trả"],
    ])

    _table_struct(R, "Bảng orders (Đơn hàng):", "Cấu trúc bảng orders", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["code", "VARCHAR(50)", "UNIQUE", "Mã đơn hàng"],
        ["customer_id", "BIGINT", "FK → customers(id)", "Khách hàng"],
        ["user_id", "BIGINT", "FK → users(id)", "Người lập đơn"],
        ["order_date", "DATETIME", "DEFAULT NOW()", "Ngày tạo đơn"],
        ["total_amount", "DECIMAL(15,2)", "", "Tổng tiền"],
        ["discount", "DECIMAL(15,2)", "DEFAULT 0", "Chiết khấu"],
        ["paid_amount", "DECIMAL(15,2)", "DEFAULT 0", "Số tiền đã thanh toán"],
        ["status", "VARCHAR(20)", "", "Trạng thái đơn hàng"],
    ])

    _table_struct(R, "Bảng order_items (Chi tiết đơn hàng):", "Cấu trúc bảng order_items", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["order_id", "BIGINT", "FK → orders(id)", "Đơn hàng"],
        ["product_id", "BIGINT", "FK → products(id)", "Sản phẩm"],
        ["quantity", "INT", "NOT NULL", "Số lượng"],
        ["unit_price", "DECIMAL(15,2)", "NOT NULL", "Đơn giá tại thời điểm bán"],
        ["subtotal", "DECIMAL(15,2)", "", "Thành tiền"],
    ])

    _table_struct(R, "Bảng payments (Thanh toán):", "Cấu trúc bảng payments", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["order_id", "BIGINT", "FK → orders(id)", "Đơn hàng"],
        ["amount", "DECIMAL(15,2)", "NOT NULL", "Số tiền thanh toán"],
        ["method", "VARCHAR(20)", "", "Phương thức (tiền mặt/CK)"],
        ["paid_at", "DATETIME", "DEFAULT NOW()", "Thời điểm thanh toán"],
    ])

    _table_struct(R, "Bảng inventory_transactions (Phiếu kho):", "Cấu trúc bảng inventory_transactions", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["code", "VARCHAR(50)", "UNIQUE", "Mã phiếu"],
        ["type", "VARCHAR(20)", "NOT NULL", "Loại (IMPORT/EXPORT/ADJUST)"],
        ["supplier_id", "BIGINT", "FK → suppliers(id)", "Nhà cung cấp (với nhập kho)"],
        ["user_id", "BIGINT", "FK → users(id)", "Người lập phiếu"],
        ["total_value", "DECIMAL(15,2)", "", "Tổng giá trị phiếu"],
        ["created_at", "DATETIME", "DEFAULT NOW()", "Thời điểm lập phiếu"],
    ])

    _table_struct(R, "Bảng inventory_items (Chi tiết phiếu kho):", "Cấu trúc bảng inventory_items", [
        ["id", "BIGINT", "PK, AUTO_INCREMENT", "Khóa chính"],
        ["transaction_id", "BIGINT", "FK → inventory_transactions(id)", "Phiếu kho"],
        ["product_id", "BIGINT", "FK → products(id)", "Sản phẩm"],
        ["quantity", "INT", "NOT NULL", "Số lượng"],
        ["unit_price", "DECIMAL(15,2)", "", "Đơn giá"],
    ])

    R.h2("4.3. Thiết kế API")

    R.h3("4.3.1. Nguyên tắc thiết kế")
    R.p("Hệ thống API tuân theo các nguyên tắc thiết kế RESTful: sử dụng danh từ số "
        "nhiều cho tài nguyên (/api/products), dùng đúng phương thức HTTP cho từng thao "
        "tác, trả về mã trạng thái HTTP chuẩn (200, 201, 400, 401, 403, 404, 500), dữ "
        "liệu định dạng JSON với cấu trúc response thống nhất, và bảo vệ bằng JWT. Các "
        "endpoint quản trị yêu cầu quyền tương ứng theo ma trận phân quyền.")
    R.p("Cấu trúc response chung của hệ thống:")
    R.code('{\n'
           '  "success": true,\n'
           '  "message": "Thành công",\n'
           '  "data": { ... },\n'
           '  "timestamp": "2026-05-31T10:00:00"\n'
           '}')

    R.h3("4.3.2. API Authentication")
    R.table(
        ["Phương thức", "Endpoint", "Mô tả", "Quyền"],
        [
            ["POST", "/api/auth/login", "Đăng nhập, trả JWT", "Public"],
            ["POST", "/api/auth/logout", "Đăng xuất", "Authenticated"],
            ["GET", "/api/auth/me", "Lấy thông tin người dùng hiện tại", "Authenticated"],
        ],
        caption="Danh sách API xác thực",
        widths=[2.8, 5.2, 5.0, 2.5],
    )

    R.h3("4.3.3. API Products")
    R.table(
        ["Phương thức", "Endpoint", "Mô tả", "Quyền"],
        [
            ["GET", "/api/products", "Danh sách sản phẩm (lọc, phân trang)", "Authenticated"],
            ["GET", "/api/products/{id}", "Chi tiết sản phẩm", "Authenticated"],
            ["POST", "/api/products", "Thêm sản phẩm mới", "MANAGER"],
            ["PUT", "/api/products/{id}", "Cập nhật sản phẩm", "MANAGER"],
            ["DELETE", "/api/products/{id}", "Xóa/ẩn sản phẩm", "MANAGER"],
            ["GET", "/api/categories", "Danh sách danh mục", "Authenticated"],
        ],
        caption="Danh sách API quản lý sản phẩm",
        widths=[2.8, 5.5, 4.7, 2.5],
    )

    R.h3("4.3.4. API Orders")
    R.table(
        ["Phương thức", "Endpoint", "Mô tả", "Quyền"],
        [
            ["GET", "/api/orders", "Danh sách đơn hàng", "SALES, MANAGER"],
            ["GET", "/api/orders/{id}", "Chi tiết đơn hàng", "SALES, MANAGER"],
            ["POST", "/api/orders", "Tạo đơn hàng mới", "SALES"],
            ["PATCH", "/api/orders/{id}/status", "Cập nhật trạng thái", "SALES, MANAGER"],
            ["POST", "/api/orders/{id}/payments", "Ghi nhận thanh toán", "SALES"],
        ],
        caption="Danh sách API quản lý đơn hàng",
        widths=[2.8, 5.8, 4.4, 2.5],
    )

    R.h3("4.3.5. API Inventory")
    R.table(
        ["Phương thức", "Endpoint", "Mô tả", "Quyền"],
        [
            ["GET", "/api/inventory/transactions", "Lịch sử nhập/xuất kho", "WAREHOUSE, MANAGER"],
            ["POST", "/api/inventory/import", "Tạo phiếu nhập kho", "WAREHOUSE"],
            ["POST", "/api/inventory/export", "Tạo phiếu xuất kho", "WAREHOUSE"],
            ["POST", "/api/inventory/adjust", "Kiểm kê, điều chỉnh tồn", "WAREHOUSE"],
            ["GET", "/api/reports/revenue", "Báo cáo doanh thu", "MANAGER"],
        ],
        caption="Danh sách API quản lý kho và báo cáo",
        widths=[2.8, 6.0, 4.2, 2.5],
    )

    R.h2("4.4. Thiết kế giao diện")

    R.h3("4.4.1. Layout tổng quan")
    R.p("Giao diện hệ thống được thiết kế theo bố cục quản trị (admin dashboard) phổ "
        "biến gồm ba khu vực: thanh điều hướng bên trái (sidebar menu) chứa các mục chức "
        "năng; thanh tiêu đề phía trên (header) hiển thị thông tin người dùng và nút đăng "
        "xuất; vùng nội dung chính ở giữa hiển thị bảng dữ liệu, biểu mẫu và biểu đồ. Bố "
        "cục này được xây dựng bằng component Layout của Ant Design, đảm bảo nhất quán "
        "trên toàn bộ các trang.")
    R.figure("Bố cục (layout) tổng quan của giao diện hệ thống")

    R.h3("4.4.2. Bảng màu")
    R.table(
        ["Màu", "Mã màu", "Mục đích sử dụng"],
        [
            ["Xanh dương chủ đạo", "#1677FF", "Màu nhấn, nút chính, liên kết"],
            ["Xanh lá", "#52C41A", "Trạng thái thành công, hoàn thành"],
            ["Vàng/Cam", "#FAAD14", "Cảnh báo, trạng thái chờ xử lý"],
            ["Đỏ", "#FF4D4F", "Lỗi, trạng thái hủy, xóa"],
            ["Xám nền", "#F5F5F5", "Nền vùng nội dung"],
            ["Trắng", "#FFFFFF", "Nền thẻ (card), bảng"],
        ],
        caption="Bảng màu sử dụng trong giao diện",
        widths=[4.5, 3.5, 7.5],
    )

    R.h3("4.4.3. Typography")
    R.p("Hệ thống sử dụng font chữ mặc định của Ant Design (font hệ thống như -apple-"
        "system, Segoe UI, Roboto) đảm bảo độ rõ ràng và khả năng đọc tốt. Cỡ chữ tiêu "
        "chuẩn cho nội dung là 14px, tiêu đề trang 20–24px, nhãn và chú thích 12px. Các "
        "tiêu đề bảng được in đậm để phân biệt với nội dung dữ liệu.")

    R.h2("4.5. Tổng kết chương")
    R.p("Chương 4 đã trình bày thiết kế chi tiết của hệ thống bao gồm kiến trúc tổng "
        "quan ba lớp, thiết kế cơ sở dữ liệu với sơ đồ ERD và cấu trúc 12 bảng dữ liệu "
        "chính, thiết kế hệ thống REST API theo các nhóm chức năng, và thiết kế giao diện "
        "người dùng. Đây là bản thiết kế làm cơ sở trực tiếp cho việc cài đặt và triển "
        "khai hệ thống ở Chương 5.")

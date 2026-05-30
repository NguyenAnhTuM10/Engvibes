# -*- coding: utf-8 -*-

def build_ch6(R):
    R.h1("CHƯƠNG 6: KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN")

    R.h2("6.1. Kết quả đạt được")

    R.h3("6.1.1. Về mặt chức năng")
    R.p("Đề tài đã xây dựng thành công một hệ thống quản lý cửa hàng vật liệu xây dựng "
        "hoàn chỉnh dưới dạng ứng dụng web, đáp ứng đầy đủ các nhóm chức năng đã đề ra:")
    R.bullet("Hệ thống xác thực và phân quyền theo bốn vai trò (Admin, Quản lý, Nhân "
             "viên bán hàng, Thủ kho) hoạt động chính xác.")
    R.bullet("Quản lý đầy đủ danh mục sản phẩm, đơn vị tính, giá nhập – giá bán.")
    R.bullet("Quản lý đơn hàng bán với tự động tính tiền, kiểm tra và trừ tồn kho.")
    R.bullet("Quản lý thanh toán và theo dõi công nợ khách hàng.")
    R.bullet("Quản lý kho với nghiệp vụ nhập, xuất, kiểm kê và theo dõi tồn thời gian thực.")
    R.bullet("Quản lý khách hàng, nhà cung cấp, nhân viên và người dùng.")
    R.bullet("Hệ thống báo cáo – thống kê trực quan phục vụ ra quyết định.")

    R.h3("6.1.2. Về mặt công nghệ")
    R.p("Về mặt công nghệ, đề tài đã vận dụng và làm chủ một bộ công nghệ hiện đại trong "
        "phát triển ứng dụng web fullstack: ReactJS kết hợp Ant Design cho frontend; "
        "Java Spring Boot, Spring Security với JWT và Spring Data JPA cho backend; MySQL "
        "cho cơ sở dữ liệu. Hệ thống được thiết kế theo kiến trúc ba lớp rõ ràng, giao "
        "tiếp qua REST API, đảm bảo tính module hóa, dễ bảo trì và mở rộng.")

    R.h3("6.1.3. Về mặt học thuật")
    R.p("Quá trình thực hiện đề tài giúp củng cố và vận dụng tổng hợp các kiến thức về "
        "phân tích – thiết kế hệ thống thông tin, thiết kế cơ sở dữ liệu quan hệ, lập "
        "trình hướng đối tượng, bảo mật ứng dụng web và quy trình phát triển phần mềm "
        "hoàn chỉnh từ khâu khảo sát, phân tích, thiết kế đến cài đặt và kiểm thử.")

    R.h2("6.2. Hạn chế")
    R.p("Bên cạnh các kết quả đạt được, đề tài vẫn còn một số hạn chế:")
    R.bullet("Hệ thống mới chỉ được kiểm thử trên dữ liệu mẫu, chưa triển khai và vận "
             "hành thực tế lâu dài tại cửa hàng.")
    R.bullet("Chưa tích hợp các thiết bị phần cứng như máy quét mã vạch, máy in hóa "
             "đơn nhiệt.")
    R.bullet("Chưa hỗ trợ ứng dụng di động và bán hàng trực tuyến cho khách hàng cuối.")
    R.bullet("Báo cáo thống kê còn ở mức cơ bản, chưa có các phân tích dự báo nâng cao.")

    R.h2("6.3. Hướng phát triển")

    R.h3("6.3.1. Ngắn hạn (3-6 tháng)")
    R.bullet("Hoàn thiện chức năng xuất hóa đơn, in phiếu và tích hợp máy quét mã vạch.")
    R.bullet("Bổ sung cảnh báo tồn kho tối thiểu và đề xuất nhập hàng tự động.")
    R.bullet("Triển khai thử nghiệm thực tế tại một cửa hàng và thu thập phản hồi.")

    R.h3("6.3.2. Trung hạn (6-12 tháng)")
    R.bullet("Phát triển ứng dụng di động cho nhân viên bán hàng và giao hàng.")
    R.bullet("Tích hợp cổng thanh toán điện tử và hóa đơn điện tử.")
    R.bullet("Bổ sung phân hệ quản lý chương trình khuyến mãi, tích điểm khách hàng.")

    R.h3("6.3.3. Dài hạn (1-2 năm)")
    R.bullet("Ứng dụng phân tích dữ liệu và học máy để dự báo nhu cầu, tối ưu tồn kho.")
    R.bullet("Mở rộng hỗ trợ chuỗi nhiều cửa hàng/chi nhánh, đồng bộ dữ liệu tập trung.")
    R.bullet("Xây dựng nền tảng thương mại điện tử bán VLXD trực tuyến.")

    R.h2("6.4. Bài học kinh nghiệm")
    R.p("Qua quá trình thực hiện đề tài, người thực hiện rút ra nhiều bài học quý báu: "
        "tầm quan trọng của việc khảo sát và phân tích yêu cầu kỹ lưỡng trước khi lập "
        "trình; lợi ích của việc thiết kế cơ sở dữ liệu và kiến trúc rõ ràng ngay từ "
        "đầu; kỹ năng tổ chức mã nguồn theo module để dễ bảo trì; và kinh nghiệm xử lý "
        "các vấn đề thực tế trong phát triển ứng dụng fullstack như xác thực, phân "
        "quyền, quản lý trạng thái và tối ưu hiệu năng.")

    R.h2("6.5. Lời kết")
    R.p("Đề tài “Xây dựng hệ thống quản lý cửa hàng vật liệu xây dựng” đã được hoàn "
        "thành với đầy đủ các mục tiêu đề ra, tạo ra một sản phẩm phần mềm có giá trị "
        "ứng dụng thực tiễn. Mặc dù vẫn còn một số hạn chế, hệ thống đã thể hiện được "
        "tính khả thi và tiềm năng phát triển. Người thực hiện hy vọng đề tài sẽ là nền "
        "tảng để tiếp tục hoàn thiện và mở rộng trong tương lai, đồng thời là tài liệu "
        "tham khảo hữu ích cho các nghiên cứu tương tự. Một lần nữa, em xin chân thành "
        "cảm ơn quý thầy cô đã hướng dẫn và hỗ trợ trong suốt quá trình thực hiện đồ án.")


def build_appendix(R):
    R.h1("PHỤ LỤC")

    R.h2("Phụ lục A: Cấu trúc source code")

    R.h3("A.1. Frontend (ReactJS)")
    R.code("frontend/\n"
           "├── public/\n"
           "├── src/\n"
           "│   ├── api/            # Cấu hình axios, gọi REST API\n"
           "│   ├── components/     # Component dùng chung\n"
           "│   ├── features/       # Module theo tính năng\n"
           "│   │   ├── auth/\n"
           "│   │   ├── products/\n"
           "│   │   ├── orders/\n"
           "│   │   ├── inventory/\n"
           "│   │   ├── customers/\n"
           "│   │   ├── suppliers/\n"
           "│   │   ├── employees/\n"
           "│   │   ├── users/\n"
           "│   │   └── reports/\n"
           "│   ├── layouts/        # Layout tổng quan (Sidebar, Header)\n"
           "│   ├── routes/         # Định tuyến\n"
           "│   ├── store/          # Quản lý trạng thái\n"
           "│   ├── App.tsx\n"
           "│   └── main.tsx\n"
           "├── package.json\n"
           "└── vite.config.ts")

    R.h3("A.2. Backend (Spring Boot)")
    R.code("backend/\n"
           "├── src/main/java/com/vlxd/\n"
           "│   ├── config/         # SecurityConfig, CorsConfig\n"
           "│   ├── security/       # JwtService, JwtAuthFilter\n"
           "│   ├── common/         # ApiResponse, GlobalExceptionHandler\n"
           "│   ├── auth/           # AuthController, AuthService\n"
           "│   ├── user/           # User, Role, UserController...\n"
           "│   ├── employee/\n"
           "│   ├── product/        # Product, Category...\n"
           "│   ├── customer/\n"
           "│   ├── supplier/\n"
           "│   ├── order/          # Order, OrderItem, Payment\n"
           "│   ├── inventory/      # InventoryTransaction...\n"
           "│   └── report/\n"
           "├── src/main/resources/\n"
           "│   └── application.properties\n"
           "└── pom.xml")

    R.h2("Phụ lục B: Hướng dẫn sử dụng")
    R.h3("B.1. Đăng nhập hệ thống")
    R.p("Truy cập địa chỉ hệ thống, nhập tên đăng nhập và mật khẩu được cấp, nhấn Đăng "
        "nhập. Hệ thống sẽ điều hướng tới trang chính tương ứng với vai trò.")
    R.h3("B.2. Quản lý sản phẩm")
    R.p("Vào menu Sản phẩm → nhấn Thêm sản phẩm → điền thông tin → Lưu. Để sửa/xóa, "
        "chọn thao tác tương ứng trên dòng sản phẩm trong bảng.")
    R.h3("B.3. Tạo đơn hàng")
    R.p("Vào menu Đơn hàng → Tạo đơn → chọn khách hàng → thêm sản phẩm và số lượng → "
        "kiểm tra tổng tiền → Xác nhận.")
    R.h3("B.4. Xử lý đơn hàng")
    R.p("Trong danh sách đơn hàng, chọn đơn cần xử lý để xem chi tiết, cập nhật trạng "
        "thái hoặc hủy đơn.")
    R.h3("B.5. Thanh toán")
    R.p("Mở đơn hàng → chọn Thanh toán → nhập số tiền và phương thức → Xác nhận. Hệ "
        "thống cập nhật công nợ còn lại.")
    R.h3("B.6. Nhập kho")
    R.p("Vào menu Kho → Nhập kho → chọn nhà cung cấp → thêm sản phẩm và số lượng → Xác "
        "nhận. Tồn kho được cập nhật tự động.")
    R.h3("B.7. Xem báo cáo")
    R.p("Vào menu Báo cáo → chọn loại báo cáo và khoảng thời gian → xem bảng và biểu đồ "
        "→ có thể xuất file.")

    R.h2("Phụ lục C: Tài khoản mặc định")
    R.table(
        ["Vai trò", "Tên đăng nhập", "Mật khẩu"],
        [
            ["Quản trị viên", "admin", "admin123"],
            ["Quản lý", "manager", "manager123"],
            ["Nhân viên bán hàng", "sales", "sales123"],
            ["Thủ kho", "warehouse", "warehouse123"],
        ],
        caption="Tài khoản mặc định của hệ thống",
        widths=[5.0, 5.0, 5.0],
    )

    R.h2("Phụ lục D: Trạng thái đơn hàng")
    R.table(
        ["Trạng thái", "Ý nghĩa"],
        [
            ["DRAFT", "Đơn nháp, chưa xác nhận"],
            ["CONFIRMED", "Đã xác nhận, đã trừ tồn kho"],
            ["PAID", "Đã thanh toán đủ"],
            ["PARTIALLY_PAID", "Đã thanh toán một phần (còn công nợ)"],
            ["COMPLETED", "Hoàn thành (đã giao và thanh toán)"],
            ["CANCELLED", "Đã hủy"],
        ],
        caption="Các trạng thái của đơn hàng",
        widths=[5.0, 10.0],
    )

    R.h2("Phụ lục E: Loại giao dịch kho")
    R.table(
        ["Loại", "Ý nghĩa"],
        [
            ["IMPORT", "Nhập kho từ nhà cung cấp (tăng tồn)"],
            ["EXPORT", "Xuất kho (giảm tồn)"],
            ["ADJUST", "Điều chỉnh tồn sau kiểm kê"],
        ],
        caption="Các loại giao dịch kho",
        widths=[5.0, 10.0],
    )

    R.h2("Phụ lục F: Ma trận phân quyền")
    R.p("Chi tiết ma trận phân quyền đã được trình bày tại mục 3.7 của đồ án. Bảng dưới "
        "đây tóm tắt lại quyền truy cập theo vai trò.")
    R.table(
        ["Chức năng", "Admin", "Quản lý", "NV bán hàng", "Thủ kho"],
        [
            ["Quản lý người dùng", "✓", "✗", "✗", "✗"],
            ["Quản lý nhân viên", "✓", "✓", "✗", "✗"],
            ["Quản lý sản phẩm", "✓", "✓", "Xem", "Xem"],
            ["Quản lý đơn hàng & thanh toán", "✓", "✓", "✓", "✗"],
            ["Quản lý kho & nhà cung cấp", "✓", "✓", "✗", "✓"],
            ["Quản lý khách hàng", "✓", "✓", "✓", "✗"],
            ["Báo cáo thống kê", "✓", "✓", "Hạn chế", "✗"],
        ],
        caption="Tóm tắt ma trận phân quyền",
        widths=[5.5, 2.4, 2.4, 2.7, 2.5],
    )


def build_references(R):
    R.h1("TÀI LIỆU THAM KHẢO")
    refs = [
        "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides (1994), Design "
        "Patterns: Elements of Reusable Object-Oriented Software, Addison-Wesley.",
        "Craig Walls (2022), Spring in Action, 6th Edition, Manning Publications.",
        "Pivotal Software, Spring Boot Reference Documentation, "
        "https://docs.spring.io/spring-boot/ (truy cập 2026).",
        "Meta Platforms, React Documentation, https://react.dev/ (truy cập 2026).",
        "Ant Design Team, Ant Design Components, https://ant.design/ (truy cập 2026).",
        "Oracle Corporation, MySQL 8.0 Reference Manual, "
        "https://dev.mysql.com/doc/ (truy cập 2026).",
        "Roy Thomas Fielding (2000), Architectural Styles and the Design of "
        "Network-based Software Architectures, Doctoral dissertation, University of "
        "California, Irvine.",
        "M. Jones, J. Bradley, N. Sakimura (2015), RFC 7519 — JSON Web Token (JWT), "
        "Internet Engineering Task Force (IETF).",
        "Martin Fowler (2002), Patterns of Enterprise Application Architecture, "
        "Addison-Wesley.",
        "Abraham Silberschatz, Henry F. Korth, S. Sudarshan (2019), Database System "
        "Concepts, 7th Edition, McGraw-Hill.",
    ]
    for i, r in enumerate(refs, 1):
        p = R.doc.add_paragraph()
        p.paragraph_format.left_indent = None
        run = p.add_run(f"[{i}] {r}")

# -*- coding: utf-8 -*-
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Pt


def build_front(R):
    d = R.doc
    # ---------------- COVER ----------------
    R.p("TRƯỜNG ĐẠI HỌC ...", bold=True, align="center")
    R.p("KHOA CÔNG NGHỆ THÔNG TIN", bold=True, align="center")
    for _ in range(4):
        R.p("")
    R.p("ĐỒ ÁN TỐT NGHIỆP", bold=True, align="center", size=18)
    for _ in range(1):
        R.p("")
    R.p("XÂY DỰNG HỆ THỐNG QUẢN LÝ CỬA HÀNG", bold=True, align="center", size=20)
    R.p("VẬT LIỆU XÂY DỰNG", bold=True, align="center", size=20)
    for _ in range(3):
        R.p("")
    R.p("Ngành: Công nghệ thông tin", align="center", size=14)
    for _ in range(6):
        R.p("")
    R.p("Sinh viên thực hiện:  .......................................", align="center")
    R.p("Mã sinh viên:           .......................................", align="center")
    R.p("Lớp:                          .......................................", align="center")
    R.p("Giảng viên hướng dẫn: ..................................", align="center")
    for _ in range(3):
        R.p("")
    R.p("Năm 2026", bold=True, align="center")
    R.page_break()

    # ---------------- LỜI CẢM ƠN ----------------
    R.h1_nobreak("LỜI CẢM ƠN")
    R.p("Trong suốt quá trình học tập và thực hiện đồ án tốt nghiệp, em đã nhận được "
        "sự giúp đỡ tận tình của quý thầy cô, gia đình và bạn bè. Em xin gửi lời cảm ơn "
        "chân thành và sâu sắc nhất đến quý thầy cô trong Khoa Công nghệ thông tin đã "
        "trang bị cho em những kiến thức nền tảng quý báu trong suốt những năm học vừa qua.")
    R.p("Đặc biệt, em xin bày tỏ lòng biết ơn sâu sắc đến giảng viên hướng dẫn đã trực "
        "tiếp định hướng, góp ý và đồng hành cùng em trong toàn bộ quá trình thực hiện đề "
        "tài “Xây dựng hệ thống quản lý cửa hàng vật liệu xây dựng”. Những góp ý quý báu "
        "của thầy/cô đã giúp em hoàn thiện cả về mặt kiến thức chuyên môn lẫn kỹ năng "
        "triển khai một sản phẩm phần mềm hoàn chỉnh.")
    R.p("Do thời gian và kiến thức còn hạn chế, đồ án không tránh khỏi những thiếu sót. "
        "Em rất mong nhận được sự đóng góp ý kiến của quý thầy cô để đề tài được hoàn "
        "thiện hơn. Em xin chân thành cảm ơn!")

    # ---------------- LỜI CAM ĐOAN ----------------
    R.h1("LỜI CAM ĐOAN")
    R.p("Em xin cam đoan đồ án tốt nghiệp “Xây dựng hệ thống quản lý cửa hàng vật liệu "
        "xây dựng” là công trình nghiên cứu của riêng em dưới sự hướng dẫn của giảng viên "
        "hướng dẫn. Các số liệu, kết quả trình bày trong đồ án là trung thực, do em tự "
        "thực hiện và chưa từng được công bố trong bất kỳ công trình nào khác. Các tài "
        "liệu tham khảo được trích dẫn đầy đủ và đúng quy định. Em xin chịu hoàn toàn "
        "trách nhiệm về nội dung đồ án của mình.")

    # ---------------- TÓM TẮT ----------------
    R.h1("TÓM TẮT ĐỒ ÁN")
    R.p("Đồ án tập trung nghiên cứu, phân tích và xây dựng một hệ thống phần mềm quản lý "
        "hoạt động kinh doanh cho cửa hàng vật liệu xây dựng (VLXD) quy mô vừa và nhỏ. "
        "Hệ thống được phát triển theo kiến trúc Client-Server ba lớp, với phần giao diện "
        "(frontend) sử dụng thư viện ReactJS kết hợp bộ component Ant Design, phần xử lý "
        "nghiệp vụ (backend) sử dụng nền tảng Java Spring Boot theo mô hình RESTful API, "
        "cơ chế xác thực và phân quyền dựa trên Spring Security và JSON Web Token (JWT), "
        "và cơ sở dữ liệu quan hệ MySQL.")
    R.p("Hệ thống cung cấp đầy đủ các nhóm chức năng cốt lõi của một cửa hàng VLXD: quản "
        "lý danh mục sản phẩm và tồn kho, quản lý đơn hàng bán, quản lý thanh toán, quản "
        "lý nhập – xuất kho, quản lý khách hàng và nhà cung cấp, quản lý nhân viên, phân "
        "quyền người dùng theo vai trò, cùng hệ thống báo cáo – thống kê phục vụ ra quyết "
        "định. Kết quả đạt được là một ứng dụng web hoàn chỉnh, giao diện thân thiện, "
        "đáp ứng các yêu cầu chức năng và phi chức năng đã đề ra, đồng thời được kiểm thử "
        "về chức năng, bảo mật và hiệu năng.")
    R.para_rich([("Từ khóa: ", True, False),
                 ("quản lý cửa hàng, vật liệu xây dựng, ReactJS, Spring Boot, MySQL, "
                  "REST API, quản lý kho, JWT.", False, True)])

    # ---------------- MỤC LỤC ----------------
    R.h1("MỤC LỤC")
    R.toc('TOC \\o "1-3" \\h \\z \\u ')

    # ---------------- DANH MỤC HÌNH ----------------
    R.h1("DANH MỤC HÌNH")
    R.toc('TOC \\h \\z \\c "Hình" ')

    # ---------------- DANH MỤC BẢNG ----------------
    R.h1("DANH MỤC BẢNG")
    R.toc('TOC \\h \\z \\c "Bảng" ')

    # ---------------- DANH MỤC TỪ VIẾT TẮT ----------------
    R.h1("DANH MỤC TỪ VIẾT TẮT")
    R.table(
        ["Từ viết tắt", "Tiếng Anh / Diễn giải", "Ý nghĩa"],
        [
            ["VLXD", "Vật liệu xây dựng", "Lĩnh vực kinh doanh của cửa hàng"],
            ["API", "Application Programming Interface", "Giao diện lập trình ứng dụng"],
            ["REST", "Representational State Transfer", "Kiểu kiến trúc thiết kế API"],
            ["JWT", "JSON Web Token", "Chuẩn token dùng cho xác thực"],
            ["JPA", "Java Persistence API", "Chuẩn ánh xạ đối tượng – quan hệ"],
            ["ORM", "Object Relational Mapping", "Ánh xạ đối tượng – quan hệ"],
            ["MVC", "Model – View – Controller", "Mẫu kiến trúc phần mềm"],
            ["CSDL", "Database", "Cơ sở dữ liệu"],
            ["ERD", "Entity Relationship Diagram", "Sơ đồ thực thể – liên kết"],
            ["UC", "Use Case", "Trường hợp sử dụng"],
            ["UI/UX", "User Interface / User Experience", "Giao diện / Trải nghiệm người dùng"],
            ["CRUD", "Create – Read – Update – Delete", "Bốn thao tác cơ bản với dữ liệu"],
            ["HTTP", "HyperText Transfer Protocol", "Giao thức truyền siêu văn bản"],
            ["DTO", "Data Transfer Object", "Đối tượng truyền dữ liệu"],
            ["SPA", "Single Page Application", "Ứng dụng một trang"],
        ],
        widths=[3.0, 6.5, 6.0],
    )

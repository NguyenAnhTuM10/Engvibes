# -*- coding: utf-8 -*-

def build_ch2(R):
    R.h1("CHƯƠNG 2: CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ")
    R.p("Chương này trình bày các kiến thức nền tảng làm cơ sở cho việc phân tích, thiết "
        "kế và xây dựng hệ thống, bao gồm tổng quan về hệ thống quản lý cửa hàng, các "
        "kiến trúc phần mềm tiêu biểu và các công nghệ frontend, backend, cơ sở dữ liệu "
        "được lựa chọn sử dụng trong đề tài.")

    R.h2("2.1. Tổng quan về hệ thống quản lý cửa hàng")

    R.h3("2.1.1. Khái niệm hệ thống quản lý cửa hàng")
    R.p("Hệ thống quản lý cửa hàng là một hệ thống thông tin được xây dựng nhằm hỗ trợ "
        "việc quản lý toàn bộ các hoạt động kinh doanh của một cửa hàng, bao gồm quản lý "
        "hàng hóa, kho bãi, bán hàng, thu chi, khách hàng, nhà cung cấp và nhân sự. Mục "
        "tiêu của hệ thống là tin học hóa các quy trình nghiệp vụ, giúp lưu trữ dữ liệu "
        "tập trung, xử lý giao dịch nhanh chóng, chính xác và cung cấp thông tin kịp thời "
        "phục vụ công tác điều hành.")
    R.p("Một hệ thống quản lý cửa hàng điển hình thường bao gồm các phân hệ: quản lý danh "
        "mục hàng hóa, quản lý kho, quản lý bán hàng và hóa đơn, quản lý công nợ – thanh "
        "toán, quản lý đối tác (khách hàng, nhà cung cấp), quản lý người dùng – phân "
        "quyền và phân hệ báo cáo – thống kê.")

    R.h3("2.1.2. Đặc điểm của cửa hàng vật liệu xây dựng")
    R.p("So với các loại hình bán lẻ thông thường, cửa hàng VLXD có những đặc điểm "
        "nghiệp vụ riêng biệt cần được hệ thống quản lý đáp ứng:")
    R.bullet("Đa dạng đơn vị tính: cùng một mặt hàng có thể tính theo nhiều đơn vị "
             "(bao, tấn, viên, m², m³, cây, mét…), đòi hỏi quản lý đơn vị tính linh hoạt.")
    R.bullet("Hàng hóa cồng kềnh, khối lượng lớn: việc quản lý vị trí lưu kho và vận "
             "chuyển có vai trò quan trọng.")
    R.bullet("Giá cả biến động: giá thép, xi măng… thay đổi thường xuyên theo thị "
             "trường, hệ thống cần lưu vết lịch sử giá nhập – giá bán.")
    R.bullet("Phát sinh công nợ: giao dịch với khách hàng (đặc biệt là nhà thầu) và "
             "nhà cung cấp thường có công nợ kéo dài, cần theo dõi chặt chẽ.")
    R.bullet("Nghiệp vụ nhập – xuất kho thường xuyên với số lượng lớn, đòi hỏi kiểm "
             "soát tồn kho theo thời gian thực.")

    R.h3("2.1.3. So sánh các giải pháp quản lý VLXD hiện có")
    R.p("Hiện nay trên thị trường có nhiều giải pháp hỗ trợ quản lý bán hàng. Bảng dưới "
        "đây so sánh một số phương án phổ biến với giải pháp mà đề tài hướng tới.")
    R.table(
        ["Tiêu chí", "Excel / Sổ sách", "Phần mềm POS đóng gói", "Hệ thống đề tài xây dựng"],
        [
            ["Chi phí", "Thấp", "Phí thuê bao định kỳ", "Chủ động, tùy biến"],
            ["Tồn kho thời gian thực", "Không", "Có", "Có"],
            ["Tùy biến nghiệp vụ VLXD", "Hạn chế", "Hạn chế", "Cao (thiết kế riêng)"],
            ["Phân quyền theo vai trò", "Không", "Có", "Có (chi tiết)"],
            ["Báo cáo thống kê", "Thủ công", "Có sẵn mẫu", "Tùy biến theo nhu cầu"],
            ["Khả năng mở rộng", "Thấp", "Phụ thuộc nhà cung cấp", "Cao (mã nguồn sở hữu)"],
        ],
        caption="So sánh các giải pháp quản lý cửa hàng VLXD",
        widths=[4.0, 3.5, 4.0, 4.0],
    )
    R.p("Qua so sánh, có thể thấy việc tự xây dựng một hệ thống chuyên biệt mang lại sự "
        "chủ động cao về tính năng, khả năng tùy biến theo đặc thù nghiệp vụ VLXD và "
        "quyền sở hữu mã nguồn, phù hợp với mục tiêu của đề tài.")

    R.h2("2.2. Kiến trúc phần mềm")

    R.h3("2.2.1. Kiến trúc Client-Server")
    R.p("Kiến trúc Client-Server (Khách – Chủ) là mô hình kiến trúc phân tán phổ biến "
        "nhất trong các ứng dụng mạng. Trong mô hình này, hệ thống được chia thành hai "
        "thành phần chính: phía Client (máy khách) gửi yêu cầu, và phía Server (máy chủ) "
        "tiếp nhận, xử lý yêu cầu và trả về kết quả. Hai thành phần giao tiếp với nhau "
        "thông qua một giao thức mạng chuẩn, thường là HTTP/HTTPS.")
    R.p("Trong đề tài này, trình duyệt web chạy ứng dụng ReactJS đóng vai trò Client, "
        "gửi các yêu cầu HTTP đến Server là ứng dụng Spring Boot. Server xử lý logic "
        "nghiệp vụ, truy vấn cơ sở dữ liệu MySQL và trả về dữ liệu dưới dạng JSON. Mô "
        "hình này cho phép tách biệt rõ ràng giao diện và xử lý nghiệp vụ, nhiều client "
        "có thể đồng thời kết nối tới cùng một server.")
    R.figure("Mô hình kiến trúc Client-Server của hệ thống")
    R.p("Ưu điểm của kiến trúc Client-Server bao gồm: quản lý dữ liệu tập trung tại "
        "server giúp đảm bảo tính nhất quán; dễ bảo trì và nâng cấp khi tách biệt thành "
        "phần; khả năng phục vụ nhiều người dùng đồng thời. Nhược điểm là phụ thuộc vào "
        "kết nối mạng và server có thể trở thành điểm nghẽn nếu không được thiết kế tốt.")

    R.h3("2.2.2. Kiến trúc MVC (Model-View-Controller)")
    R.p("MVC là một mẫu kiến trúc phần mềm chia ứng dụng thành ba thành phần có trách "
        "nhiệm tách biệt:")
    R.bullet("Model: đại diện cho dữ liệu và logic nghiệp vụ, chịu trách nhiệm truy xuất "
             "và xử lý dữ liệu (trong đề tài là các Entity, Service tương tác với CSDL).")
    R.bullet("View: chịu trách nhiệm hiển thị dữ liệu cho người dùng (trong đề tài là "
             "các component giao diện ReactJS).")
    R.bullet("Controller: tiếp nhận yêu cầu từ người dùng, điều phối giữa Model và View "
             "(trong đề tài là các REST Controller của Spring Boot).")
    R.p("Việc tách biệt ba thành phần giúp mã nguồn rõ ràng, dễ kiểm thử, dễ bảo trì và "
        "cho phép nhiều lập trình viên làm việc song song trên các thành phần khác nhau. "
        "Spring Boot hỗ trợ mạnh mẽ mô hình MVC thông qua các annotation như @Controller, "
        "@RestController, @Service và @Repository.")
    R.figure("Luồng xử lý trong mô hình MVC")

    R.h3("2.2.3. Kiến trúc 3-Tier (3 lớp)")
    R.p("Kiến trúc ba lớp là một dạng mở rộng của kiến trúc Client-Server, trong đó ứng "
        "dụng được tổ chức thành ba lớp logic độc lập:")
    R.bullet("Lớp trình bày (Presentation Layer): giao diện người dùng, nơi tiếp nhận "
             "thao tác và hiển thị kết quả — ứng dụng ReactJS.")
    R.bullet("Lớp nghiệp vụ (Business Logic Layer): xử lý các quy tắc nghiệp vụ, kiểm "
             "tra ràng buộc, điều phối luồng dữ liệu — các Service trong Spring Boot.")
    R.bullet("Lớp dữ liệu (Data Access Layer): chịu trách nhiệm lưu trữ và truy xuất "
             "dữ liệu — các Repository (Spring Data JPA) và CSDL MySQL.")
    R.p("Việc phân tách rõ ràng ba lớp giúp tăng tính module hóa, mỗi lớp có thể được "
        "phát triển, kiểm thử và thay đổi độc lập mà ít ảnh hưởng đến các lớp khác. Đây "
        "là kiến trúc được áp dụng xuyên suốt trong hệ thống của đề tài.")
    R.figure("Mô hình kiến trúc 3 lớp của hệ thống")

    R.h3("2.2.4. REST API")
    R.p("REST (Representational State Transfer) là một phong cách kiến trúc cho việc "
        "thiết kế các dịch vụ web. Một API tuân theo REST (RESTful API) sử dụng các "
        "phương thức HTTP chuẩn để thao tác trên tài nguyên (resource), trong đó mỗi tài "
        "nguyên được định danh bằng một URL.")
    R.table(
        ["Phương thức HTTP", "Ý nghĩa", "Ví dụ"],
        [
            ["GET", "Lấy dữ liệu (truy vấn)", "GET /api/products"],
            ["POST", "Tạo mới tài nguyên", "POST /api/products"],
            ["PUT", "Cập nhật toàn bộ tài nguyên", "PUT /api/products/{id}"],
            ["PATCH", "Cập nhật một phần", "PATCH /api/orders/{id}/status"],
            ["DELETE", "Xóa tài nguyên", "DELETE /api/products/{id}"],
        ],
        caption="Các phương thức HTTP trong REST API",
        widths=[4.0, 6.0, 5.5],
    )
    R.p("REST API có các đặc điểm chính: phi trạng thái (stateless — mỗi yêu cầu chứa "
        "đầy đủ thông tin để xử lý), giao tiếp qua các định dạng phổ biến (thường là "
        "JSON), và có giao diện đồng nhất. Hệ thống của đề tài sử dụng REST API làm cầu "
        "nối giao tiếp giữa frontend ReactJS và backend Spring Boot, dữ liệu trao đổi "
        "dưới định dạng JSON.")

    R.h2("2.3. Công nghệ Frontend")

    R.h3("2.3.1. ReactJS")
    R.p("ReactJS là một thư viện JavaScript mã nguồn mở do Meta (Facebook) phát triển, "
        "dùng để xây dựng giao diện người dùng, đặc biệt là các ứng dụng một trang "
        "(Single Page Application). React nổi bật với các đặc điểm:")
    R.bullet("Component-based: giao diện được chia thành các component độc lập, có thể "
             "tái sử dụng và kết hợp với nhau.")
    R.bullet("Virtual DOM: React sử dụng DOM ảo để tối ưu việc cập nhật giao diện, chỉ "
             "render lại những phần thay đổi, giúp tăng hiệu năng.")
    R.bullet("Luồng dữ liệu một chiều: dữ liệu truyền từ component cha xuống con qua "
             "props, giúp ứng dụng dễ kiểm soát và gỡ lỗi.")
    R.bullet("Hooks: cho phép quản lý state và vòng đời component trong các function "
             "component (useState, useEffect, useContext…).")
    R.p("Trong đề tài, ReactJS được sử dụng để xây dựng toàn bộ giao diện người dùng, "
        "kết hợp với React Router để điều hướng và các thư viện quản lý trạng thái, gọi "
        "API.")

    R.h3("2.3.2. Ant Design")
    R.p("Ant Design (antd) là một bộ thư viện component UI dành cho React, cung cấp sẵn "
        "hệ thống các thành phần giao diện chất lượng cao, đồng nhất và chuyên nghiệp như "
        "bảng dữ liệu (Table), biểu mẫu (Form), hộp thoại (Modal), thông báo "
        "(Notification), menu, layout… Ant Design giúp rút ngắn đáng kể thời gian phát "
        "triển giao diện, đảm bảo tính nhất quán về thẩm mỹ và trải nghiệm người dùng. "
        "Đặc biệt, component Table và Form của Ant Design rất phù hợp với các ứng dụng "
        "quản lý có nhiều thao tác nhập liệu và hiển thị danh sách.")

    R.h3("2.3.3. Các thư viện hỗ trợ khác")
    R.table(
        ["Thư viện", "Vai trò"],
        [
            ["React Router", "Định tuyến (routing) cho ứng dụng SPA"],
            ["Axios", "Thư viện gọi HTTP request đến backend"],
            ["React Query / Redux", "Quản lý trạng thái và cache dữ liệu server"],
            ["Day.js", "Xử lý, định dạng ngày tháng"],
            ["Recharts / Ant Design Charts", "Vẽ biểu đồ thống kê trong báo cáo"],
            ["Vite", "Công cụ build và dev server cho frontend"],
        ],
        caption="Các thư viện hỗ trợ phía frontend",
        widths=[5.5, 10.0],
    )

    R.h2("2.4. Công nghệ Backend")

    R.h3("2.4.1. Java và Spring Boot")
    R.p("Java là ngôn ngữ lập trình hướng đối tượng phổ biến, có tính ổn định, bảo mật "
        "cao và hệ sinh thái thư viện phong phú, được sử dụng rộng rãi trong phát triển "
        "ứng dụng doanh nghiệp. Spring Boot là một framework xây dựng trên nền Spring "
        "Framework, giúp đơn giản hóa quá trình phát triển ứng dụng Java thông qua cơ chế "
        "tự động cấu hình (auto-configuration), máy chủ nhúng (embedded server) và quản "
        "lý phụ thuộc thông minh.")
    R.p("Các ưu điểm chính của Spring Boot bao gồm:")
    R.bullet("Tự động cấu hình: giảm thiểu cấu hình thủ công nhờ cơ chế convention over "
             "configuration.")
    R.bullet("Máy chủ nhúng (Tomcat): ứng dụng có thể đóng gói thành file JAR và chạy "
             "độc lập mà không cần cài đặt server riêng.")
    R.bullet("Tích hợp dependency injection: quản lý các bean và phụ thuộc một cách tự "
             "động qua IoC Container.")
    R.bullet("Hệ sinh thái phong phú: tích hợp dễ dàng với Spring Security, Spring Data "
             "JPA, Spring Web…")
    R.p("Trong đề tài, Spring Boot đóng vai trò trung tâm xử lý nghiệp vụ, cung cấp các "
        "REST API, quản lý bảo mật và tương tác với cơ sở dữ liệu.")
    R.figure("Các thành phần chính của ứng dụng Spring Boot")

    R.h3("2.4.2. Spring Security và JWT")
    R.p("Spring Security là module chuyên trách về xác thực (authentication) và phân "
        "quyền (authorization) trong hệ sinh thái Spring. Nó cung cấp một chuỗi bộ lọc "
        "(filter chain) để bảo vệ các tài nguyên của ứng dụng, hỗ trợ nhiều cơ chế xác "
        "thực và quản lý quyền truy cập theo vai trò.")
    R.p("JWT (JSON Web Token) là một chuẩn mở để truyền tải thông tin xác thực dưới dạng "
        "một chuỗi token được ký số. Một JWT gồm ba phần: Header (thông tin thuật toán), "
        "Payload (dữ liệu, ví dụ id và vai trò người dùng) và Signature (chữ ký xác thực "
        "tính toàn vẹn). Khi người dùng đăng nhập thành công, server tạo và trả về một "
        "JWT; ở các yêu cầu tiếp theo, client gửi kèm token này trong header Authorization "
        "để server xác thực mà không cần lưu trạng thái phiên (stateless).")
    R.p("Việc kết hợp Spring Security với JWT giúp hệ thống xác thực phi trạng thái, phù "
        "hợp với kiến trúc REST API, đồng thời phân quyền chi tiết theo từng vai trò "
        "(Admin, Quản lý, Nhân viên, Thủ kho).")
    R.figure("Luồng xác thực dựa trên JWT")

    R.h3("2.4.3. Spring Data JPA")
    R.p("Spring Data JPA là một module giúp đơn giản hóa tầng truy cập dữ liệu bằng cách "
        "trừu tượng hóa JPA (Java Persistence API) — chuẩn ánh xạ đối tượng – quan hệ "
        "(ORM) của Java. Với Spring Data JPA, lập trình viên chỉ cần khai báo các "
        "interface Repository kế thừa từ JpaRepository, framework sẽ tự động sinh ra các "
        "câu truy vấn CRUD cơ bản. Ngoài ra có thể định nghĩa truy vấn tùy chỉnh thông "
        "qua quy ước đặt tên phương thức hoặc annotation @Query. Điều này giúp giảm đáng "
        "kể mã nguồn lặp lại và tăng năng suất phát triển.")

    R.h2("2.5. Cơ sở dữ liệu")

    R.h3("2.5.1. MySQL")
    R.p("MySQL là hệ quản trị cơ sở dữ liệu quan hệ (RDBMS) mã nguồn mở phổ biến nhất "
        "hiện nay. MySQL nổi bật với tốc độ xử lý nhanh, độ ổn định cao, dễ sử dụng, hỗ "
        "trợ giao dịch (transaction) với chuẩn ACID khi dùng engine InnoDB, và có cộng "
        "đồng hỗ trợ rộng lớn. MySQL phù hợp với các ứng dụng web vừa và nhỏ như hệ thống "
        "của đề tài, đảm bảo lưu trữ dữ liệu an toàn, nhất quán và truy vấn hiệu quả.")

    R.h3("2.5.2. Thiết kế cơ sở dữ liệu quan hệ")
    R.p("Thiết kế cơ sở dữ liệu quan hệ là quá trình tổ chức dữ liệu thành các bảng có "
        "quan hệ với nhau nhằm giảm thiểu dư thừa và đảm bảo tính toàn vẹn. Quá trình "
        "thiết kế thường tuân theo các dạng chuẩn hóa (Normal Forms):")
    R.bullet("Chuẩn 1 (1NF): mỗi ô chỉ chứa một giá trị nguyên tử, không lặp nhóm.")
    R.bullet("Chuẩn 2 (2NF): thỏa 1NF và mọi thuộc tính không khóa phụ thuộc đầy đủ vào "
             "khóa chính.")
    R.bullet("Chuẩn 3 (3NF): thỏa 2NF và không tồn tại phụ thuộc bắc cầu giữa các thuộc "
             "tính không khóa.")
    R.p("Việc chuẩn hóa giúp tránh các bất thường khi thêm, sửa, xóa dữ liệu. Tuy nhiên "
        "trong một số trường hợp (ví dụ lưu thông tin tổng tiền của đơn hàng tại thời "
        "điểm bán), có thể chấp nhận một mức phi chuẩn hóa hợp lý nhằm tối ưu hiệu năng "
        "truy vấn và lưu vết lịch sử. Hệ thống của đề tài áp dụng chuẩn hóa tới 3NF cho "
        "phần lớn các bảng, kết hợp lưu trữ một số giá trị chốt tại thời điểm giao dịch.")

    R.h2("2.6. Công cụ phát triển")

    R.h3("2.6.1. IDE và Editor")
    R.table(
        ["Công cụ", "Mục đích sử dụng"],
        [
            ["IntelliJ IDEA", "Phát triển backend Java Spring Boot"],
            ["Visual Studio Code", "Phát triển frontend ReactJS"],
            ["MySQL Workbench / DBeaver", "Quản trị và thiết kế cơ sở dữ liệu"],
        ],
        caption="Các IDE và Editor sử dụng",
        widths=[5.5, 10.0],
    )

    R.h3("2.6.2. Công cụ hỗ trợ khác")
    R.table(
        ["Công cụ", "Mục đích sử dụng"],
        [
            ["Git / GitHub", "Quản lý mã nguồn và phiên bản"],
            ["Postman", "Kiểm thử các REST API"],
            ["Maven / Gradle", "Quản lý phụ thuộc và build backend"],
            ["npm / Node.js", "Quản lý phụ thuộc và build frontend"],
            ["Draw.io / StarUML", "Vẽ biểu đồ UML và sơ đồ ERD"],
            ["Swagger / OpenAPI", "Tài liệu hóa API"],
        ],
        caption="Các công cụ hỗ trợ phát triển khác",
        widths=[5.5, 10.0],
    )

    R.h2("2.7. Tổng kết chương")
    R.p("Chương 2 đã trình bày các cơ sở lý thuyết và công nghệ nền tảng cho đề tài, bao "
        "gồm tổng quan về hệ thống quản lý cửa hàng và đặc thù nghiệp vụ VLXD, các kiến "
        "trúc phần mềm (Client-Server, MVC, 3-Tier, REST API) và bộ công nghệ được lựa "
        "chọn: ReactJS + Ant Design cho frontend, Java Spring Boot + Spring Security/JWT "
        "+ Spring Data JPA cho backend, và MySQL cho cơ sở dữ liệu. Đây là tiền đề để "
        "tiến hành phân tích và thiết kế hệ thống ở các chương tiếp theo.")

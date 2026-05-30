# -*- coding: utf-8 -*-

def build_ch1(R):
    R.h1("CHƯƠNG 1: MỞ ĐẦU")

    R.h2("1.1. Đặt vấn đề")

    R.h3("1.1.1. Bối cảnh nghiên cứu")
    R.p("Trong những năm gần đây, ngành xây dựng tại Việt Nam liên tục tăng trưởng cùng "
        "với tốc độ đô thị hóa và nhu cầu nhà ở ngày càng cao. Đi kèm với sự phát triển "
        "đó là sự bùng nổ về số lượng các cửa hàng, đại lý kinh doanh vật liệu xây dựng "
        "(VLXD) như xi măng, sắt thép, gạch, cát, đá, sơn, thiết bị vệ sinh, ống nhựa và "
        "hàng nghìn mặt hàng khác. Đây là một thị trường có giá trị giao dịch lớn nhưng "
        "phần lớn các cửa hàng vẫn đang được vận hành theo phương thức thủ công, dựa trên "
        "sổ sách giấy tờ hoặc các bảng tính Excel rời rạc.")
    R.p("Sự phát triển mạnh mẽ của công nghệ thông tin, đặc biệt là các nền tảng phát "
        "triển ứng dụng web hiện đại, đã mở ra cơ hội để số hóa hoạt động quản lý của các "
        "doanh nghiệp vừa và nhỏ. Việc ứng dụng một hệ thống phần mềm quản lý phù hợp "
        "không chỉ giúp giảm thiểu sai sót, tiết kiệm thời gian, mà còn cung cấp dữ liệu "
        "kịp thời và chính xác phục vụ cho việc ra quyết định kinh doanh. Đây chính là "
        "bối cảnh và động lực để đề tài này được lựa chọn nghiên cứu.")
    R.p("Mặt hàng VLXD có những đặc thù riêng biệt so với hàng hóa tiêu dùng thông "
        "thường: khối lượng và kích thước lớn, đơn vị tính đa dạng (bao, viên, mét, "
        "tấn, m³, cây…), giá cả biến động theo thị trường, và thường phát sinh nghiệp vụ "
        "công nợ với cả khách hàng lẫn nhà cung cấp. Những đặc thù này khiến cho việc "
        "quản lý thủ công trở nên phức tạp và dễ sai sót, đặt ra nhu cầu cấp thiết về một "
        "công cụ quản lý chuyên biệt.")

    R.h3("1.1.2. Thực trạng quản lý tại các cửa hàng VLXD")
    R.p("Qua khảo sát thực tế tại một số cửa hàng VLXD quy mô vừa và nhỏ, có thể nhận "
        "thấy một số hạn chế phổ biến trong công tác quản lý hiện nay:")
    R.bullet("Quản lý hàng hóa bằng sổ sách thủ công hoặc file Excel rời rạc, khó tra "
             "cứu, dễ thất lạc và không đồng bộ giữa các bộ phận.")
    R.bullet("Không nắm bắt được số lượng tồn kho theo thời gian thực, dẫn đến tình "
             "trạng thiếu hàng khi cần bán hoặc tồn đọng vốn do nhập dư.")
    R.bullet("Việc lập đơn hàng, tính tiền và xuất hóa đơn mất nhiều thời gian, dễ nhầm "
             "lẫn về số lượng, đơn giá, đặc biệt với các đơn hàng nhiều mặt hàng.")
    R.bullet("Khó theo dõi công nợ của khách hàng và nhà cung cấp, dẫn tới rủi ro thất "
             "thoát tài chính.")
    R.bullet("Thiếu công cụ báo cáo – thống kê tổng hợp về doanh thu, lợi nhuận, mặt "
             "hàng bán chạy, khiến chủ cửa hàng khó đánh giá hiệu quả kinh doanh.")
    R.bullet("Phân công công việc và phân quyền giữa các nhân viên (bán hàng, thủ kho, "
             "thu ngân, quản lý) không rõ ràng, khó truy vết trách nhiệm khi có sai sót.")
    R.p("Những hạn chế trên cho thấy nhu cầu thực tế về một hệ thống phần mềm có khả "
        "năng tin học hóa toàn bộ quy trình nghiệp vụ của cửa hàng VLXD, từ khâu nhập "
        "hàng, quản lý kho, bán hàng, thanh toán cho đến báo cáo thống kê.")

    R.h3("1.1.3. Sự cần thiết của đề tài")
    R.p("Từ thực trạng nêu trên, việc nghiên cứu và xây dựng một hệ thống quản lý cửa "
        "hàng VLXD là cần thiết và mang tính ứng dụng cao. Hệ thống hướng tới giải quyết "
        "trực tiếp các bài toán mà cửa hàng đang gặp phải: quản lý tập trung dữ liệu, "
        "kiểm soát tồn kho chính xác, tăng tốc độ phục vụ khách hàng, hỗ trợ phân quyền "
        "rõ ràng và cung cấp số liệu báo cáo trực quan.")
    R.p("Bên cạnh giá trị ứng dụng, đề tài còn mang ý nghĩa học thuật khi giúp sinh viên "
        "vận dụng tổng hợp các kiến thức về phân tích – thiết kế hệ thống, lập trình web "
        "fullstack với các công nghệ hiện đại (ReactJS, Spring Boot, MySQL), thiết kế "
        "cơ sở dữ liệu quan hệ, bảo mật ứng dụng và quy trình phát triển phần mềm hoàn "
        "chỉnh từ khâu phân tích yêu cầu đến kiểm thử và triển khai.")

    R.h2("1.2. Mục tiêu và nhiệm vụ nghiên cứu")

    R.h3("1.2.1. Mục tiêu tổng quát")
    R.p("Mục tiêu tổng quát của đề tài là nghiên cứu, phân tích, thiết kế và xây dựng "
        "thành công một hệ thống phần mềm quản lý cửa hàng vật liệu xây dựng dưới dạng "
        "ứng dụng web, giúp tin học hóa và tối ưu hóa các quy trình nghiệp vụ của cửa "
        "hàng, nâng cao hiệu quả quản lý và kinh doanh.")

    R.h3("1.2.2. Mục tiêu cụ thể")
    R.p("Để đạt được mục tiêu tổng quát, đề tài đặt ra các mục tiêu cụ thể sau:")
    R.bullet("Khảo sát và phân tích đầy đủ các quy trình nghiệp vụ của cửa hàng VLXD, "
             "từ đó xác định các yêu cầu chức năng và phi chức năng của hệ thống.")
    R.bullet("Thiết kế kiến trúc hệ thống theo mô hình Client-Server ba lớp, đảm bảo "
             "tính mở rộng, dễ bảo trì và tách biệt rõ ràng các thành phần.")
    R.bullet("Thiết kế cơ sở dữ liệu quan hệ chuẩn hóa, đáp ứng đầy đủ các nghiệp vụ "
             "lưu trữ thông tin sản phẩm, đơn hàng, kho, khách hàng, nhà cung cấp.")
    R.bullet("Xây dựng backend cung cấp hệ thống RESTful API với cơ chế xác thực, phân "
             "quyền an toàn dựa trên Spring Security và JWT.")
    R.bullet("Xây dựng frontend là ứng dụng web một trang (SPA) với giao diện trực quan, "
             "thân thiện bằng ReactJS và Ant Design.")
    R.bullet("Triển khai đầy đủ các phân hệ: quản lý sản phẩm, đơn hàng, thanh toán, "
             "kho, khách hàng, nhà cung cấp, nhân viên, người dùng và báo cáo thống kê.")
    R.bullet("Kiểm thử hệ thống về mặt chức năng, bảo mật và hiệu năng nhằm đảm bảo "
             "chất lượng sản phẩm.")

    R.h3("1.2.3. Nhiệm vụ nghiên cứu")
    R.p("Trên cơ sở các mục tiêu đã đề ra, đề tài xác định các nhiệm vụ nghiên cứu "
        "chính bao gồm:")
    R.num("Nghiên cứu cơ sở lý thuyết về hệ thống quản lý cửa hàng và đặc điểm nghiệp "
          "vụ của lĩnh vực kinh doanh VLXD.")
    R.num("Nghiên cứu các kiến trúc phần mềm (Client-Server, MVC, 3-Tier) và phong cách "
          "thiết kế REST API.")
    R.num("Nghiên cứu và làm chủ các công nghệ: ReactJS, Ant Design, Java Spring Boot, "
          "Spring Security, Spring Data JPA, MySQL.")
    R.num("Phân tích, thiết kế hệ thống bằng các biểu đồ UML (Use Case) và mô hình "
          "dữ liệu (ERD).")
    R.num("Lập trình, tích hợp và kiểm thử hệ thống hoàn chỉnh.")

    R.h2("1.3. Đối tượng và phạm vi nghiên cứu")

    R.h3("1.3.1. Đối tượng nghiên cứu")
    R.p("Đối tượng nghiên cứu của đề tài bao gồm:")
    R.bullet("Quy trình nghiệp vụ quản lý và kinh doanh của cửa hàng vật liệu xây dựng "
             "quy mô vừa và nhỏ.")
    R.bullet("Các công nghệ và nền tảng phát triển ứng dụng web hiện đại: ReactJS, "
             "Spring Boot, MySQL và hệ sinh thái liên quan.")
    R.bullet("Các phương pháp phân tích, thiết kế hệ thống thông tin và thiết kế cơ sở "
             "dữ liệu quan hệ.")

    R.h3("1.3.2. Phạm vi nghiên cứu")
    R.p("Do giới hạn về thời gian và nguồn lực, đề tài giới hạn phạm vi nghiên cứu như "
        "sau:")
    R.para_rich([("Về chức năng: ", True, False),
                 ("Hệ thống tập trung vào các nghiệp vụ quản lý nội bộ của cửa hàng bao "
                  "gồm quản lý sản phẩm, kho, đơn hàng bán, thanh toán, khách hàng, nhà "
                  "cung cấp, nhân viên, người dùng và báo cáo thống kê. Đề tài KHÔNG bao "
                  "gồm các chức năng như bán hàng online cho khách hàng cuối (e-commerce), "
                  "tích hợp cổng thanh toán điện tử, ứng dụng di động hay kết nối thiết bị "
                  "phần cứng chuyên dụng (máy quét mã vạch công nghiệp, cân điện tử).", False, False)])
    R.para_rich([("Về người dùng: ", True, False),
                 ("Hệ thống phục vụ các nhóm người dùng nội bộ gồm Quản trị viên, Quản "
                  "lý, Nhân viên bán hàng và Thủ kho.", False, False)])
    R.para_rich([("Về công nghệ: ", True, False),
                 ("Frontend dùng ReactJS + Ant Design; Backend dùng Java Spring Boot; "
                  "CSDL dùng MySQL; triển khai dưới dạng ứng dụng web chạy trên trình "
                  "duyệt trong môi trường mạng nội bộ của cửa hàng.", False, False)])

    R.h2("1.4. Phương pháp nghiên cứu")

    R.h3("1.4.1. Phương pháp nghiên cứu lý thuyết")
    R.p("Đề tài sử dụng phương pháp nghiên cứu tài liệu nhằm thu thập, tổng hợp và phân "
        "tích các kiến thức liên quan từ giáo trình, tài liệu chuyên ngành, tài liệu kỹ "
        "thuật chính thức của các công nghệ (React, Spring Boot, MySQL) và các bài báo, "
        "công trình nghiên cứu có liên quan. Trên cơ sở đó, hệ thống hóa cơ sở lý thuyết "
        "phục vụ cho việc thiết kế và xây dựng hệ thống.")

    R.h3("1.4.2. Phương pháp nghiên cứu thực tiễn")
    R.p("Đề tài kết hợp các phương pháp thực tiễn sau:")
    R.bullet("Phương pháp khảo sát: quan sát và tìm hiểu quy trình hoạt động thực tế "
             "tại cửa hàng VLXD để xác định yêu cầu.")
    R.bullet("Phương pháp phân tích – thiết kế hệ thống: sử dụng UML và mô hình ERD để "
             "mô hình hóa hệ thống.")
    R.bullet("Phương pháp thực nghiệm: lập trình, tích hợp và chạy thử nghiệm hệ thống "
             "trên dữ liệu mẫu.")
    R.bullet("Phương pháp kiểm thử: kiểm thử chức năng, bảo mật và hiệu năng để đánh "
             "giá chất lượng sản phẩm.")

    R.h2("1.5. Ý nghĩa khoa học và thực tiễn")

    R.h3("1.5.1. Ý nghĩa khoa học")
    R.p("Đề tài góp phần hệ thống hóa quy trình phân tích, thiết kế và xây dựng một ứng "
        "dụng web quản lý theo kiến trúc nhiều lớp với công nghệ hiện đại. Kết quả nghiên "
        "cứu có thể được sử dụng làm tài liệu tham khảo cho các nghiên cứu, dự án tương "
        "tự trong lĩnh vực phát triển phần mềm quản lý doanh nghiệp.")

    R.h3("1.5.2. Ý nghĩa thực tiễn")
    R.p("Về mặt thực tiễn, hệ thống là một sản phẩm phần mềm có khả năng triển khai và "
        "ứng dụng trực tiếp vào hoạt động của các cửa hàng VLXD, giúp tiết kiệm chi phí "
        "vận hành, giảm sai sót, nâng cao năng suất lao động và hỗ trợ ra quyết định kinh "
        "doanh dựa trên dữ liệu.")

    R.h2("1.6. Bố cục đồ án")
    R.p("Nội dung đồ án được tổ chức thành sáu chương như sau:")
    R.bullet("Chương 1 – Mở đầu: Trình bày bối cảnh, lý do chọn đề tài, mục tiêu, "
             "nhiệm vụ, đối tượng, phạm vi và phương pháp nghiên cứu.")
    R.bullet("Chương 2 – Cơ sở lý thuyết và công nghệ: Trình bày các kiến thức nền tảng "
             "về hệ thống quản lý, kiến trúc phần mềm và các công nghệ được sử dụng.")
    R.bullet("Chương 3 – Phân tích hệ thống: Phân tích quy trình nghiệp vụ, xác định "
             "yêu cầu, phân tích actor và đặc tả các use case.")
    R.bullet("Chương 4 – Thiết kế hệ thống: Thiết kế kiến trúc, cơ sở dữ liệu, API và "
             "giao diện người dùng.")
    R.bullet("Chương 5 – Cài đặt và kiểm thử: Trình bày kết quả cài đặt, demo giao diện "
             "và kết quả kiểm thử hệ thống.")
    R.bullet("Chương 6 – Kết luận và hướng phát triển: Tổng kết kết quả đạt được, hạn "
             "chế và đề xuất hướng phát triển trong tương lai.")

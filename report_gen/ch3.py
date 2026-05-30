# -*- coding: utf-8 -*-

def _uc_spec(R, code, name, actor, precond, postcond, main_flow, alt_flow, exc_flow):
    R.table(
        ["Mục", "Nội dung"],
        [
            ["Mã Use Case", code],
            ["Tên Use Case", name],
            ["Tác nhân", actor],
            ["Mô tả", f"Cho phép {actor.lower()} thực hiện chức năng {name.lower()}."],
            ["Điều kiện trước", precond],
            ["Điều kiện sau", postcond],
        ],
        widths=[4.0, 11.5],
    )
    R.p("Luồng sự kiện chính:", bold=True)
    for i, step in enumerate(main_flow, 1):
        R.num(step)
    if alt_flow:
        R.p("Luồng sự kiện thay thế:", bold=True)
        for step in alt_flow:
            R.bullet(step)
    if exc_flow:
        R.p("Luồng ngoại lệ:", bold=True)
        for step in exc_flow:
            R.bullet(step)


def build_ch3(R):
    R.h1("CHƯƠNG 3: PHÂN TÍCH HỆ THỐNG")
    R.p("Chương này tập trung phân tích quy trình nghiệp vụ hiện tại của cửa hàng VLXD, "
        "xác định các yêu cầu chức năng và phi chức năng, phân tích các tác nhân tham gia "
        "hệ thống và đặc tả chi tiết các trường hợp sử dụng (use case) cùng ma trận phân "
        "quyền.")

    R.h2("3.1. Khảo sát hiện trạng")

    R.h3("3.1.1. Quy trình bán hàng hiện tại")
    R.p("Qua khảo sát, quy trình bán hàng hiện tại tại cửa hàng VLXD diễn ra như sau: "
        "Khi khách hàng đến mua, nhân viên bán hàng tư vấn và kiểm tra hàng hóa bằng cách "
        "hỏi thủ kho hoặc kiểm tra trực tiếp tại kho. Sau khi khách chốt đơn, nhân viên "
        "ghi thông tin đơn hàng ra giấy hoặc sổ, tính tổng tiền thủ công, rồi chuyển cho "
        "thu ngân thu tiền. Việc cập nhật tồn kho được thực hiện thủ công vào cuối ngày "
        "hoặc theo định kỳ. Nghiệp vụ nhập hàng từ nhà cung cấp cũng được ghi nhận tương "
        "tự bằng sổ sách.")
    R.figure("Sơ đồ quy trình bán hàng thủ công hiện tại")

    R.h3("3.1.2. Các vấn đề tồn tại")
    R.p("Từ quy trình hiện tại, có thể chỉ ra một số vấn đề chính:")
    R.bullet("Tốc độ xử lý đơn hàng chậm do tính tiền và ghi chép thủ công.")
    R.bullet("Số liệu tồn kho không chính xác và không cập nhật theo thời gian thực.")
    R.bullet("Khó tra cứu lịch sử giao dịch, công nợ của khách hàng và nhà cung cấp.")
    R.bullet("Thiếu báo cáo tổng hợp phục vụ quản lý, điều hành.")
    R.bullet("Dữ liệu phân tán, dễ thất lạc, không có cơ chế phân quyền và bảo mật.")

    R.h2("3.2. Xác định yêu cầu hệ thống")

    R.h3("3.2.1. Yêu cầu chức năng")
    R.p("Hệ thống cần đáp ứng các nhóm yêu cầu chức năng sau:")
    R.table(
        ["Mã", "Nhóm chức năng", "Mô tả"],
        [
            ["FR01", "Xác thực & phân quyền", "Đăng nhập, đăng xuất, phân quyền theo vai trò"],
            ["FR02", "Quản lý sản phẩm", "Thêm/sửa/xóa/tìm kiếm sản phẩm, danh mục, đơn vị tính"],
            ["FR03", "Quản lý đơn hàng", "Tạo, cập nhật, hủy, theo dõi trạng thái đơn hàng"],
            ["FR04", "Quản lý thanh toán", "Ghi nhận thanh toán, theo dõi công nợ khách hàng"],
            ["FR05", "Quản lý kho", "Nhập kho, xuất kho, kiểm kê, theo dõi tồn kho"],
            ["FR06", "Quản lý khách hàng", "Lưu trữ thông tin, lịch sử mua hàng, công nợ"],
            ["FR07", "Quản lý nhà cung cấp", "Lưu trữ thông tin, lịch sử nhập hàng, công nợ"],
            ["FR08", "Quản lý nhân viên", "Quản lý hồ sơ nhân viên, phân công vai trò"],
            ["FR09", "Báo cáo thống kê", "Báo cáo doanh thu, tồn kho, công nợ, hàng bán chạy"],
            ["FR10", "Quản lý người dùng", "Tạo tài khoản, gán vai trò, khóa/mở tài khoản"],
        ],
        caption="Danh sách yêu cầu chức năng",
        widths=[2.0, 5.0, 8.5],
    )

    R.h3("3.2.2. Yêu cầu phi chức năng")
    R.table(
        ["Mã", "Yêu cầu", "Mô tả"],
        [
            ["NFR01", "Hiệu năng", "Thời gian phản hồi trung bình < 2 giây với thao tác thông thường"],
            ["NFR02", "Bảo mật", "Mật khẩu mã hóa (BCrypt), xác thực JWT, phân quyền chặt chẽ"],
            ["NFR03", "Khả dụng", "Giao diện thân thiện, dễ sử dụng, tương thích trình duyệt phổ biến"],
            ["NFR04", "Khả năng mở rộng", "Kiến trúc nhiều lớp, dễ bổ sung tính năng mới"],
            ["NFR05", "Tính toàn vẹn", "Đảm bảo nhất quán dữ liệu qua giao dịch (transaction)"],
            ["NFR06", "Khả năng bảo trì", "Mã nguồn rõ ràng, tổ chức theo module, có tài liệu"],
        ],
        caption="Danh sách yêu cầu phi chức năng",
        widths=[2.0, 4.0, 9.5],
    )

    R.h2("3.3. Phân tích Actor")
    R.p("Hệ thống xác định bốn nhóm tác nhân (actor) chính tham gia sử dụng, mỗi tác "
        "nhân có vai trò và quyền hạn khác nhau:")
    R.table(
        ["Tác nhân", "Mô tả vai trò", "Quyền hạn chính"],
        [
            ["Quản trị viên (Admin)", "Người quản trị hệ thống", "Toàn quyền: quản lý người dùng, phân quyền, cấu hình hệ thống"],
            ["Quản lý (Manager)", "Chủ/quản lý cửa hàng", "Quản lý sản phẩm, kho, nhân viên, xem báo cáo thống kê"],
            ["Nhân viên bán hàng", "Nhân viên trực tiếp bán hàng", "Tạo đơn hàng, quản lý khách hàng, ghi nhận thanh toán"],
            ["Thủ kho", "Nhân viên quản lý kho", "Nhập/xuất kho, kiểm kê, theo dõi tồn kho, quản lý NCC"],
        ],
        caption="Danh sách các tác nhân của hệ thống",
        widths=[4.0, 5.0, 6.5],
    )

    R.h2("3.4. Biểu đồ Use Case và Đặc tả chi tiết")

    R.h3("3.4.1. Use Case tổng quan hệ thống")
    R.p("Biểu đồ use case tổng quan thể hiện toàn bộ các chức năng chính của hệ thống và "
        "mối quan hệ giữa các tác nhân với chức năng tương ứng.")
    R.figure("Biểu đồ Use Case tổng quan hệ thống")
    R.p("Danh sách các use case chính của hệ thống được tổng hợp trong bảng sau:")
    R.table(
        ["Mã UC", "Tên Use Case", "Tác nhân chính"],
        [
            ["UC01", "Đăng nhập hệ thống", "Tất cả người dùng"],
            ["UC02", "Quản lý sản phẩm", "Quản lý"],
            ["UC03", "Quản lý đơn hàng", "Nhân viên bán hàng"],
            ["UC04", "Quản lý thanh toán", "Nhân viên bán hàng"],
            ["UC05", "Quản lý kho", "Thủ kho"],
            ["UC06", "Quản lý khách hàng", "Nhân viên bán hàng"],
            ["UC07", "Quản lý nhà cung cấp", "Thủ kho / Quản lý"],
            ["UC08", "Quản lý nhân viên", "Quản lý"],
            ["UC09", "Báo cáo thống kê", "Quản lý"],
            ["UC10", "Quản lý người dùng", "Quản trị viên"],
        ],
        caption="Danh sách Use Case của hệ thống",
        widths=[2.5, 7.0, 6.0],
    )

    # UC01
    R.h3("3.4.2. Đặc tả Use Case UC01: Đăng nhập hệ thống")
    _uc_spec(
        R, "UC01", "Đăng nhập hệ thống", "Tất cả người dùng",
        "Người dùng đã có tài khoản trong hệ thống.",
        "Người dùng được xác thực và truy cập vào hệ thống theo đúng quyền hạn.",
        ["Người dùng truy cập trang đăng nhập.",
         "Người dùng nhập tên đăng nhập và mật khẩu.",
         "Người dùng nhấn nút Đăng nhập.",
         "Hệ thống kiểm tra thông tin đăng nhập.",
         "Hệ thống tạo JWT và trả về cho client.",
         "Hệ thống điều hướng người dùng vào trang chính theo vai trò."],
        ["Người dùng chọn 'Quên mật khẩu' để liên hệ quản trị viên đặt lại."],
        ["Nếu thông tin không hợp lệ, hệ thống hiển thị thông báo lỗi và yêu cầu nhập lại.",
         "Nếu tài khoản bị khóa, hệ thống thông báo và từ chối đăng nhập."],
    )
    R.figure("Biểu đồ tuần tự chức năng Đăng nhập")

    # UC02
    R.h3("3.4.3. Đặc tả Use Case UC02: Quản lý sản phẩm")
    _uc_spec(
        R, "UC02", "Quản lý sản phẩm", "Quản lý",
        "Người dùng đã đăng nhập với quyền Quản lý.",
        "Thông tin sản phẩm được thêm/cập nhật/xóa thành công trong hệ thống.",
        ["Người dùng chọn menu Quản lý sản phẩm.",
         "Hệ thống hiển thị danh sách sản phẩm với tìm kiếm, lọc, phân trang.",
         "Người dùng chọn thao tác Thêm/Sửa/Xóa sản phẩm.",
         "Người dùng nhập/chỉnh sửa thông tin sản phẩm (tên, danh mục, đơn vị tính, "
         "giá nhập, giá bán, mô tả).",
         "Hệ thống kiểm tra hợp lệ dữ liệu và lưu vào CSDL.",
         "Hệ thống thông báo kết quả và cập nhật lại danh sách."],
        ["Người dùng tìm kiếm sản phẩm theo tên hoặc mã, lọc theo danh mục."],
        ["Nếu dữ liệu không hợp lệ (thiếu trường bắt buộc, giá âm), hệ thống báo lỗi.",
         "Nếu sản phẩm đã phát sinh giao dịch, hệ thống chỉ cho phép ẩn (ngừng kinh "
         "doanh) thay vì xóa cứng."],
    )
    R.figure("Biểu đồ tuần tự chức năng Quản lý sản phẩm")

    # UC03
    R.h3("3.4.4. Đặc tả Use Case UC03: Quản lý đơn hàng")
    _uc_spec(
        R, "UC03", "Quản lý đơn hàng", "Nhân viên bán hàng",
        "Người dùng đã đăng nhập; sản phẩm và khách hàng đã tồn tại trong hệ thống.",
        "Đơn hàng được tạo và lưu trữ, tồn kho được cập nhật tương ứng.",
        ["Người dùng chọn chức năng Tạo đơn hàng.",
         "Người dùng chọn khách hàng (hoặc khách lẻ).",
         "Người dùng thêm các sản phẩm vào đơn, nhập số lượng.",
         "Hệ thống tự động kiểm tra tồn kho và tính thành tiền, tổng tiền.",
         "Người dùng xác nhận đơn hàng.",
         "Hệ thống lưu đơn hàng, trừ tồn kho và cập nhật trạng thái đơn."],
        ["Người dùng có thể áp dụng chiết khấu cho đơn hàng.",
         "Người dùng có thể lưu đơn ở trạng thái nháp để xử lý sau."],
        ["Nếu số lượng đặt vượt quá tồn kho, hệ thống cảnh báo và không cho xác nhận.",
         "Người dùng có thể hủy đơn hàng; hệ thống hoàn trả tồn kho nếu đã trừ."],
    )
    R.figure("Biểu đồ tuần tự chức năng Tạo đơn hàng")

    # UC04
    R.h3("3.4.5. Đặc tả Use Case UC04: Quản lý thanh toán")
    _uc_spec(
        R, "UC04", "Quản lý thanh toán", "Nhân viên bán hàng",
        "Đơn hàng đã được tạo trong hệ thống.",
        "Khoản thanh toán được ghi nhận, công nợ của khách hàng được cập nhật.",
        ["Người dùng chọn đơn hàng cần thanh toán.",
         "Hệ thống hiển thị tổng tiền và số tiền đã thanh toán (nếu có).",
         "Người dùng nhập số tiền khách thanh toán và phương thức (tiền mặt/chuyển khoản).",
         "Hệ thống ghi nhận giao dịch thanh toán.",
         "Hệ thống cập nhật công nợ còn lại và trạng thái thanh toán của đơn hàng."],
        ["Cho phép thanh toán nhiều lần (thanh toán một phần) cho cùng một đơn hàng."],
        ["Nếu số tiền thanh toán lớn hơn công nợ, hệ thống cảnh báo.",
         "Nếu nhập số tiền không hợp lệ, hệ thống báo lỗi."],
    )

    # UC05
    R.h3("3.4.6. Đặc tả Use Case UC05: Quản lý kho")
    _uc_spec(
        R, "UC05", "Quản lý kho", "Thủ kho",
        "Người dùng đã đăng nhập với quyền Thủ kho.",
        "Phiếu nhập/xuất kho được lưu, số lượng tồn kho được cập nhật chính xác.",
        ["Người dùng chọn chức năng Nhập kho hoặc Xuất kho.",
         "Người dùng chọn nhà cung cấp (với nhập kho) và các sản phẩm, số lượng.",
         "Hệ thống tính tổng giá trị phiếu.",
         "Người dùng xác nhận phiếu.",
         "Hệ thống lưu phiếu và cập nhật số lượng tồn kho tương ứng (cộng/trừ)."],
        ["Người dùng thực hiện kiểm kê kho, điều chỉnh số lượng tồn thực tế.",
         "Người dùng xem lịch sử các giao dịch nhập – xuất kho."],
        ["Nếu xuất kho vượt tồn, hệ thống cảnh báo và từ chối.",
         "Nếu dữ liệu phiếu không hợp lệ, hệ thống báo lỗi."],
    )
    R.figure("Biểu đồ tuần tự chức năng Nhập kho")

    # UC06
    R.h3("3.4.7. Đặc tả Use Case UC06 - Quản lý khách hàng")
    _uc_spec(
        R, "UC06", "Quản lý khách hàng", "Nhân viên bán hàng",
        "Người dùng đã đăng nhập.",
        "Thông tin khách hàng được lưu trữ và quản lý trong hệ thống.",
        ["Người dùng chọn menu Quản lý khách hàng.",
         "Hệ thống hiển thị danh sách khách hàng.",
         "Người dùng thêm/sửa/xóa thông tin khách hàng (tên, SĐT, địa chỉ, loại KH).",
         "Hệ thống lưu và cập nhật danh sách.",
         "Người dùng có thể xem lịch sử mua hàng và công nợ của khách hàng."],
        ["Tìm kiếm khách hàng theo tên hoặc số điện thoại."],
        ["Nếu khách hàng còn công nợ, hệ thống không cho phép xóa."],
    )

    # UC07
    R.h3("3.4.8. Đặc tả Use Case UC07 - Quản lý nhà cung cấp")
    _uc_spec(
        R, "UC07", "Quản lý nhà cung cấp", "Thủ kho / Quản lý",
        "Người dùng đã đăng nhập với quyền phù hợp.",
        "Thông tin nhà cung cấp được lưu trữ và quản lý trong hệ thống.",
        ["Người dùng chọn menu Quản lý nhà cung cấp.",
         "Hệ thống hiển thị danh sách nhà cung cấp.",
         "Người dùng thêm/sửa/xóa thông tin nhà cung cấp.",
         "Hệ thống lưu và cập nhật danh sách.",
         "Người dùng xem lịch sử nhập hàng và công nợ phải trả cho nhà cung cấp."],
        ["Tìm kiếm nhà cung cấp theo tên hoặc mã."],
        ["Nếu nhà cung cấp còn công nợ hoặc đã phát sinh giao dịch, không cho xóa cứng."],
    )

    # UC08
    R.h3("3.4.9. Đặc tả Use Case UC08 - Quản lý nhân viên")
    _uc_spec(
        R, "UC08", "Quản lý nhân viên", "Quản lý",
        "Người dùng đã đăng nhập với quyền Quản lý.",
        "Hồ sơ nhân viên được quản lý trong hệ thống.",
        ["Người dùng chọn menu Quản lý nhân viên.",
         "Hệ thống hiển thị danh sách nhân viên.",
         "Người dùng thêm/sửa/xóa hồ sơ nhân viên (họ tên, chức vụ, SĐT, ngày vào làm).",
         "Người dùng gán vai trò cho nhân viên.",
         "Hệ thống lưu và cập nhật danh sách."],
        ["Tìm kiếm, lọc nhân viên theo chức vụ hoặc trạng thái làm việc."],
        ["Nếu nhân viên đang gắn với tài khoản đăng nhập đang hoạt động, cần xử lý "
         "tài khoản trước khi xóa."],
    )

    # UC09
    R.h3("3.4.10. Đặc tả Use Case UC09 - Báo cáo thống kê")
    _uc_spec(
        R, "UC09", "Báo cáo thống kê", "Quản lý",
        "Người dùng đã đăng nhập với quyền Quản lý; hệ thống đã có dữ liệu giao dịch.",
        "Hệ thống hiển thị các báo cáo, biểu đồ thống kê theo yêu cầu.",
        ["Người dùng chọn menu Báo cáo thống kê.",
         "Người dùng chọn loại báo cáo (doanh thu, tồn kho, công nợ, hàng bán chạy).",
         "Người dùng chọn khoảng thời gian và các bộ lọc.",
         "Hệ thống truy vấn, tổng hợp dữ liệu.",
         "Hệ thống hiển thị kết quả dưới dạng bảng và biểu đồ.",
         "Người dùng có thể xuất báo cáo ra file (Excel/PDF)."],
        ["Người dùng so sánh số liệu giữa các kỳ."],
        ["Nếu không có dữ liệu trong khoảng thời gian chọn, hệ thống thông báo phù hợp."],
    )
    R.figure("Biểu đồ tuần tự chức năng Báo cáo thống kê")

    # UC10
    R.h3("3.4.11. Đặc tả Use Case UC10 - Quản lý người dùng")
    _uc_spec(
        R, "UC10", "Quản lý người dùng", "Quản trị viên",
        "Người dùng đã đăng nhập với quyền Quản trị viên.",
        "Tài khoản người dùng và phân quyền được quản lý trong hệ thống.",
        ["Quản trị viên chọn menu Quản lý người dùng.",
         "Hệ thống hiển thị danh sách tài khoản.",
         "Quản trị viên tạo mới/chỉnh sửa tài khoản, gán vai trò.",
         "Quản trị viên có thể khóa/mở khóa hoặc đặt lại mật khẩu tài khoản.",
         "Hệ thống lưu và cập nhật danh sách tài khoản."],
        ["Lọc tài khoản theo vai trò hoặc trạng thái."],
        ["Không cho phép tự khóa tài khoản quản trị đang đăng nhập.",
         "Nếu tên đăng nhập đã tồn tại, hệ thống báo lỗi trùng."],
    )

    R.h2("3.7. Ma trận phân quyền chi tiết")
    R.p("Bảng ma trận phân quyền dưới đây thể hiện quyền truy cập của từng vai trò đối "
        "với các chức năng của hệ thống. Ký hiệu: ✓ — được phép; ✗ — không được phép.")
    R.table(
        ["Chức năng", "Admin", "Quản lý", "NV bán hàng", "Thủ kho"],
        [
            ["Đăng nhập", "✓", "✓", "✓", "✓"],
            ["Quản lý sản phẩm", "✓", "✓", "Xem", "Xem"],
            ["Quản lý đơn hàng", "✓", "✓", "✓", "✗"],
            ["Quản lý thanh toán", "✓", "✓", "✓", "✗"],
            ["Quản lý kho", "✓", "✓", "✗", "✓"],
            ["Quản lý khách hàng", "✓", "✓", "✓", "✗"],
            ["Quản lý nhà cung cấp", "✓", "✓", "✗", "✓"],
            ["Quản lý nhân viên", "✓", "✓", "✗", "✗"],
            ["Báo cáo thống kê", "✓", "✓", "Xem hạn chế", "✗"],
            ["Quản lý người dùng", "✓", "✗", "✗", "✗"],
        ],
        caption="Ma trận phân quyền chi tiết theo vai trò",
        widths=[5.0, 2.5, 2.5, 3.0, 2.5],
    )

    R.h2("3.8. Tổng kết chương")
    R.p("Chương 3 đã khảo sát hiện trạng, xác định các yêu cầu chức năng và phi chức "
        "năng, phân tích bốn nhóm tác nhân và đặc tả chi tiết mười use case chính của hệ "
        "thống cùng ma trận phân quyền. Kết quả phân tích này là cơ sở quan trọng để "
        "tiến hành thiết kế hệ thống ở Chương 4.")

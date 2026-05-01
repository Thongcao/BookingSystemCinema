# BookingSystemCinema - Logic Nghiep Vu & Chuc Nang (Business Logic)

Tai lieu nay mo ta chi tiet ve cac luong xu ly nghiep vu va kien truc he thong cua du an.

---

## 1. Tong Quan Kien Truc (Architecture Overview)
He thong duoc thiet ke theo mo hinh MVC truyen thong ket hop RESTful API cho cac chuc nang bat dong bo (chon ghe).
- **Frontend (UI):** Render bang Spring Boot Thymeleaf, su dung Bootstrap 5 va Fetch API de goi REST.
- **Backend (Core):** Spring Boot voi mo hinh Service - Repository.
- **Database (SQL Server):** Luu tru toan bo du lieu cung (Phim, Lich chieu, User, Ghe, Booking).
- **Cache (Redis):** Quan ly trang thai giu ghe tam thoi (Soft Lock) voi hieu suat cao va co TTL (Time To Live).

---

## 2. Danh Sach Chuc Nang Chinh

### 2.1. Quan Ly Nguoi Dung & Xac Thuc (Auth)
- **Role:** Co 2 quyen chinh la `ADMIN` va `CUSTOMER`.
- **JWT (JSON Web Token):**
  - Khi dang nhap thanh cong, API tra ve mot ma JWT.
  - Ma JWT duoc luu tai `localStorage` cua trinh duyet.
  - Khi thuc hien cac chuc nang can xac thuc (vi du: dat ve), frontend se gui token nay trong header `Authorization: Bearer <token>`.
- **Spring Security:** Cau hinh stateless session, chan cac endpoint `/api/admin/**` voi quyen ADMIN.

### 2.2. Quan Ly Phim & Lich Chieu (Core Data)
- **Movie:** Luu tru thong tin phim (Ten, mo ta, thoi luong, poster).
- **Cinema & Room:** Moi rap (Cinema) co nhieu phong (Room). Moi phong co so ghe (Total Seats) co dinh.
- **Showtime:** Lich chieu lien ket giua 1 bo Phim va 1 Phong chieu vao 1 thoi diem nhat dinh, co gia ve co ban (`basePrice`).

### 2.3. Chuc Nang Chon Ghe (Seat Booking) & Soft Lock
Day la mot trong nhung logic phuc tap nhat he thong de tranh tinh trang 2 nguoi cung mua 1 ghe (Concurrency). He thong su dung **Redis** de xu ly Soft Lock.

- **Soft Lock:** Khi nguoi dung A bam vao mot ghe tren UI, 1 request `HOLD` duoc gui len server.
  - Server kiem tra trong Redis xem ghe nay da bi ai giu chua (Key: `booking:showtime:{id}:seat:{id}`).
  - Neu chua, luu Key vao Redis voi Value la `userId` cua nguoi dung A, va thoi gian ton tai (TTL) la 5 phut.
  - Ghe tren man hinh cua nguoi dung A se chuyen sang trang thai `Selected` (Mau xanh). Ghe nay tren man hinh nguoi dung B se hien thi la `Holding` (Mau vang) va khong the bam vao.
- **Release Lock:** Nguoi dung co the bo chon ghe, hoac neu sau 5 phut nguoi dung khong thanh toan, Redis se tu dong xoa Key (Expire) va ghe se tro lai trang thai Available (San sang).

### 2.4. Dat Ve & Thanh Toan (Checkout Workflow)
Khi nguoi dung bam "Thanh Toan", he thong chay qua luong 11 buoc (nam trong transaction) nhu sau:
1. Kiem tra user, showtime co ton tai khong.
2. Tao Entity `Booking` voi trang thai `HOLDING`.
3. Tinh toan tong tien ve (`Showtime.basePrice` + phu thu loai ghe).
4. **Hard Lock Ghe (Database):** Insert cac ve vao bang `Ticket`. Bang `Ticket` co constraint Unique tren `(showtime_id, seat_id)`. Neu 2 user bam nut thanh toan cung tich tac, database se bat loi `DataIntegrityViolation` -> dam bao khong the double booking tren DB.
5. Kiem tra va tinh tien F&B Items (Bap nuoc). Luu vao `BookingDetail`.
6. Kiem tra va ap dung Voucher (Neu co, tru bot tien).
7. Cap nhat Tong tien cuoi cung vao `Booking`.
8. Su dung **Strategy Pattern** de xu ly thanh toan (Hien tai la `MockPaymentStrategy`).
9. Luu lich su thanh toan (`PaymentTransaction`).
10. Luu `Booking`.
11. **Clear Redis Lock:** Khi DB da insert Ticket (Hard lock thanh cong), he thong chu dong xoa Key giu ghe tam thoi trong Redis de giai phong bo nho.

### 2.5. Don Dep Du Lieu Cu (Scheduler)
- Mot Job `@Scheduled` chay moi phut 1 lan.
- Kiem tra cac `Booking` dang o trang thai `HOLDING` ma qua 15 phut chua duoc chuyen sang `CONFIRMED`.
- Neu phat hien, doi trang thai Booking sang `CANCELLED`, XOA cac `Ticket` lien quan (de giai phong cho ngoi trong DB) va tra lai luot su dung Voucher.

---

## 3. Mo Hinh Du Lieu (ERD Overview)

- `User`: Luu thong tin tai khoan dang nhap.
- `Cinema`, `Room`, `Seat`: Kien truc vat ly cua rap.
- `Movie`, `Showtime`: Lich chieu phim.
- `Item`: San pham F&B (Bap, nuoc).
- `Voucher`: Ma giam gia (Percent, MaxDiscount).
- `Booking`: Luu tong quan hoa don cua mot khach hang.
- `BookingDetail`: Luu chi tiet khach mua bap nuoc gi.
- `Ticket`: Luu the hien cho 1 ve (1 ghe) cu the ma khach da mua, lien ket vao Booking.
- `PaymentTransaction`: Lich su giao dich cua booking do.

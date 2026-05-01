# BookingSystemCinema - He Thong Dat Ve Xem Phim

Du an He thong Dat ve Xem phim (Cinema Booking System) duoc xay dung bang **Spring Boot 3**, **Thymeleaf**, **Redis**, va **SQL Server**. He thong cung cap cac chuc nang cot loi cua mot rap chieu phim nhu quan ly lich chieu, dat ve (chon ghe), thanh toan, ban bap nuoc (F&B) va ap dung ma giam gia (Voucher).

---

## 1. Huong Dan Cai Dat & Chay Du An (HOW TO RUN)

### 1.1. Yeu cau he thong (Prerequisites)
- **Java:** JDK 17 hoac moi hon.
- **Maven:** Phien ban 3.8+ (hoac co the su dung Maven Wrapper `.\mvnw` di kem trong project).
- **Co so du lieu SQL Server:** Dang chay va co the ket noi.
- **Redis:** Dang chay o `localhost:6379` (dung cho tinh nang Soft Lock - giu ghe khi dang chon).

### 1.2. Cau hinh Database & Redis
Mo file `src/main/resources/application.yml` va thay doi cac cau hinh sau neu moi truong cua ban khac mac dinh:

```yaml
spring:
  datasource:
    # URL ket noi SQL Server (Tao san database ten la BookingSystemCinema)
    url: jdbc:sqlserver://localhost:1433;databaseName=BookingSystemCinema;encrypt=true;trustServerCertificate=true
    username: sa
    password: YourPassword123! # Doi lai password SQL Server cua ban
  
  data:
    redis:
      host: localhost
      port: 6379
```

> **Luu y:** Neu SQL Server cua ban chay o cong khac hoac ten DB khac, hay dieu chinh o chuoi URL tren. Database `BookingSystemCinema` phai duoc tao trong truoc khi chay.

### 1.3. Build va Chay du an

Su dung terminal o thu muc goc cua du an, chay lenh:

```bash
# Clean va build du an
.\mvnw clean compile

# Chay ung dung Spring Boot
.\mvnw spring-boot:run
```

Hoac ban co the mo project bang **IntelliJ IDEA** / **Eclipse** va chay class `BookingSystemCinemaApplication.java`.

### 1.4. Du lieu mau (Data Seeding)
Khi ung dung khoi chay lan dau tien, he thong se tu dong tao cau truc bang (nho `ddl-auto: update`) va **tu dong seed du lieu mau** thong qua class `DataInitializer.java`. 

Du lieu duoc tao mac dinh gom:
- 1 Rap chieu phim (Galaxy Cinema Nguyen Du).
- 1 Phong chieu voi 50 ghe (co ghe Normal va VIP).
- 2 Bo phim & Lich chieu.
- F&B Items (Bap, nuoc ngot, combo).
- Vouchers (WELCOME10, SUMMER25).
- **Tai khoan mac dinh:**
  - **Admin:** Username: `admin` / Password: `123`
  - **Customer:** Username: `demo` / Password: `123`

### 1.5. Truy cap ung dung
Sau khi khoi chay thanh cong (Terminal bao `Started BookingSystemCinemaApplication`), mo trinh duyet va truy cap:

- **Trang chu giao dien (Nguoi dung):** http://localhost:8080/
- **Dang nhap:** http://localhost:8080/login

---

## 2. Cong Nghe Su Dung (Tech Stack)
- **Backend:** Java 17, Spring Boot 3.x, Spring Web, Spring Data JPA, Spring Security, JWT (JSON Web Tokens).
- **Frontend:** Thymeleaf, Vanilla JavaScript (Fetch API), Bootstrap 5, FontAwesome.
- **Database & Cache:** Microsoft SQL Server, Redis.
- **Khac:** Lombok, Maven.

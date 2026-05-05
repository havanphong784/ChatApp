# 🚀 Quick Start Guide - Chat App with Protocol Buffers

## Cách Chạy Ứng Dụng

### Terminal 1: Chạy Server
```powershell
cd D:\Code\Java\ChatApp
mvn exec:java@server
```

Bạn sẽ thấy:
```
🚀 Server khởi động trên cổng 4444
📱 Có kết nối mới từ 127.0.0.1
[SERVER] Alice đã kết nối. Tổng: 1
```

### Terminal 2, 3, 4...: Chạy Multiple Clients
```powershell
# Client 1
cd D:\Code\Java\ChatApp
mvn javafx:run

# Client 2 (trong terminal khác)
cd D:\Code\Java\ChatApp
mvn javafx:run

# Client 3 (trong terminal khác)
cd D:\Code\Java\ChatApp
mvn javafx:run
```

## Giao Diện Client

### 1. Màn Hình Kết Nối (lần đầu)
```
┌─────────────────────────────────────┐
│  Trò Chuyện Với Protocol Buffers    │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ Tên người dùng:              │  │
│  │ [     User_123         ]     │  │
│  │                              │  │
│  │ Địa chỉ Server:             │  │
│  │ [    localhost         ]     │  │
│  │                              │  │
│  │ Cổng:                        │  │
│  │ [      4444           ]      │  │
│  │                              │  │
│  │        [KẾT NỐI]             │  │
│  │ Chưa kết nối                 │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

### 2. Màn Hình Trò Chuyện (sau khi kết nối)

```
┌──────────────────────────────────────────────────────┐
│ 👤 Alice                 ✅ Kết nối thành công      │ (Header)
├──────────────────────────┬───────────────────────────┤
│                          │ 👥 Người dùng            │
│  💬 Trò Chuyện           │ ┌─────────────────────┐  │
│ ┌────────────────────┐   │ │ Alice               │  │
│ │ System: Alice đã   │   │ │ Bob                 │  │
│ │ tham gia...        │   │ │ Charlie             │  │
│ │                    │   │ │                     │  │
│ │ Server: Chào      │   │ │                     │  │
│ │ mọi người!         │   │ └─────────────────────┘  │
│ │                    │   │                          │
│ │ Alice: Xin chào!  │   │                          │
│ │                    │   │                          │
│ │ Bob: Hi Alice!    │   │                          │
│ │                    │   │                          │
│ └────────────────────┘   │                          │
├────────────────────┬─────┴───────────────────────────┤
│ [Nhập tin nhắn...] │ [📤 Gửi]                       │ (Input)
└────────────────────┴───────────────────────────────┘
```

## Bước Chi Tiết

### Step 1: Chuẩn Bị
```bash
cd D:\Code\Java\ChatApp
mvn clean compile
```

### Step 2: Khởi Động Server
Mở Terminal 1:
```bash
mvn exec:java@server
```

### Step 3: Khởi Động Client Đầu Tiên
Mở Terminal 2:
```bash
mvn javafx:run
```

Input:
- Tên người dùng: `Alice`
- Server: `localhost`
- Cổng: `4444`
- Nhấp: **Kết Nối**

### Step 4: Khởi Động Thêm Clients
Mở Terminal 3, 4, 5...:
```bash
mvn javafx:run
```

Input:
- Tên người dùng: `Bob`, `Charlie`, etc.
- Server: `localhost`
- Cổng: `4444`

### Step 5: Nhắn Tin
- Gõ tin nhắn vào ô input
- Nhấp "📤 Gửi" hoặc nhấn **Enter**
- Tin nhắn sẽ xuất hiện trên tất cả clients

## Ví Dụ Output

### Server Console:
```
🚀 Server khởi động trên cổng 4444
📱 Có kết nối mới từ 127.0.0.1
[SERVER] Alice đã kết nối. Tổng: 1
📱 Có kết nối mới từ 127.0.0.1
[SERVER] Bob đã kết nối. Tổng: 2
📱 Có kết nối mới từ 127.0.0.1
[SERVER] Charlie đã kết nối. Tổng: 3
[SERVER] Alice đã ngắt kết nối. Tổng: 2
```

### Client (Alice) Console:
```
System: System đã tham gia trò chuyện
System: System đã tham gia trò chuyện
System: Alice đã tham gia trò chuyện
System: Bob đã tham gia trò chuyện
System: Charlie đã tham gia trò chuyện
Alice: Xin chào mọi người!
Bob: Hi Alice!
Charlie: Chào các bạn!
System: Alice đã rời khỏi trò chuyện
```

## Các Lệnh Hữu Ích

### Build dự án:
```bash
mvn clean package
```

### Chạy tests:
```bash
mvn test
```

### View dependencies:
```bash
mvn dependency:tree
```

### Clean cache:
```bash
mvn clean
```

## Lưu Ý Quan Trọng

✅ **Server phải chạy trước** khi client kết nối  
✅ **Port 4444 phải trống** (không bị chiếm dụng)  
✅ **Firewall phải cho phép** kết nối trên port 4444  
✅ **Mỗi client cần username khác nhau** (tùy chọn)  
✅ **Message được broadcast cho tất cả client**  

## Troubleshooting

### ❌ "Connection refused"
```
Giải pháp:
1. Kiểm tra server đã chạy?
2. Kiểm tra port 4444 trống?
3. Kiểm tra firewall?
```

### ❌ "Address already in use"
```
Port 4444 đang bị sử dụng. Giải pháp:
1. Tìm process dùng port 4444
2. Kill process đó
3. Hoặc chọn port khác (sửa Server/Main.java)
```

### ❌ "Proto file not found"
```
Giải pháp:
1. Chạy: mvn clean compile
2. Kiểm tra file src/main/proto/chat.proto tồn tại
```

## Architecture Overview

```
         ┌─────────────┐
         │   Server    │
         │  Port 4444  │
         └──────┬──────┘
                │
        ┌───────┼───────┐
        │       │       │
     ┌──▼───┐ ┌─▼──┐ ┌─▼──┐
     │Alice │ │Bob │ │Charlie
     └──────┘ └────┘ └────┘
     (Client) (Client) (Client)
```

## Điều Gì Xảy Ra Khi Gửi Tin Nhắn?

```
1. Client A: Input "Xin chào"
                    │
                    ▼
2. ChatClient: Tạo ChatMessage (sender: "Alice", content: "Xin chào")
                    │
                    ▼
3. Convert: Message → Protocol Buffers (binary)
                    │
                    ▼
4. Network: Gửi qua socket đến Server (Port 4444)
                    │
                    ▼
5. Server: Nhận tin nhắn → Parse Protocol Buffers
                    │
                    ▼
6. Broadcast: Gửi lại tất cả clients (Alice, Bob, Charlie)
                    │
                    ▼
7. Display: Hiểu thị trên UI của mỗi client
```

## Protocol Buffers Benefits

✅ **Compact Format**: Dữ liệu nhỏ hơn JSON/XML ~50%  
✅ **Type Safety**: Kiểm tra kiểu dữ liệu tự động  
✅ **Fast Serialization**: Nhanh hơn JSON nhiều lần  
✅ **Backward Compatible**: Cập nhật schema dễ dàng  
✅ **Multiple Languages**: Có thể dùng với Python, Go, C++, etc.

## Tiếp Theo

- [ ] Thêm xác thực người dùng
- [ ] Lưu lịch sử chat
- [ ] Thêm private messages
- [ ] Thêm file transfer
- [ ] Encryption cho messages
- [ ] Audio/Video chat

---

**Vui lòng tham khảo README.md để biết thêm chi tiết!**


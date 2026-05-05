# Chat Application với Protocol Buffers

Một ứng dụng trò chuyện client-server sử dụng Protocol Buffers để tối ưu hóa truyền dữ liệu.

## 📋 Yêu cầu

- Java 26+
- Maven 3.8.9+
- Protocol Buffers Compiler (tự động tải về qua Maven)

## 🏗️ Cấu trúc Dự Án

```
ChatApp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── ChatApp/           # Client UI và logic
│   │   │   │   ├── Main.java      # Entry point của client
│   │   │   │   ├── ChatClient.java # Kết nối và nhận gửi tin nhắn
│   │   │   │   ├── ChatAppController.java # Controller UI
│   │   │   │   └── chatapp.fxml   # FXML layout cho client
│   │   │   └── Server/            # Server logic
│   │   │       ├── Main.java      # Entry point của server
│   │   │       ├── ChatServer.java # Quản lý client connections
│   │   │       └── ClientHandler.java # Xử lý từng client
│   │   └── proto/
│   │       └── chat.proto         # Protocol Buffers message definition
│   └── test/
├── pom.xml                        # Maven configuration
└── target/                        # Build output
```

## 🔧 Bước Cài Đặt

### 1. Biên Dịch Dự Án

```bash
cd D:\Code\Java\ChatApp
mvn clean compile
```

Lệnh này sẽ:
- Biên dịch file `.proto` thành các Java classes
- Biên dịch tất cả các Java source files
- Tạo ra các class files trong thư mục `target/classes`

### 2. Xây Dựng JAR (Optional)

```bash
mvn package
```

## 🚀 Chạy Ứng Dụng

### Chạy Server

```bash
cd D:\Code\Java\ChatApp
mvn exec:java@server
```

Hoặc chạy trực tiếp từ IDE:
- Mở `Server/Main.java`
- Chạy main() method

Server sẽ lắng nghe trên cổng `4444` (localhost:4444)

### Chạy Client

```bash
cd D:\Code\Java\ChatApp
mvn javafx:run
```

Hoặc chạy trực tiếp từ IDE:
- Mở `ChatApp/Main.java`
- Chạy main() method

Giao diện sẽ hiển thị:
1. **Màn hình kết nối**: Nhập tên người dùng, địa chỉ server và cổng
2. **Sau khi kết nối**:
   - Khu vực trò chuyện ở giữa
   - Danh sách người dùng ở phía bên phải
   - Thanh nhập tin nhắn ở dưới

## 📱 Hướng Dẫn Sử Dụng

### 1. Khởi động Server
```
mvn exec:java@server
```

### 2. Khởi động Multiple Clients (cùng hoặc khác máy)
```
mvn javafx:run
```

### 3. Kết Nối đến Server
- Tên người dùng: Nhập bất kỳ tên nào
- Địa chỉ Server: `localhost` (nếu cùng máy) hoặc IP address
- Cổng: `4444`
- Nhấp "Kết Nối"

### 4. Gửi và Nhận Tin Nhắn
- Nhập tin nhắn trong ô text ở dưới
- Nhấp "📤 Gửi" hoặc nhấp Enter
- Tin nhắn sẽ được gửi cho tất cả người dùng đang kết nối

## 📦 Protocol Buffers Message Format

### ChatMessage
```protobuf
message ChatMessage {
  enum MessageType {
    JOIN = 0;         # Khi người dùng tham gia
    LEAVE = 1;        # Khi người dùng rời đi
    MESSAGE = 2;      # Tin nhắn từ người dùng
    USER_LIST = 3;    # Danh sách người dùng đang online
  }

  MessageType type = 1;
  string sender = 2;
  string content = 3;
  int64 timestamp = 4;
  repeated string users = 5;    # Danh sách tên người dùng
}
```

## 🔐 Tính Năng

✅ **Protocol Buffers**: Sử dụng protobuf để tối ưu hóa kích thước dữ liệu  
✅ **Multi-client Support**: Hỗ trợ nhiều client kết nối đồng thời  
✅ **Real-time Chat**: Trò chuyện thời gian thực giữa các client  
✅ **User List**: Hiển thị danh sách người dùng online  
✅ **System Messages**: Thông báo khi người dùng tham gia/rời khỏi  
✅ **Timestamp**: Mỗi tin nhắn có dấu thời gian  
✅ **JavaFX UI**: Giao diện người dùng hiện đại với JavaFX  

## 🛠️ Phát Triển Thêm

### Thêm Maven Exec Plugin để chạy Server
Để chạy server dễ dàng, thêm vào `pom.xml`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        <execution>
            <id>server</id>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>Server.Main</mainClass>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## ⚠️ Ghi Chú Quan Trọng

1. **Thread Safety**: Sử dụng `ConcurrentHashMap` và `CopyOnWriteArrayList` cho thread safety
2. **Message Format**: Mỗi tin nhắn được gửi với format: `[size(4 bytes)][message(protobuf)]`
3. **Error Handling**: Client sẽ tự động disconnect nếu kết nối bị gián đoạn
4. **Broadcast**: Tất cả tin nhắn đều được gửi cho tất cả client (broadcast)

## 📝 Ví Dụ Sử Dụng

### Client kết nối:
```
Username: Alice
Server: localhost
Port: 4444
```

### Server log:
```
🚀 Server khởi động trên cổng 4444
📱 Có kết nối mới từ 127.0.0.1
[SERVER] Alice đã kết nối. Tổng: 1
```

### Broadcast message:
```
Alice: Xin chào mọi người!
```

Tất cả client sẽ nhìn thấy tin nhắn này.

## 🐛 Troubleshooting

### 1. "Connection refused"
- Kiểm tra server đã chạy chưa
- Kiểm tra firewall có chặn cổng 4444 không

### 2. "Proto file not found"
- Chạy `mvn clean compile` lại
- Kiểm tra file `src/main/proto/chat.proto` tồn tại

### 3. Import errors
- Chạy `mvn clean` để xóa cache
- Rebuild project

## 📄 License

Dự án này được tạo cho mục đích học tập.

## 👨‍💻 Tác Giả

Phát triển với Protocol Buffers và JavaFX


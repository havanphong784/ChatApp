# 🔵 Protocol Buffers - Auto-Generated Classes

## Overview

When you run `mvn clean compile`, the protobuf compiler automatically generates Java classes from `src/main/proto/chat.proto`.

Location: `target/generated-sources/protobuf/java/ChatApp/proto/`

## Generated Files

### 1. ChatMessage.java (Main Class)
**Status**: ⭐ USED IN PROJECT

The primary message class used throughout the application.

#### Key Methods:
```java
// Creating messages:
ChatMessage.newBuilder()
    .setType(ChatMessage.MessageType.MESSAGE)
    .setSender("Alice")
    .setContent("Hello")
    .setTimestamp(System.currentTimeMillis())
    .addUsers("Bob")
    .addUsers("Charlie")
    .build()

// Serialization:
ChatMessage message = ...;
byte[] data = message.toByteArray();           // To bytes

// Deserialization:
ChatMessage message = ChatMessage.parseFrom(
    com.google.protobuf.ByteString.copyFrom(data)
);

// Getters:
message.getType()           // ChatMessage.MessageType
message.getSender()         // String
message.getContent()        // String
message.getTimestamp()      // long
message.getUsersList()      // List<String>
message.getUsersCount()     // int
```

### 2. ChatMessageOrBuilder.java (Interface)
**Status**: Internal Interface

Generated interface implemented by ChatMessage and Builder.

Used for type-safe access to message fields without knowing if it's a message or builder.

```java
public interface ChatMessageOrBuilder extends 
    com.google.protobuf.MessageOrBuilder {
    
    // Field accessors
    int getTypeValue();
    ChatMessage.MessageType getType();
    String getSender();
    String getContent();
    long getTimestamp();
    java.util.List<String> getUsersList();
    String getUsers(int index);
}
```

### 3. ChatMessageProto.java
**Status**: Registry Class

Contains metadata about the protobuf messages and enums.

```java
// Access descriptor information:
ChatMessageProto.ChatMessageProto.getDescriptor()
ChatMessageProto.getMessageDescriptor("ChatMessage")

// Used internally by protobuf runtime
```

### 4. ServerResponse.java (Bonus)
**Status**: Defined but not used in current version

Reserved for future server response messages.

```java
ServerResponse.newBuilder()
    .setSuccess(true)
    .setMessage("Connection accepted")
    .build()
```

### 5. ServerResponseOrBuilder.java
**Status**: Interface for ServerResponse

Similar to ChatMessageOrBuilder.

## Message Types (Enum)

```java
public enum MessageType implements 
    com.google.protobuf.ProtocolMessageEnum {
    
    JOIN = 0;       // User joining the chat
    LEAVE = 1;      // User leaving the chat
    MESSAGE = 2;    // Regular chat message
    USER_LIST = 3;  // List of connected users
    
    UNRECOGNIZED = -1;  // Unknown type
}
```

## Usage Examples in Project

### Server Sending Join Notification
```java
ChatMessage joinNotification = ChatMessage.newBuilder()
    .setType(ChatMessage.MessageType.MESSAGE)
    .setSender("System")
    .setContent(username + " đã tham gia trò chuyện")
    .setTimestamp(System.currentTimeMillis())
    .build();
server.broadcastMessage(joinNotification);
```

### Client Sending Message
```java
ChatMessage message = ChatMessage.newBuilder()
    .setType(ChatMessage.MessageType.MESSAGE)
    .setSender(username)
    .setContent(content)
    .setTimestamp(System.currentTimeMillis())
    .build();

byte[] data = message.toByteArray();
output.writeInt(data.length);    // 4-byte size prefix
output.write(data);              // Message data
output.flush();
```

### Server Parsing Incoming Message
```java
int messageSize = input.readInt();               // Read size
byte[] messageBuffer = new byte[messageSize];
input.readFully(messageBuffer);                  // Read data

ChatMessage message = ChatMessage.parseFrom(
    com.google.protobuf.ByteString.copyFrom(messageBuffer)
);

if (message.getType() == ChatMessage.MessageType.JOIN) {
    // Handle join
} else if (message.getType() == ChatMessage.MessageType.MESSAGE) {
    // Handle message
}
```

### Client Displaying User List
```java
if (message.getType() == ChatMessage.MessageType.USER_LIST) {
    List<String> users = message.getUsersList();
    userListView.getItems().setAll(users);
}
```

## Compilation Process

```
src/main/proto/chat.proto
        ↓
[protobuf-maven-plugin]
        ↓
target/generated-sources/protobuf/java/ChatApp/proto/
        ↓
ChatMessage.java
ChatMessageOrBuilder.java
ChatMessageProto.java
ServerResponse.java
ServerResponseOrBuilder.java
        ↓
[Java Compiler]
        ↓
target/classes/ChatApp/proto/
        ↓
.class files
```

## Builder Pattern

The protobuf compiler generates an **immutable** ChatMessage class with a **Builder** for construction.

```java
// Builder pattern usage:
ChatMessage.Builder builder = ChatMessage.newBuilder();
builder.setType(ChatMessage.MessageType.MESSAGE);
builder.setSender("Alice");
builder.setContent("Hello");
builder.setTimestamp(System.currentTimeMillis());
builder.addUsers("Bob");
builder.addUsers("Charlie");

ChatMessage message = builder.build();  // Create immutable message

// Message is now immutable:
// message.setContent("New");  // ❌ Compile error!

// Modifying requires new builder:
ChatMessage modified = ChatMessage.newBuilder(message)
    .setContent("Modified")
    .build();
```

## Key Features of Generated Classes

✅ **Type-safe**: Compile-time type checking  
✅ **Immutable**: Once built, cannot be modified  
✅ **Efficient**: Minimal memory overhead  
✅ **Serializable**: toByteArray() and parseFrom()  
✅ **Comparable**: Built-in equals() and hashCode()  
✅ **Debuggable**: toString() with readable format  

## Size Optimization

Protobuf uses variable-length integer encoding:

```
Field Example:
- type: ENUM (0) = 0 bytes
- sender: STRING = ~5 chars = ~10 bytes  
- content: STRING = ~50 chars = ~100 bytes
- timestamp: LONG = 8 bytes
- users: repeated STRING = ~50 bytes

Total Protobuf: ~165 bytes (optimized)
Total JSON: ~300+ bytes (verbose)
```

## Error Handling

```java
try {
    ChatMessage message = ChatMessage.parseFrom(data);
    // Message parsed successfully
} catch (com.google.protobuf.InvalidProtocolBufferException e) {
    // Failed to parse - data is corrupted or wrong type
    e.printStackTrace();
}
```

## Backward Compatibility

Protocol Buffers support schema evolution:

```protobuf
// Original
message ChatMessage {
    string sender = 1;
    string content = 2;
}

// New version (compatible!)
message ChatMessage {
    string sender = 1;
    string content = 2;
    int64 timestamp = 3;  // New field
}

// Old clients can still parse new messages
// New fields are simply ignored if not set
```

## Memory Usage

```java
ChatMessage message = ChatMessage.newBuilder()
    .setType(ChatMessage.MessageType.MESSAGE)
    .setSender("Alice")                    // ~50 bytes
    .setContent("Hello World!")            // ~15 bytes  
    .setTimestamp(System.currentTimeMillis())  // ~8 bytes
    .addUsers("Bob")                       // ~5 bytes
    .addUsers("Charlie")                   // ~8 bytes
    .build();

byte[] data = message.toByteArray();  // ~86 bytes
```

## Regenerating Classes

If you modify `chat.proto`, regenerate:

```bash
mvn clean compile
```

The plugin will automatically:
1. Regenerate all .java files
2. Recompile everything
3. Update the target directory

## Debugging Generated Code

To inspect generated classes:

```bash
# Navigate to generated sources
cd target/generated-sources/protobuf/java/ChatApp/proto

# View the generated code
cat ChatMessage.java | less

# Or use IDE (IntelliJ IDEA):
# Right-click → Open Generated Files
```

## Best Practices

✅ Always use `.newBuilder()` to create messages  
✅ Use `.build()` once all fields are set  
✅ Catch `InvalidProtocolBufferException` when parsing  
✅ Don't modify proto files without versioning  
✅ Keep field numbers stable across versions  
✅ Use optional fields for backward compatibility  
✅ Test serialization/deserialization roundtrips  

## Summary

The generated protobuf classes provide:
- **Type-safe** message definitions
- **Efficient** serialization (50% smaller than JSON)
- **Fast** parsing and building
- **Backward compatible** schema evolution
- **Zero boilerplate** code generation

---

**Generated by**: Protocol Buffers Compiler v4.34.1  
**Java Version**: 26  
**Generation Date**: 2026-05-05


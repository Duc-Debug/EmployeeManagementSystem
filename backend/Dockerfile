# ==========================================
# 1. Build Stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Sao chép mã nguồn và công cụ Maven Wrapper
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# ==========================================
# 2. Runtime Stage (Hardened Non-Root)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Tạo group, user non-root và khởi tạo trước các thư mục dữ liệu runtime
RUN addgroup -S appgroup \
    && adduser -S appuser -G appgroup \
    && mkdir -p /app/uploads/backups /app/uploads/task-attachments \
    && chown -R appuser:appgroup /app

WORKDIR /app

# Sao chép file jar đã build và gán quyền sở hữu trực tiếp cho appuser
COPY --from=builder --chown=appuser:appgroup /app/target/employeemanagement-0.0.1-SNAPSHOT.jar app.jar

# Chuyển quyền thực thi sang user non-root
USER appuser:appgroup

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

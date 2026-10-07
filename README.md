# Để chạy dự án local

## Docker

### For MySQL
```
docker pull mysql:8.4

# Lệnh này phải chạy trên powershell
docker run -d `
  --name mysql-saas-pms `
  -e MYSQL_ROOT_PASSWORD=123456 `
  -e MYSQL_DATABASE=saas_management `
  -p 3306:3306 `
  mysql:8.4
```


# Thay đổi 1 số thuộc tính trong application.properties để cá nhân hóa việc test

```
# ADMIN
acc.user=22a1001d0222@students.hou.edu.vn


# Email của he thong SaaS
spring.mail.username=namhoang.enkarin1211@gmail.com
spring.mail.password=lwqt iikb situ pppc
```

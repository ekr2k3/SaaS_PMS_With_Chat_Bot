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

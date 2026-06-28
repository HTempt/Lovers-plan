#!/bin/bash
# ================================================
# 双人岛 (Lovers Plan) 一键部署脚本
# ================================================
# 用法:
#   chmod +x deploy.sh
#   ./deploy.sh               # 首次部署
#   ./deploy.sh rebuild       # 重新构建并启动（代码更新后）
#   ./deploy.sh restart       # 仅重启容器
#   ./deploy.sh stop          # 停止服务
#   ./deploy.sh logs          # 查看日志
# ================================================

set -e

cd "$(dirname "$0")"

ACTION="${1:-start}"

case "$ACTION" in
  start)
    echo "========================================"
    echo "  双人岛 - 首次部署"
    echo "========================================"
    echo ""
    echo "【步骤 1/3】检查依赖..."
    command -v docker >/dev/null 2>&1 || { echo "❌ 请先安装 Docker: https://docs.docker.com/engine/install/"; exit 1; }
    command -v docker-compose >/dev/null 2>&1 || docker compose version >/dev/null 2>&1 || { echo "❌ 请先安装 Docker Compose"; exit 1; }
    echo "✅ Docker 已安装"
    echo ""
    echo "【步骤 2/3】检查配置文件..."
    if [ ! -f .env ]; then
      echo "⚠️  未找到 .env 文件，正在从模板创建..."
      cp .env.example .env 2>/dev/null || true
      echo "⚠️  请编辑 .env 文件，修改其中的密码和密钥！"
      echo ""
      echo "关键配置项:"
      echo "  MYSQL_ROOT_PASSWORD  - MySQL root 密码"
      echo "  JWT_SECRET           - JWT 签名密钥（建议修改）"
      echo "  WECHAT_APP_ID        - 微信小程序 AppID"
      echo "  WECHAT_APP_SECRET    - 微信小程序 AppSecret"
      echo "  SERVER_HOST          - 服务器公网 IP（当前: $(grep SERVER_HOST .env 2>/dev/null | cut -d= -f2)）"
      echo ""
      read -p "按回车继续部署，或按 Ctrl+C 取消并编辑 .env 文件..."
    fi
    echo "✅ 配置就绪"
    echo ""
    echo "【步骤 3/3】启动所有服务..."
    docker compose up -d --build
    echo ""
    echo "========================================"
    echo "  ✅ 部署完成！"
    echo "========================================"
    echo ""
    echo "服务访问地址:"
    echo "  API 接口:      http://$(grep SERVER_HOST .env | cut -d= -f2):8080"
    echo "  通过 Nginx:    http://$(grep SERVER_HOST .env | cut -d= -f2)"
    echo "  MinIO 控制台:  http://$(grep SERVER_HOST .env | cut -d= -f2):9001"
    echo ""
    echo "查看运行状态: docker compose ps"
    echo "查看日志:      docker compose logs -f"
    ;;

  rebuild)
    echo "🔄 重新构建并启动..."
    docker compose up -d --build
    echo "✅ 已更新"
    ;;

  restart)
    echo "🔄 重启服务..."
    docker compose restart
    echo "✅ 已重启"
    ;;

  stop)
    echo "🛑 停止服务..."
    docker compose down
    echo "✅ 已停止"
    ;;

  logs)
    shift
    docker compose logs -f "$@"
    ;;

  status)
    docker compose ps
    ;;

  *)
    echo "用法: $0 [start|rebuild|restart|stop|logs|status]"
    exit 1
    ;;
esac

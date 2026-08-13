#!/usr/bin/env bash
#
# 로컬 빌드 -> 서버로 WAR 전송 -> Tomcat 재배포
#
# 실행 (Git Bash):  bash deploy-remote.sh
#
set -euo pipefail

# ===== 설정 (환경에 맞게 수정) ============================================
SSH_HOST="maillink2@tech.maillink.co.kr"          # 예: maillink@3.35.xxx.xxx (또는 ~/.ssh/config 별칭)
WAR_NAME="blog.war"                        # mvn 산출물 (pom.xml finalName)
REMOTE_TMP="/home/maillink2/blog.war"       # 서버로 업로드할 임시 경로
TOMCAT_WEBAPPS="/opt/tomcat/webapps"       # Tomcat webapps 경로
DEPLOY_NAME="ROOT"                          # 배포 이름 (루트=ROOT)
TOMCAT_SERVICE="tomcat"                     # systemd 서비스명
# =========================================================================

# 스크립트가 있는 폴더(=프로젝트 루트)로 이동
cd "$(dirname "$0")"

echo "==> [1/3] 로컬 빌드"
mvn clean package

WAR_PATH="target/$WAR_NAME"
if [[ ! -f "$WAR_PATH" ]]; then
    echo "!! 빌드 산출물이 없습니다: $WAR_PATH  (배포 중단, 서버는 그대로)" >&2
    exit 1
fi
echo "==> 빌드 성공: $WAR_PATH"

echo "==> [2/3] 서버로 WAR 전송"
scp "$WAR_PATH" "$SSH_HOST:$REMOTE_TMP"

echo "==> [3/3] 서버에서 재배포 (Tomcat 정지 → 교체 → 시작)"
ssh -t "$SSH_HOST" "
    set -e
    sudo systemctl stop $TOMCAT_SERVICE
    sudo rm -rf $TOMCAT_WEBAPPS/$DEPLOY_NAME $TOMCAT_WEBAPPS/$DEPLOY_NAME.war
    sudo mv $REMOTE_TMP $TOMCAT_WEBAPPS/$DEPLOY_NAME.war
    sudo chown tomcat:tomcat $TOMCAT_WEBAPPS/$DEPLOY_NAME.war
    sudo systemctl start $TOMCAT_SERVICE
    sleep 3
    sudo systemctl --no-pager --lines=0 status $TOMCAT_SERVICE || true
"

echo
echo "==> 배포 완료: https://tech.maillink.co.kr/"
echo "    로그:  ssh $SSH_HOST 'sudo tail -f /opt/tomcat/logs/catalina.out'"

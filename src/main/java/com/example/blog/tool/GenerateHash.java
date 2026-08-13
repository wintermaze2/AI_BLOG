package com.example.blog.tool;

import com.example.blog.util.PasswordUtil;

/**
 * 관리자 비밀번호의 BCrypt 해시를 생성하는 CLI 도구.
 *
 * 실행:
 *   mvn -q compile exec:java \
 *       -Dexec.mainClass=com.example.blog.tool.GenerateHash \
 *       -Dexec.args="원하는_비밀번호"
 *
 * 출력된 해시를 admin_user 테이블에 INSERT 하세요:
 *   INSERT INTO admin_user (username, password_hash) VALUES ('admin', '<출력된_해시>');
 */
public class GenerateHash {
    public static void main(String[] args) {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("사용법: GenerateHash <비밀번호>");
            System.exit(1);
        }
        String hash = PasswordUtil.hash(args[0]);
        System.out.println(hash);
    }
}

package olympus.hephaestus.login.register.repository;

import olympus.hephaestus.login.register.domain.EmailVerificationCodeEntity;
import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailVerificationCodeRepository extends BaseAbstractRepository<EmailVerificationCodeEntity, Long> {

    @Select("""
            SELECT id,
                   email,
                   scene,
                   code_hash AS codeHash,
                   expire_at AS expireAt,
                   used_at AS usedAt,
                   send_ip AS sendIp,
                   created_at AS createdAt
            FROM sys_email_verification_code
            WHERE email = #{email}
              AND scene = #{scene}
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    EmailVerificationCodeEntity findLatest(@Param("email") String email, @Param("scene") String scene);

    @Update("""
            UPDATE sys_email_verification_code
            SET used_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND used_at IS NULL
            """)
    int markUsed(@Param("id") Long id);
}

package com.example.springaidemo.login.log.repository;

import com.example.springaidemo.login.log.domain.LoginLogEntity;
import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogRepository extends BaseAbstractRepository<LoginLogEntity, Long> {

    @Insert("""
            INSERT INTO sys_login_log (
                operation_type,
                username,
                person_id,
                person_name,
                session_id,
                success_flag,
                message,
                client_ip,
                user_agent,
                request_uri,
                created_at
            ) VALUES (
                #{operationType},
                #{username},
                #{personId},
                #{personName},
                #{sessionId},
                #{successFlag},
                #{message},
                #{clientIp},
                #{userAgent},
                #{requestUri},
                CURRENT_TIMESTAMP
            )
            """)
    void insertLog(LoginLogEntity entity);

    @Select("""
            <script>
            SELECT id,
                   operation_type AS operationType,
                   username,
                   person_id AS personId,
                   person_name AS personName,
                   session_id AS sessionId,
                   success_flag AS successFlag,
                   message,
                   client_ip AS clientIp,
                   user_agent AS userAgent,
                   request_uri AS requestUri,
                   created_at AS createdAt
            FROM sys_login_log
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        username LIKE CONCAT('%', #{keyword}, '%')
                        OR person_name LIKE CONCAT('%', #{keyword}, '%')
                        OR client_ip LIKE CONCAT('%', #{keyword}, '%')
                        OR message LIKE CONCAT('%', #{keyword}, '%')
                    )
                </if>
                <if test="operationType != null and operationType != ''">
                    AND operation_type = #{operationType}
                </if>
                <if test="success != null">
                    AND success_flag = #{success}
                </if>
                <if test="startTime != null">
                    AND created_at &gt;= #{startTime}
                </if>
                <if test="endTime != null">
                    AND created_at &lt;= #{endTime}
                </if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<LoginLogEntity> query(@Param("keyword") String keyword,
                               @Param("operationType") String operationType,
                               @Param("success") Boolean success,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM sys_login_log
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        username LIKE CONCAT('%', #{keyword}, '%')
                        OR person_name LIKE CONCAT('%', #{keyword}, '%')
                        OR client_ip LIKE CONCAT('%', #{keyword}, '%')
                        OR message LIKE CONCAT('%', #{keyword}, '%')
                    )
                </if>
                <if test="operationType != null and operationType != ''">
                    AND operation_type = #{operationType}
                </if>
                <if test="success != null">
                    AND success_flag = #{success}
                </if>
                <if test="startTime != null">
                    AND created_at &gt;= #{startTime}
                </if>
                <if test="endTime != null">
                    AND created_at &lt;= #{endTime}
                </if>
            </where>
            </script>
            """)
    long count(@Param("keyword") String keyword,
               @Param("operationType") String operationType,
               @Param("success") Boolean success,
               @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime);

    @Delete("DELETE FROM sys_login_log WHERE created_at < #{cutoffTime}")
    int deleteBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}

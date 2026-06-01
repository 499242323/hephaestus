package com.example.springaidemo.login.log.repository;

import com.example.springaidemo.login.log.domain.OperationLogEntity;
import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogRepository extends BaseAbstractRepository<OperationLogEntity, Long> {

    @Insert("""
            INSERT INTO heph_operation_log (
                operator_person_id,
                operator_name,
                operator_username,
                operator_unit_id,
                operator_unit_name,
                module_code,
                module_name,
                action_code,
                action_name,
                target_type,
                target_id,
                target_name,
                success_flag,
                summary,
                detail,
                client_ip,
                user_agent,
                request_uri,
                request_method,
                created_at
            ) VALUES (
                #{operatorPersonId},
                #{operatorName},
                #{operatorUsername},
                #{operatorUnitId},
                #{operatorUnitName},
                #{moduleCode},
                #{moduleName},
                #{actionCode},
                #{actionName},
                #{targetType},
                #{targetId},
                #{targetName},
                #{successFlag},
                #{summary},
                #{detail},
                #{clientIp},
                #{userAgent},
                #{requestUri},
                #{requestMethod},
                CURRENT_TIMESTAMP
            )
            """)
    void insertLog(OperationLogEntity entity);

    @Select("""
            <script>
            SELECT id,
                   operator_person_id AS operatorPersonId,
                   operator_name AS operatorName,
                   operator_username AS operatorUsername,
                   operator_unit_id AS operatorUnitId,
                   operator_unit_name AS operatorUnitName,
                   module_code AS moduleCode,
                   module_name AS moduleName,
                   action_code AS actionCode,
                   action_name AS actionName,
                   target_type AS targetType,
                   target_id AS targetId,
                   target_name AS targetName,
                   success_flag AS successFlag,
                   summary,
                   detail,
                   client_ip AS clientIp,
                   user_agent AS userAgent,
                   request_uri AS requestUri,
                   request_method AS requestMethod,
                   created_at AS createdAt
              FROM heph_operation_log
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        operator_name LIKE CONCAT('%', #{keyword}, '%')
                        OR operator_username LIKE CONCAT('%', #{keyword}, '%')
                        OR operator_unit_name LIKE CONCAT('%', #{keyword}, '%')
                        OR module_name LIKE CONCAT('%', #{keyword}, '%')
                        OR action_name LIKE CONCAT('%', #{keyword}, '%')
                        OR target_id LIKE CONCAT('%', #{keyword}, '%')
                        OR target_name LIKE CONCAT('%', #{keyword}, '%')
                        OR summary LIKE CONCAT('%', #{keyword}, '%')
                        OR detail LIKE CONCAT('%', #{keyword}, '%')
                        OR client_ip LIKE CONCAT('%', #{keyword}, '%')
                        OR request_uri LIKE CONCAT('%', #{keyword}, '%')
                    )
                </if>
                <if test="moduleCode != null and moduleCode != ''">
                    AND module_code = #{moduleCode}
                </if>
                <if test="actionCode != null and actionCode != ''">
                    AND action_code = #{actionCode}
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
    List<OperationLogEntity> query(@Param("keyword") String keyword,
                                   @Param("moduleCode") String moduleCode,
                                   @Param("actionCode") String actionCode,
                                   @Param("success") Boolean success,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("limit") int limit,
                                   @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(1)
              FROM heph_operation_log
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        operator_name LIKE CONCAT('%', #{keyword}, '%')
                        OR operator_username LIKE CONCAT('%', #{keyword}, '%')
                        OR operator_unit_name LIKE CONCAT('%', #{keyword}, '%')
                        OR module_name LIKE CONCAT('%', #{keyword}, '%')
                        OR action_name LIKE CONCAT('%', #{keyword}, '%')
                        OR target_id LIKE CONCAT('%', #{keyword}, '%')
                        OR target_name LIKE CONCAT('%', #{keyword}, '%')
                        OR summary LIKE CONCAT('%', #{keyword}, '%')
                        OR detail LIKE CONCAT('%', #{keyword}, '%')
                        OR client_ip LIKE CONCAT('%', #{keyword}, '%')
                        OR request_uri LIKE CONCAT('%', #{keyword}, '%')
                    )
                </if>
                <if test="moduleCode != null and moduleCode != ''">
                    AND module_code = #{moduleCode}
                </if>
                <if test="actionCode != null and actionCode != ''">
                    AND action_code = #{actionCode}
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
               @Param("moduleCode") String moduleCode,
               @Param("actionCode") String actionCode,
               @Param("success") Boolean success,
               @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime);
}

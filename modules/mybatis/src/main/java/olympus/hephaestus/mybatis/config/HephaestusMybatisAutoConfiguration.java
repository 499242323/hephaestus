package olympus.hephaestus.mybatis.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
@MapperScan(
        basePackages = "olympus.hephaestus",
        annotationClass = Mapper.class
)
public class HephaestusMybatisAutoConfiguration {
}

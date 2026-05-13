package com.raf.framework.datasource.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 强制走主库注解。
 *
 * <p>当启用读写分离自动路由（{@code raf.datasource.read-write-splitting.enabled=true}）后，
 * 框架会自动将 SELECT 路由到从库，INSERT/UPDATE/DELETE 路由到主库。
 *
 * <p>在以下场景需要强制走主库时，在类或方法上标注此注解：
 * <ul>
 *   <li>读己写场景：刚写入后立即查询，需要读到最新数据</li>
 *   <li>强一致性查询：对数据实时性要求极高的场景</li>
 *   <li>分布式事务中的查询操作</li>
 * </ul>
 *
 * <p>注意：事务中的所有操作默认强制走主库，无需此注解。
 *
 * @author Jerry
 * @see com.raf.framework.datasource.DsSelector
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ForceMaster {
}

package com.aidijing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author : 披荆斩棘
 * @date : 2017/9/8
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties( prefix = "aidijing.transactional" )
public class TransactionalConfig {


    private static final String   CUSTOMIZE_TRANSACTION_INTERCEPTOR_NAME               = "customizeTransactionInterceptor";
    private static final String   DATA_SOURCE_SWITCH_METHOD_INTERCEPTOR_NAME           = "dataSourceSwitchMethodInterceptor";
    /**
     * 默认只对 "*Service" , "*ServiceImpl" Bean 进行事务处理,"*"表示模糊匹配, 比如 : userService,orderServiceImpl
     */
    private static final String[] DEFAULT_TRANSACTION_BEAN_NAMES                       = { "*Service" , "*ServiceImpl" };
    /**
     * 可传播事务配置
     */
    private static final String[] DEFAULT_REQUIRED_METHOD_RULE_TRANSACTION_ATTRIBUTES  = {
            "add*" ,
            "save*" ,
            "insert*" ,
            "delete*" ,
            "update*" ,
            "edit*" ,
            "batch*" ,
            "create*" ,
            "remove*" ,
    };
    /**
     * 默认的只读事务
     */
    private static final String[] DEFAULT_READ_ONLY_METHOD_RULE_TRANSACTION_ATTRIBUTES = {
            "get*" ,
            "count*" ,
            "find*" ,
            "query*" ,
            "select*" ,
            "list*" ,
            "*" ,
    };
    /**
     * 自定义事务 BeanName 拦截
     */
    private              String[] customizeTransactionBeanNames                        = {};
    /**
     * 自定义方法名的事务属性相关联,可以使用通配符(*)字符关联相同的事务属性的设置方法; 只读事务
     */
    private              String[] customizeReadOnlyMethodRuleTransactionAttributes     = {};
    /**
     * 自定义方法名的事务属性相关联,可以使用通配符(*)字符关联相同的事务属性的设置方法;
     * 传播事务(默认的){@link org.springframework.transaction.annotation.Propagation#REQUIRED}
     */
    private              String[] customizeRequiredMethodRuleTransactionAttributes     = {};


    /**
     * 配置事务拦截器
     *
     * @param transactionManager : 事务管理器
     * @return
     */
    @Bean( CUSTOMIZE_TRANSACTION_INTERCEPTOR_NAME )
    public TransactionInterceptor customizeTransactionInterceptor ( PlatformTransactionManager transactionManager ) {
        NameMatchTransactionAttributeSource transactionAttributeSource = new NameMatchTransactionAttributeSource();
        RuleBasedTransactionAttribute       readOnly                   = this.readOnlyTransactionRule();
        RuleBasedTransactionAttribute       required                   = this.requiredTransactionRule();
        // 默认的只读事务配置
        for ( String methodName : DEFAULT_READ_ONLY_METHOD_RULE_TRANSACTION_ATTRIBUTES ) {
            transactionAttributeSource.addTransactionalMethod( methodName , readOnly );
        }
        // 默认的传播事务配置
        for ( String methodName : DEFAULT_REQUIRED_METHOD_RULE_TRANSACTION_ATTRIBUTES ) {
            transactionAttributeSource.addTransactionalMethod( methodName , required );
        }
        // 定制的只读事务配置
        for ( String methodName : customizeReadOnlyMethodRuleTransactionAttributes ) {
            transactionAttributeSource.addTransactionalMethod( methodName , readOnly );
        }
        // 定制的传播事务配置
        for ( String methodName : customizeRequiredMethodRuleTransactionAttributes ) {
            transactionAttributeSource.addTransactionalMethod( methodName , required );
        }
        return new TransactionInterceptor( transactionManager , transactionAttributeSource );
    }


    /**
     * 配置事务 AOP 切入点
     * {@link DataSourceSwitch}
     *
     * @param transactionInterceptor : {@link #customizeTransactionInterceptor(PlatformTransactionManager)}

     @Bean public AspectJExpressionPointcutAdvisor aspectJExpressionPointcutAdvisor ( @Qualifier( "customizeTransactionInterceptor" ) TransactionInterceptor transactionInterceptor ) {
     AspectJExpressionPointcutAdvisor pointcut = new AspectJExpressionPointcutAdvisor();
     pointcut.setAdvice( transactionInterceptor );
     pointcut.setExpression( "execution (* com.aidijing.*.*service.*.*(..))" );
     return pointcut;
     }
     */


    /**
     * {@link #dataSourceSwitchMethodInterceptor()}
     * {@link #customizeTransactionInterceptor(PlatformTransactionManager)}
     *
     * @return
     */
    @Bean
    public BeanNameAutoProxyCreator beanNameAutoProxyCreator () {
        BeanNameAutoProxyCreator beanNameAutoProxyCreator = new BeanNameAutoProxyCreator();
        beanNameAutoProxyCreator.setInterceptorNames( DATA_SOURCE_SWITCH_METHOD_INTERCEPTOR_NAME ,
                                                      CUSTOMIZE_TRANSACTION_INTERCEPTOR_NAME );

        List< String > transactionBeanNames = new ArrayList<>( DEFAULT_TRANSACTION_BEAN_NAMES.length + customizeTransactionBeanNames.length );
        // 默认
        transactionBeanNames.addAll( Arrays.asList( DEFAULT_TRANSACTION_BEAN_NAMES ) );
        // 定制
        transactionBeanNames.addAll( Arrays.asList( customizeTransactionBeanNames ) );
        // 归集
        for ( String transactionBeanName : transactionBeanNames ) {
            beanNameAutoProxyCreator.setBeanNames( transactionBeanName );
        }
        beanNameAutoProxyCreator.setProxyTargetClass( true );
        return beanNameAutoProxyCreator;
    }

    @Bean( DATA_SOURCE_SWITCH_METHOD_INTERCEPTOR_NAME )
    public DataSourceSwitchMethodInterceptor dataSourceSwitchMethodInterceptor () {
        return new DataSourceSwitchMethodInterceptor();
    }


    /**
     * 支持当前事务;如果不存在创建一个新的
     */
    private RuleBasedTransactionAttribute requiredTransactionRule () {
        RuleBasedTransactionAttribute required = new RuleBasedTransactionAttribute();
        required.setRollbackRules( Collections.singletonList( new RollbackRuleAttribute( Exception.class ) ) );
        required.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRED );
        required.setTimeout( TransactionDefinition.TIMEOUT_DEFAULT );
        return required;
    }

    /**
     * 只读事务
     */
    private RuleBasedTransactionAttribute readOnlyTransactionRule () {
        RuleBasedTransactionAttribute readOnly = new RuleBasedTransactionAttribute();
        readOnly.setReadOnly( true );
        readOnly.setPropagationBehavior( TransactionDefinition.PROPAGATION_NOT_SUPPORTED );
        return readOnly;
    }


}

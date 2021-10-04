package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;


public class IsCacheEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean cacheEnabled = LapisMain.globalConfig.getCacheEnabled();
        return cacheEnabled != null && cacheEnabled;
    }
}

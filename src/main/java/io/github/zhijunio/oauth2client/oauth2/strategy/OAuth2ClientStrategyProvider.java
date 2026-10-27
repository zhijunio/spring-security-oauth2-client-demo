package io.github.zhijunio.oauth2client.oauth2.strategy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ClientStrategyProvider {

    private final Map<String, OAuth2ClientStrategy> strategies;

    public OAuth2ClientStrategyProvider(List<OAuth2ClientStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(OAuth2ClientStrategy::type, Function.identity()));
    }

    public OAuth2ClientStrategy getStrategyByType(String type) {
        OAuth2ClientStrategy strategy = strategies.get(type == null ? null : type.toUpperCase(java.util.Locale.ROOT));
        if (strategy == null) throw new IllegalArgumentException("Unsupported OAuth2 identity mapping type: " + type);
        return strategy;
    }
}

package com.medchain.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration.
 * AI features use GeminiClient (direct REST API) instead of Spring AI ChatClient.
 * This avoids requiring Google Cloud ADC credentials for local development.
 */
@Configuration
public class SpringAIConfig {
    // No beans needed - GeminiClient handles AI calls directly via REST API
}

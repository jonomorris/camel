/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.webhook;

import javax.annotation.processing.Generated;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ExtendedPropertyConfigurerGetter;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.spi.ConfigurerStrategy;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.support.component.PropertyConfigurerSupport;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointSchemaGeneratorMojo")
@SuppressWarnings("unchecked")
public class WebhookComponentConfigurer extends PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    private org.apache.camel.component.webhook.WebhookConfiguration getOrCreateConfiguration(WebhookComponent target) {
        if (target.getConfiguration() == null) {
            target.setConfiguration(new org.apache.camel.component.webhook.WebhookConfiguration());
        }
        return target.getConfiguration();
    }

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        WebhookComponent target = (WebhookComponent) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "autowiredenabled":
        case "autowiredEnabled": target.setAutowiredEnabled(property(camelContext, boolean.class, value)); return true;
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": target.setBridgeErrorHandler(property(camelContext, boolean.class, value)); return true;
        case "configuration": target.setConfiguration(property(camelContext, org.apache.camel.component.webhook.WebhookConfiguration.class, value)); return true;
        case "webhookautoregister":
        case "webhookAutoRegister": getOrCreateConfiguration(target).setWebhookAutoRegister(property(camelContext, boolean.class, value)); return true;
        case "webhookbasepath":
        case "webhookBasePath": getOrCreateConfiguration(target).setWebhookBasePath(property(camelContext, java.lang.String.class, value)); return true;
        case "webhookcomponentname":
        case "webhookComponentName": getOrCreateConfiguration(target).setWebhookComponentName(property(camelContext, java.lang.String.class, value)); return true;
        case "webhookexternalurl":
        case "webhookExternalUrl": getOrCreateConfiguration(target).setWebhookExternalUrl(property(camelContext, java.lang.String.class, value)); return true;
        case "webhookpath":
        case "webhookPath": getOrCreateConfiguration(target).setWebhookPath(property(camelContext, java.lang.String.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Class<?> getOptionType(String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "autowiredenabled":
        case "autowiredEnabled": return boolean.class;
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return boolean.class;
        case "configuration": return org.apache.camel.component.webhook.WebhookConfiguration.class;
        case "webhookautoregister":
        case "webhookAutoRegister": return boolean.class;
        case "webhookbasepath":
        case "webhookBasePath": return java.lang.String.class;
        case "webhookcomponentname":
        case "webhookComponentName": return java.lang.String.class;
        case "webhookexternalurl":
        case "webhookExternalUrl": return java.lang.String.class;
        case "webhookpath":
        case "webhookPath": return java.lang.String.class;
        default: return null;
        }
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        WebhookComponent target = (WebhookComponent) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "autowiredenabled":
        case "autowiredEnabled": return target.isAutowiredEnabled();
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return target.isBridgeErrorHandler();
        case "configuration": return target.getConfiguration();
        case "webhookautoregister":
        case "webhookAutoRegister": return getOrCreateConfiguration(target).isWebhookAutoRegister();
        case "webhookbasepath":
        case "webhookBasePath": return getOrCreateConfiguration(target).getWebhookBasePath();
        case "webhookcomponentname":
        case "webhookComponentName": return getOrCreateConfiguration(target).getWebhookComponentName();
        case "webhookexternalurl":
        case "webhookExternalUrl": return getOrCreateConfiguration(target).getWebhookExternalUrl();
        case "webhookpath":
        case "webhookPath": return getOrCreateConfiguration(target).getWebhookPath();
        default: return null;
        }
    }
}


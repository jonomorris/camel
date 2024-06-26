/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.impl;

import javax.annotation.processing.Generated;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ExtendedPropertyConfigurerGetter;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.spi.ConfigurerStrategy;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.ExtendedCamelContext;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@Generated("org.apache.camel.maven.packaging.GenerateConfigurerMojo")
@SuppressWarnings("unchecked")
public class ExtendedCamelContextConfigurer extends org.apache.camel.support.component.PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        org.apache.camel.ExtendedCamelContext target = (org.apache.camel.ExtendedCamelContext) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "basepackagescan":
        case "basePackageScan": target.setBasePackageScan(property(camelContext, java.lang.String.class, value)); return true;
        case "bootstrapfactoryfinder":
        case "bootstrapFactoryFinder": target.setBootstrapFactoryFinder(property(camelContext, org.apache.camel.spi.FactoryFinder.class, value)); return true;
        case "defaultfactoryfinder":
        case "defaultFactoryFinder": target.setDefaultFactoryFinder(property(camelContext, org.apache.camel.spi.FactoryFinder.class, value)); return true;
        case "description": target.setDescription(property(camelContext, java.lang.String.class, value)); return true;
        case "endpointserviceregistry":
        case "endpointServiceRegistry": target.setEndpointServiceRegistry(property(camelContext, org.apache.camel.spi.EndpointServiceRegistry.class, value)); return true;
        case "errorhandlerfactory":
        case "errorHandlerFactory": target.setErrorHandlerFactory(property(camelContext, org.apache.camel.ErrorHandlerFactory.class, value)); return true;
        case "eventnotificationapplicable":
        case "eventNotificationApplicable": target.setEventNotificationApplicable(property(camelContext, boolean.class, value)); return true;
        case "exchangefactory":
        case "exchangeFactory": target.setExchangeFactory(property(camelContext, org.apache.camel.spi.ExchangeFactory.class, value)); return true;
        case "exchangefactorymanager":
        case "exchangeFactoryManager": target.setExchangeFactoryManager(property(camelContext, org.apache.camel.spi.ExchangeFactoryManager.class, value)); return true;
        case "headersmapfactory":
        case "headersMapFactory": target.setHeadersMapFactory(property(camelContext, org.apache.camel.spi.HeadersMapFactory.class, value)); return true;
        case "managementmbeanassembler":
        case "managementMBeanAssembler": target.setManagementMBeanAssembler(property(camelContext, org.apache.camel.spi.ManagementMBeanAssembler.class, value)); return true;
        case "name": target.setName(property(camelContext, java.lang.String.class, value)); return true;
        case "processorexchangefactory":
        case "processorExchangeFactory": target.setProcessorExchangeFactory(property(camelContext, org.apache.camel.spi.ProcessorExchangeFactory.class, value)); return true;
        case "profile": target.setProfile(property(camelContext, java.lang.String.class, value)); return true;
        case "reactiveexecutor":
        case "reactiveExecutor": target.setReactiveExecutor(property(camelContext, org.apache.camel.spi.ReactiveExecutor.class, value)); return true;
        case "registry": target.setRegistry(property(camelContext, org.apache.camel.spi.Registry.class, value)); return true;
        case "startupsteprecorder":
        case "startupStepRecorder": target.setStartupStepRecorder(property(camelContext, org.apache.camel.spi.StartupStepRecorder.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Class<?> getOptionType(String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "basepackagescan":
        case "basePackageScan": return java.lang.String.class;
        case "bootstrapfactoryfinder":
        case "bootstrapFactoryFinder": return org.apache.camel.spi.FactoryFinder.class;
        case "defaultfactoryfinder":
        case "defaultFactoryFinder": return org.apache.camel.spi.FactoryFinder.class;
        case "description": return java.lang.String.class;
        case "endpointserviceregistry":
        case "endpointServiceRegistry": return org.apache.camel.spi.EndpointServiceRegistry.class;
        case "errorhandlerfactory":
        case "errorHandlerFactory": return org.apache.camel.ErrorHandlerFactory.class;
        case "eventnotificationapplicable":
        case "eventNotificationApplicable": return boolean.class;
        case "exchangefactory":
        case "exchangeFactory": return org.apache.camel.spi.ExchangeFactory.class;
        case "exchangefactorymanager":
        case "exchangeFactoryManager": return org.apache.camel.spi.ExchangeFactoryManager.class;
        case "headersmapfactory":
        case "headersMapFactory": return org.apache.camel.spi.HeadersMapFactory.class;
        case "managementmbeanassembler":
        case "managementMBeanAssembler": return org.apache.camel.spi.ManagementMBeanAssembler.class;
        case "name": return java.lang.String.class;
        case "processorexchangefactory":
        case "processorExchangeFactory": return org.apache.camel.spi.ProcessorExchangeFactory.class;
        case "profile": return java.lang.String.class;
        case "reactiveexecutor":
        case "reactiveExecutor": return org.apache.camel.spi.ReactiveExecutor.class;
        case "registry": return org.apache.camel.spi.Registry.class;
        case "startupsteprecorder":
        case "startupStepRecorder": return org.apache.camel.spi.StartupStepRecorder.class;
        default: return null;
        }
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        org.apache.camel.ExtendedCamelContext target = (org.apache.camel.ExtendedCamelContext) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "basepackagescan":
        case "basePackageScan": return target.getBasePackageScan();
        case "bootstrapfactoryfinder":
        case "bootstrapFactoryFinder": return target.getBootstrapFactoryFinder();
        case "defaultfactoryfinder":
        case "defaultFactoryFinder": return target.getDefaultFactoryFinder();
        case "description": return target.getDescription();
        case "endpointserviceregistry":
        case "endpointServiceRegistry": return target.getEndpointServiceRegistry();
        case "errorhandlerfactory":
        case "errorHandlerFactory": return target.getErrorHandlerFactory();
        case "eventnotificationapplicable":
        case "eventNotificationApplicable": return target.isEventNotificationApplicable();
        case "exchangefactory":
        case "exchangeFactory": return target.getExchangeFactory();
        case "exchangefactorymanager":
        case "exchangeFactoryManager": return target.getExchangeFactoryManager();
        case "headersmapfactory":
        case "headersMapFactory": return target.getHeadersMapFactory();
        case "managementmbeanassembler":
        case "managementMBeanAssembler": return target.getManagementMBeanAssembler();
        case "name": return target.getName();
        case "processorexchangefactory":
        case "processorExchangeFactory": return target.getProcessorExchangeFactory();
        case "profile": return target.getProfile();
        case "reactiveexecutor":
        case "reactiveExecutor": return target.getReactiveExecutor();
        case "registry": return target.getRegistry();
        case "startupsteprecorder":
        case "startupStepRecorder": return target.getStartupStepRecorder();
        default: return null;
        }
    }
}


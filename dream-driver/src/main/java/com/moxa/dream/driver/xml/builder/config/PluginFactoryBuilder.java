package com.moxa.dream.driver.xml.builder.config;

import com.moxa.dream.driver.xml.builder.XMLBuilder;
import com.moxa.dream.driver.xml.moudle.XmlConstant;
import com.moxa.dream.driver.xml.moudle.XmlHandler;
import com.moxa.dream.driver.xml.util.XmlUtil;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

public class PluginFactoryBuilder extends XMLBuilder {
    private PluginFactory pluginFactory;

    public PluginFactoryBuilder(XmlHandler workHandler) {
        super(workHandler);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        switch (qName) {
            case XmlConstant.PLUGINFACTORY:
                pluginFactory = XmlUtil.applyAttributes(PluginFactory.class, attributes);
                break;
            case XmlConstant.INTERCEPTOR:
                InterceptorBuilder interceptorBuilder = new InterceptorBuilder(workHandler);
                interceptorBuilder.startElement(uri, localName, qName, attributes);
                break;
            default:
                throwXmlException(uri, localName, qName, attributes, XmlConstant.PLUGINFACTORY);
        }
    }

    @Override
    public void characters(String s) {

    }

    @Override
    public Object endElement(String uri, String localName, String qName) {
        return pluginFactory;
    }

    @Override
    public void builder(String uri, String localName, String qName, Object obj) {
        switch (qName) {
            case XmlConstant.INTERCEPTOR:
                InterceptorBuilder.Interceptor interceptor = (InterceptorBuilder.Interceptor) obj;
                pluginFactory.interceptorList.add(interceptor);
                break;
        }
    }

    static class PluginFactory {
        private final List<InterceptorBuilder.Interceptor> interceptorList = new ArrayList<>();
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<InterceptorBuilder.Interceptor> getInterceptorList() {
            return interceptorList;
        }
    }
}

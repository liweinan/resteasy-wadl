package org.jboss.resteasy.wadl.wadl;

import net.java.dev.wadl._2009._02.Application;
import net.java.dev.wadl._2009._02.ObjectFactory;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

/**
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class TestWADL_JAXB_Types {

    @Test
    public void testA() throws Exception {
        ObjectFactory factory = new ObjectFactory();
        Application app = factory.createApplication();
        JAXBContext context = JAXBContext.newInstance(Application.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(app, System.out);
    }
}

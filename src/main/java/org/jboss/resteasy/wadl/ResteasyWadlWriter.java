package org.jboss.resteasy.wadl;

import net.java.dev.wadl._2009._02.Application;
import net.java.dev.wadl._2009._02.ObjectFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class ResteasyWadlWriter {
    public void writeWadl(String uri, HttpServletRequest req, HttpServletResponse resp, Map<String, ResteasyWadlServiceRegistry> services)
            throws IOException {

        ServletOutputStream output = resp.getOutputStream();

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        try {
            writeWadl(writer);
        } catch (JAXBException e) {
            throw new IOException(e);
        }

        byte[] bytes = stringWriter.toString().getBytes();
        resp.setContentLength(bytes.length);
        output.write(bytes);
        output.flush();
        output.close();
    }

    private void writeWadl(PrintWriter writer) throws JAXBException {
        ObjectFactory factory = new ObjectFactory();
        Application app = factory.createApplication();
        JAXBContext context = JAXBContext.newInstance(Application.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(app, writer);
    }


}

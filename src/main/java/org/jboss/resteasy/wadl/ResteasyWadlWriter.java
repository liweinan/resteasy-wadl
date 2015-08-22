package org.jboss.resteasy.wadl;

import net.java.dev.wadl._2009._02.Application;
import net.java.dev.wadl._2009._02.ObjectFactory;
import net.java.dev.wadl._2009._02.Resources;
import org.jboss.resteasy.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class ResteasyWadlWriter {

    private final static Logger logger = Logger.getLogger(ResteasyWadlWriter.class);

    public void writeWadl(String base, HttpServletRequest req, HttpServletResponse resp, Map<String, ResteasyWadlServiceRegistry> serviceRegistries)
            throws IOException {

        ServletOutputStream output = resp.getOutputStream();
        for (Map.Entry<String, ResteasyWadlServiceRegistry> entry : serviceRegistries.entrySet()) {
            String uri = base;
            if (entry.getKey() != null) uri += entry.getKey();

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            try {
                ObjectFactory factory = new ObjectFactory();
                Application app = factory.createApplication();
                JAXBContext context = JAXBContext.newInstance(Application.class);

                Marshaller marshaller = context.createMarshaller();
                Resources resources = new Resources();
                resources.setBase(uri);
                app.getResources().add(resources);

                writeWadl(writer, entry.getValue(), app, marshaller);

                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(app, writer);
            } catch (JAXBException e) {
                throw new IOException(e);
            }

            byte[] bytes = stringWriter.toString().getBytes();
            resp.setContentLength(bytes.length);
            output.write(bytes);
            output.flush();
            output.close();
        }
    }

    private void writeWadl(PrintWriter writer, ResteasyWadlServiceRegistry serviceRegistry, Application app, Marshaller marshaller) throws JAXBException {

        for (ResteasyWadlMethodMetaData methodMetaData : serviceRegistry.getMethodMetaData()) {
            logger.debug("Path: " + methodMetaData.getUri());
            logger.debug(" Invoker: " + methodMetaData.getResource());

            for (String httpMethod : methodMetaData.getHttpMethods()) {

            }
        }


    }


}

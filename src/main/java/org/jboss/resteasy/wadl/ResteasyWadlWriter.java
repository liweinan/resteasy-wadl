package org.jboss.resteasy.wadl;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.wadl.jaxb.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
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

                processWadl(entry.getValue(), resources);

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

    private void processWadl(ResteasyWadlServiceRegistry serviceRegistry, Resources root) throws JAXBException {

        for (Map.Entry<String, ResteasyWadlResourceMetaData> resourceMetaDataEntry : serviceRegistry.getResources().entrySet()) {
            logger.debug("Path: " + resourceMetaDataEntry.getKey());
            Resource resourceClass = new Resource();
            Resource currentResourceClass = resourceClass;

            resourceClass.setPath(resourceMetaDataEntry.getKey());
            root.getResource().add(resourceClass);

            for (ResteasyWadlMethodMetaData methodMetaData : resourceMetaDataEntry.getValue().getMethodsMetaData()) {
                Method method = new Method();

                // First we need to check whether @Path annotation exists in a method.
                // If the @Path annotation exists, we need to create a resource for it.
                if (methodMetaData.getMethodUri() != null) {
                    Resource methodResource = new Resource();
                    methodResource.setPath(methodMetaData.getMethodUri());
                    methodResource.getMethodOrResource().add(method);
                    resourceClass.getMethodOrResource().add(methodResource);
                    currentResourceClass = methodResource;
                } else {
                    // register method into resource
                    resourceClass.getMethodOrResource().add(method);
                }


                for (ResteasyWadlMethodParamMetaData paramMetaData : methodMetaData.getParameters()) {
                    // All the method's @PathParam belong to resource
                    if (paramMetaData.getParamType().equals(ResteasyWadlMethodParamMetaData.MethodParamType.PATH_PARAMETER)) {
                        Param param = new Param();
                        param.setName(paramMetaData.getParamName());
                        param.setStyle(ParamStyle.TEMPLATE);
                        param.setType(paramMetaData.getType().toString()); // FIXME
                        currentResourceClass.getParam().add(param);
                    } else if (paramMetaData.getParamType().equals(ResteasyWadlMethodParamMetaData.MethodParamType.COOKIE_PARAMETER)) {
                        Request request = new Request();
                        Param param = new Param();
                        request.getParam().add(param);
                        param.setName("Cookie");
                        param.setStyle(ParamStyle.HEADER);
                        param.setType(paramMetaData.getType().toString()); // FIXME
                        method.setRequest(request);
                    }
                }

                // method name = {GET, POST, DELETE, ...}
                for (String name : methodMetaData.getHttpMethods()) {
                    method.setName(name);
                }

                // method id = method name
                method.setId(methodMetaData.getMethod().getName());

                // response type
                Response response = new Response();
                Representation representation = new Representation();
                // FIXME: get correct response type
                representation.setMediaType(methodMetaData.getWants());
                response.getRepresentation().add(representation);

                method.getResponse().add(response);
            }
        }

        for (ResteasyWadlServiceRegistry subService : serviceRegistry.getLocators())
            processWadl(subService, root);
    }
}

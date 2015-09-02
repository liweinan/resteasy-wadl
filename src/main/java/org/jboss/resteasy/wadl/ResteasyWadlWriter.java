package org.jboss.resteasy.wadl;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.wadl.jaxb.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
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

                // method name = {GET, POST, DELETE, ...}
                for (String name : methodMetaData.getHttpMethods()) {
                    method.setName(name);
                }

                // method id = method name
                method.setId(methodMetaData.getMethod().getName());

                // process method parameters
                for (ResteasyWadlMethodParamMetaData paramMetaData : methodMetaData.getParameters()) {
                    Param param = createParam(currentResourceClass, method, paramMetaData);
                }

                // process response of method
                Response response = createResponse(serviceRegistry, methodMetaData);
                method.getResponse().add(response);
            }
        }

        for (ResteasyWadlServiceRegistry subService : serviceRegistry.getLocators())
            processWadl(subService, root);
    }

    private Response createResponse(ResteasyWadlServiceRegistry serviceRegistry, ResteasyWadlMethodMetaData methodMetaData) {
        Response response = new Response();
        Representation representation = new Representation();

        Class _type = methodMetaData.getMethod().getReturnType();
        Type _generic = methodMetaData.getMethod().getGenericReturnType();

        MediaType mediaType = MediaType.WILDCARD_TYPE;

        if (methodMetaData.getProduces() != null) {
            mediaType = MediaType.valueOf(methodMetaData.getProduces());
            if (mediaType == null) {
                mediaType = serviceRegistry.getProviderFactory().getConcreteMediaTypeFromMessageBodyWriters(_type, _generic, methodMetaData.getMethod().getAnnotations(), MediaType.WILDCARD_TYPE);
                if (mediaType == null)
                    mediaType = MediaType.WILDCARD_TYPE;
            }
        }

        representation.setMediaType(mediaType.toString());
        response.getRepresentation().add(representation);
        return response;
    }

    private Param createParam(Resource currentResourceClass, Method method, ResteasyWadlMethodParamMetaData paramMetaData) {
        Param param = new Param();
        // All the method's @PathParam belong to resource
        if (paramMetaData.getParamType().equals(ResteasyWadlMethodParamMetaData.MethodParamType.PATH_PARAMETER)) {
            param.setStyle(ParamStyle.TEMPLATE);
            setType(param, paramMetaData);
            param.setName(paramMetaData.getParamName());
            currentResourceClass.getParam().add(param);
        } else if (paramMetaData.getParamType().equals(ResteasyWadlMethodParamMetaData.MethodParamType.COOKIE_PARAMETER)) {
            param.setStyle(ParamStyle.HEADER);
            setType(param, paramMetaData);
            Request request = new Request();
            request.getParam().add(param);
            param.setName("Cookie");
            param.setPath(paramMetaData.getParamName());
            method.setRequest(request);
        }

        return param;
    }

    private void setType(Param param, ResteasyWadlMethodParamMetaData paramMetaData) {
        if (paramMetaData.getType().equals(int.class) || paramMetaData.getType().equals(Integer.class)) {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "int", "xs"));
        } else if (paramMetaData.getType().equals(boolean.class) || paramMetaData.getType().equals(Boolean.class)) {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "boolean", "xs"));
        } else if (paramMetaData.getType().equals(long.class) || paramMetaData.getType().equals(Long.class)) {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "long", "xs"));
        } else if (paramMetaData.getType().equals(short.class) || paramMetaData.getType().equals(Short.class)) {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "short", "xs"));
        } else if (paramMetaData.getType().equals(byte.class) || paramMetaData.getType().equals(Byte.class)) {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "byte", "xs"));
        } else if (paramMetaData.getType().equals(float.class) || paramMetaData.getType().equals(Float.class)) {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "float", "xs"));
        } else if (paramMetaData.getType().equals(double.class) || paramMetaData.getType().equals(Double.class)) {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "double", "xs"));
        } else {
            param.setType(new QName("http://www.w3.org/2001/XMLSchema", "string", "xs"));
        }
    }
}

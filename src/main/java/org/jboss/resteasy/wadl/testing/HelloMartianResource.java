package org.jboss.resteasy.wadl.testing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
@Path("/martian")
public class HelloMartianResource {
    @GET
    public String hello() {
        return "Hello, Martian!";
    }
}

package org.jboss.resteasy.wadl.testing;

import javax.ws.rs.*;

/**
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
@Path("/smoke")
public class Smoke {

    @GET
    @Path("/header")
    @Produces("text/plain")
    public String testHeaderParam(@HeaderParam("Referer") String referer) {
        return referer;
    }

    @GET
    @Path("/header2")
    @Produces("text/plain")
    public String testHeaderParam2(@HeaderParam("Referer") String referer, @HeaderParam("Accept") String accept) {
        return accept;
    }

    @GET
    @Path("mixed/{p}")
    @Produces("text/plain")
    public String mixed(@HeaderParam("h1") String h1, @CookieParam("c1") String c1, @PathParam("p1") String p1, @QueryParam("q1") String q1,
                        @HeaderParam("h2") String h2, @CookieParam("c2") String c2, @PathParam("p2") String p2, @QueryParam("q2") String q2) {
        return "";
    }
}

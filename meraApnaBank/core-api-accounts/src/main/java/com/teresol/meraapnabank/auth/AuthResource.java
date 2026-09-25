package com.teresol.meraapnabank.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    public static class SignupRequest {
        public String fullName;
        public String email;
        public String password;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    @Inject
    AuthService authService;

    @POST
    @Path("/signup")
    public Map<String, Object> signup(SignupRequest request) {
        return authService.signup(request.fullName, request.email, request.password);
    }

    @POST
    @Path("/login")
    public Map<String, Object> login(LoginRequest request) {
        return authService.login(request.email, request.password);
    }

    /** Protected by AuthFilter, which stores the authenticated email on the request. */
    @GET
    @Path("/me")
    public Map<String, Object> me(@Context ContainerRequestContext context) {
        return authService.profile((String) context.getProperty(AuthFilter.USER_PROPERTY));
    }
}

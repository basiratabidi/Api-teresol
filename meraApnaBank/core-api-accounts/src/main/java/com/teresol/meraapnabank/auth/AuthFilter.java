package com.teresol.meraapnabank.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** Requires a valid bearer token on the banking APIs and /auth/me. */
@Provider
public class AuthFilter implements ContainerRequestFilter {

    static final String USER_PROPERTY = "auth.user";

    @Inject
    AuthService authService;

    @Override
    public void filter(ContainerRequestContext context) {
        String path = context.getUriInfo().getPath();
        if (path.startsWith("/")) path = path.substring(1);
        boolean protectedPath = path.startsWith("accounts") || path.startsWith("branches") || path.equals("auth/me");
        if (!protectedPath || "OPTIONS".equals(context.getMethod())) return;

        String header = context.getHeaderString("Authorization");
        String user = (header != null && header.startsWith("Bearer ")) ? authService.verify(header.substring(7)) : null;
        if (user == null) {
            context.abortWith(Response.status(401).type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("status", 401, "error", "Please sign in to continue.")).build());
            return;
        }
        context.setProperty(USER_PROPERTY, user);
    }
}

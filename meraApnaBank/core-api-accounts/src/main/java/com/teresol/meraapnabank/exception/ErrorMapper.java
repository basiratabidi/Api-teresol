package com.teresol.meraapnabank.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

/**
 * Relays errors from dataaccess-ms-accounts (400, 404, 409, ...) to the caller
 * with the same status and body, instead of Quarkus' default 500.
 */
@Provider
public class ErrorMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response downstream = exception.getResponse();
        int status = downstream.getStatus();

        if (exception instanceof ClientWebApplicationException && downstream.hasEntity()) {
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(downstream.readEntity(String.class))
                    .build();
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("status", status, "error", String.valueOf(exception.getMessage())))
                .build();
    }
}

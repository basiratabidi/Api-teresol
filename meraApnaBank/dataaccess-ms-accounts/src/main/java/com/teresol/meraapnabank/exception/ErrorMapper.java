package com.teresol.meraapnabank.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Turns JAX-RS exceptions (400, 404, 409, ...) into a JSON body so callers
 * see the reason, not just the status code.
 */
@Provider
public class ErrorMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("status", status, "error", String.valueOf(exception.getMessage())))
                .build();
    }
}

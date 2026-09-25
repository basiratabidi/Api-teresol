package com.teresol.meraapnabank.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        int status = (exception instanceof WebApplicationException webException)
                ? webException.getResponse().getStatus()
                : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", errorMessage(exception));

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    // Errors relayed from dataaccess-ms-accounts carry its JSON body; surface that
    // message instead of the REST client's generic "Received: ..." text.
    private String errorMessage(Throwable exception) {
        if (exception instanceof WebApplicationException webException) {
            try {
                Map<?, ?> downstream = webException.getResponse().readEntity(Map.class);
                if (downstream != null && downstream.get("error") != null) {
                    return downstream.get("error").toString();
                }
            } catch (RuntimeException ignored) {
                // no readable JSON body; fall back to the exception message
            }
        }
        return exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}

package com.teresol.meraapnabank;

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
        body.put("error", exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName());

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}

package com.example.projectrplbo.model;

public class Response {

    private int statusCode;

    private String message;

    private Object payload;

    public Response() {}

    public Response(int statusCode, String message, Object payload) {

        this.statusCode = statusCode;

        this.message = message;

        this.payload = payload;

    }

    public boolean isSuccess() {

        return statusCode >= 200 && statusCode < 300;

    }

    public static Response ok(String message, Object payload) {

        return new Response(200, message, payload);

    }

    public static Response notFound(String message) {

        return new Response(404, message, null);

    }

    public static Response badRequest(String message) {

        return new Response(400, message, null);

    }

    public static Response serverError(String message) {

        return new Response(500, message, null);

    }

    public int getStatusCode() { return statusCode; }

    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public Object getPayload() { return payload; }

    public void setPayload(Object payload) { this.payload = payload; }

}
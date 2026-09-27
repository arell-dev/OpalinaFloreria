package com.floral.opalina.session;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.util.*;

@Component
@SessionScope
public class CarritoSession {
    private final Map<String, Integer> cantidades = new LinkedHashMap<>();

    public synchronized Map<String, Integer> cantidades() {
        return Map.copyOf(cantidades);
    }

    public synchronized void agregar(String id, int cantidad) {
        cantidades.merge(id, cantidad, Integer::sum);
    }

    public synchronized void cantidad(String id, int cantidad) {
        if (cantidad <= 0)
            cantidades.remove(id);
        else
            cantidades.put(id, cantidad);
    }

    public synchronized void quitar(String id) {
        cantidades.remove(id);
    }

    public synchronized void vaciar() {
        cantidades.clear();
    }

    public synchronized boolean vacio() {
        return cantidades.isEmpty();
    }
}

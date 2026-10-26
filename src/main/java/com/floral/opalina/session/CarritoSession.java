package com.floral.opalina.session;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.util.*;

@Component
@SessionScope
public class CarritoSession {
    private final Map<Long, Integer> cantidades = new LinkedHashMap<>();

    public synchronized Map<Long, Integer> cantidades() {
        return Map.copyOf(cantidades);
    }

    public synchronized void agregar(Long id, int cantidad) {
        cantidades.merge(id, cantidad, Integer::sum);
    }

    public synchronized void cantidad(Long id, int cantidad) {
        if (cantidad <= 0)
            cantidades.remove(id);
        else
            cantidades.put(id, cantidad);
    }

    public synchronized void quitar(Long id) {
        cantidades.remove(id);
    }

    public synchronized void vaciar() {
        cantidades.clear();
    }

    public synchronized boolean vacio() {
        return cantidades.isEmpty();
    }
}

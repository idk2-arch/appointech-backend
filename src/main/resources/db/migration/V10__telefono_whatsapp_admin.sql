-- Telefono de WhatsApp del administrador para clientes fuera de cobertura (opcional, inicialmente vacio).
ALTER TABLE configuracion_franjas
    ADD COLUMN telefono_whatsapp_admin VARCHAR(30);

-- 5 proyectos, cada uno con 5 todos (25 todos en total)

INSERT INTO projects (id, name) VALUES (1, 'Nakano Frontend');
INSERT INTO projects (id, name) VALUES (2, 'API Pagos');
INSERT INTO projects (id, name) VALUES (3, 'App Movil');
INSERT INTO projects (id, name) VALUES (4, 'Dashboard Analytics');
INSERT INTO projects (id, name) VALUES (5, 'Infraestructura Cloud');

-- Todos del proyecto 1
INSERT INTO todos (title, is_completed, project_id) VALUES ('Diseñar wireframes', FALSE, 1);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Implementar login', TRUE, 1);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Crear componentes UI', FALSE, 1);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Integrar API', FALSE, 1);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Pruebas de usabilidad', FALSE, 1);

-- Todos del proyecto 2
INSERT INTO todos (title, is_completed, project_id) VALUES ('Definir endpoints', TRUE, 2);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Validar webhooks', FALSE, 2);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Integrar pasarela', FALSE, 2);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Manejo de reembolsos', FALSE, 2);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Documentar OpenAPI', TRUE, 2);

-- Todos del proyecto 3
INSERT INTO todos (title, is_completed, project_id) VALUES ('Configurar Flutter', TRUE, 3);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Pantalla de inicio', TRUE, 3);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Notificaciones push', FALSE, 3);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Modo offline', FALSE, 3);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Publicar en stores', FALSE, 3);

-- Todos del proyecto 4
INSERT INTO todos (title, is_completed, project_id) VALUES ('Conectar data warehouse', FALSE, 4);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Graficas de ventas', TRUE, 4);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Filtros por fecha', FALSE, 4);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Exportar CSV', FALSE, 4);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Alertas automaticas', FALSE, 4);

-- Todos del proyecto 5
INSERT INTO todos (title, is_completed, project_id) VALUES ('Provisionar VPC', TRUE, 5);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Configurar CI/CD', TRUE, 5);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Monitoreo con Prometheus', FALSE, 5);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Backup automatico', FALSE, 5);
INSERT INTO todos (title, is_completed, project_id) VALUES ('Politicas de seguridad', FALSE, 5);

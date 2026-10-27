delete from command_executions;
delete from command;
delete from device_accesses where device_id not in (select id from devices where name = 'Seed Lamp');
delete from devices where name <> 'Seed Lamp';
delete from users where email not in ('owner@example.com', 'guest@example.com', 'stranger@example.com');

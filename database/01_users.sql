create database lapis;
create user lapis_api password '<missing>';
create user lapis_proc password '<missing>';

grant pg_read_all_data to lapis_api;
grant pg_read_all_data, pg_write_all_data to lapis_proc;

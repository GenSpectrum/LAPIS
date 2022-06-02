-- Create staging tables
create table y_main_metadata_staging (like y_main_metadata including all);
create table y_main_sequence_staging (like y_main_sequence including all);
create table y_main_sequence_columnar_staging (like y_main_sequence_columnar including all);
create table y_main_aa_sequence_staging (like y_main_aa_sequence including all);
create table y_main_aa_sequence_columnar_staging (like y_main_aa_sequence_columnar including all);

-- Switch out the tables
create or replace function y_switch_in_staging_tables() returns void security definer as $$
begin
  alter table y_main_metadata rename to y_main_metadata_old;
  alter table y_main_sequence rename to y_main_sequence_old;
  alter table y_main_sequence_columnar rename to y_main_sequence_columnar_old;
  alter table y_main_aa_sequence rename to y_main_aa_sequence_old;
  alter table y_main_aa_sequence_columnar rename to y_main_aa_sequence_columnar_old;

  alter table y_main_metadata_staging rename to y_main_metadata;
  alter table y_main_sequence_staging rename to y_main_sequence;
  alter table y_main_sequence_columnar_staging rename to y_main_sequence_columnar;
  alter table y_main_aa_sequence_staging rename to y_main_aa_sequence;
  alter table y_main_aa_sequence_columnar_staging rename to y_main_aa_sequence_columnar;

  truncate y_main_metadata_old, y_main_sequence_old,
    y_main_sequence_columnar_old, y_main_aa_sequence_old,
    y_main_aa_sequence_columnar_old;

  alter table y_main_metadata_old rename to y_main_metadata_staging;
  alter table y_main_sequence_old rename to y_main_sequence_staging;
  alter table y_main_sequence_columnar_old rename to y_main_sequence_columnar_staging;
  alter table y_main_aa_sequence_old rename to y_main_aa_sequence_staging;
  alter table y_main_aa_sequence_columnar_old rename to y_main_aa_sequence_columnar_staging;
end;
$$ language plpgsql;
revoke all on function y_switch_in_staging_tables() from public;
grant execute on function y_switch_in_staging_tables() to lapis_proc;

create or replace function y_switch_in_staging_tables_without_truncate() returns void security definer as $$
begin
  alter table y_main_metadata rename to y_main_metadata_old;
  alter table y_main_sequence rename to y_main_sequence_old;
  alter table y_main_sequence_columnar rename to y_main_sequence_columnar_old;
  alter table y_main_aa_sequence rename to y_main_aa_sequence_old;
  alter table y_main_aa_sequence_columnar rename to y_main_aa_sequence_columnar_old;

  alter table y_main_metadata_staging rename to y_main_metadata;
  alter table y_main_sequence_staging rename to y_main_sequence;
  alter table y_main_sequence_columnar_staging rename to y_main_sequence_columnar;
  alter table y_main_aa_sequence_staging rename to y_main_aa_sequence;
  alter table y_main_aa_sequence_columnar_staging rename to y_main_aa_sequence_columnar;

  alter table y_main_metadata_old rename to y_main_metadata_staging;
  alter table y_main_sequence_old rename to y_main_sequence_staging;
  alter table y_main_sequence_columnar_old rename to y_main_sequence_columnar_staging;
  alter table y_main_aa_sequence_old rename to y_main_aa_sequence_staging;
  alter table y_main_aa_sequence_columnar_old rename to y_main_aa_sequence_columnar_staging;
end;
$$ language plpgsql;
revoke all on function y_switch_in_staging_tables_without_truncate() from public;
grant execute on function y_switch_in_staging_tables_without_truncate() to lapis_proc;

alter table t_mock_request drop column headers;
alter table t_mock_request add match_pattern varchar(4096);

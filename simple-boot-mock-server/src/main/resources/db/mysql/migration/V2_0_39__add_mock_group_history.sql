ALTER TABLE t_mock_group ADD data_version INT(11) DEFAULT 0 COMMENT 'Data Version';
ALTER TABLE t_mock_group ADD modify_from INT(11) COMMENT 'Modified From (History Parent ID)';

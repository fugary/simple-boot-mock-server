CREATE TABLE t_mock_scenario
(
    id            int(11) NOT NULL AUTO_INCREMENT,
    group_id      int(11),
    scenario_name varchar(255),
    scenario_code varchar(64),
    description   text,
    status        int(11),
    creator       varchar(255),
    create_date   timestamp NULL,
    modifier      varchar(255),
    modify_date   timestamp NULL,
    PRIMARY KEY (id)
);

ALTER TABLE t_mock_group ADD COLUMN active_scenario_code varchar(64);

ALTER TABLE t_mock_request ADD COLUMN scenario_code varchar(64);
